package com.zipwhip.api.signals;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.zipwhip.api.signals.dto.*;
import com.zipwhip.api.signals.dto.json.SignalProviderGsonBuilder;
import com.zipwhip.concurrent.*;
import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.signals2.SignalServerEvent;
import com.zipwhip.signals2.presence.Presence;
import com.zipwhip.signals2.presence.UserAgent;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.FutureDateUtil;
import com.zipwhip.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Date: 7/24/13
 * Time: 5:28 PM
 * <p/>
 * SignalProvider is responsible for clientId. It is not responsible for executing a /signals/connect
 * <p/>
 * Because the caller needs to execute a /signals/connect (and then cancel/reset) we need to use the ConnectionHandle
 * metaphor.
 *
 * @author Michael
 * @version 1
 */
public class SignalProviderImpl extends CascadingDestroyableBase implements SignalProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignalProviderImpl.class);

    public static final int EMPTY_REQUEST = 1;
    public static final int EMPTY_SIGNATURE = 2;
    public static final int CLIENT_ID_ALREADY_EXISTS = 3;
    public static final int FAILED_TO_LOCK = 4;
    public static final int NOT_CONNECTED = 5;
    public static final int REQUEST_CANCELLED = 6;
    public static final int ACK_TIMEOUT = 7;
    public static final int EMPTY_CLIENT_ID = 8;
    public static final int UNKNOWN_CLIENT_ID = 9;

    private final ObservableHelper<SubscribeResult> subscribeEvent;
    private final ObservableHelper<SubscribeResult> unsubscribeEvent;
    private final ObservableHelper<Throwable> exceptionEvent;
    private final ObservableHelper<Void> connectionChangedEvent;
    private final ObservableHelper<DeliveredMessage> signalReceivedEvent;
    private final ObservableHelper<Event<Presence>> presenceChangedEvent;
    private final ObservableHelper<BindResult> bindEvent;

    private Executor executor = Executors.newSingleThreadExecutor();
    private Executor eventExecutor = SimpleExecutor.getInstance();
    private Presence presence = new Presence();
    private SignalsSubscribeActor signalsSubscribeActor;
    private ImportantTaskExecutor importantTaskExecutor;
    private BufferedOrderedQueue<DeliveredMessage> bufferedOrderedQueue;
    private Gson gson = SignalProviderGsonBuilder.getInstance();
    private SignalConnection signalConnection;
    private long pingTimeoutSeconds = 30;

    private final Map<String, SubscriptionRequest> pendingSubscriptionRequests = new ConcurrentHashMap<String, SubscriptionRequest>();

    private volatile ObservableFuture<Void> externalConnectFuture;
    private volatile MutableObservableFuture<Void> connectFuture;

    private volatile String clientId;
    private volatile String token;
    private volatile ObservableFuture<BindResult> bindFuture;
    private volatile ObservableFuture<String> pingFuture;
    private volatile long pingCount = 0;

    public SignalProviderImpl() {
        this(SimpleExecutor.getInstance());
    }

    public SignalProviderImpl(Executor eventExecutor) {
        connectionChangedEvent = new ObservableHelper<Void>("ConnectionChangedEvent", eventExecutor);
        exceptionEvent = new ObservableHelper<Throwable>("ExceptionEvent", eventExecutor);
        subscribeEvent = new ObservableHelper<SubscribeResult>("SubscribeEvent", eventExecutor);
        unsubscribeEvent = new ObservableHelper<SubscribeResult>("UnsubscribeEvent", eventExecutor);
        signalReceivedEvent = new ObservableHelper<DeliveredMessage>("MessageReceivedEvent", eventExecutor);
        bindEvent = new ObservableHelper<BindResult>("BindEvent", eventExecutor);
        presenceChangedEvent = new ObservableHelper<Event<Presence>>("PresenceChangedEvent", eventExecutor);
    }

    @Override
    public synchronized ObservableFuture<Void> connect(UserAgent userAgent) throws IllegalStateException {
        return connect(userAgent, null, null);
    }

    public synchronized ObservableFuture<Void> connect(UserAgent userAgent, String clientId, String token) throws IllegalStateException {
        // allow multiple connect attempts to reuse the same future.
        if (externalConnectFuture != null) {
            return externalConnectFuture;
        }

        if (userAgent == null) {
            throw new IllegalStateException("The userAgent cannot be null");
        } else if (signalConnection.isConnected()) {
            return fail("Already connected!");
        }

        // The only thing on Presence that a customer can specify is the userAgent.
        // Everything else is defined from the server.
        presence.setUserAgent(userAgent);

        connectFuture = future();
        setClientId(clientId, token);

        // The reason to use the "importantTaskExecutor" this way is so the future can be timed out.
        // If we issue a connect request and it doesn't come back for 1 minute, we need to be able to
        // time it out/cancel it.
        ObservableFuture<Void> futureThatSupportsTimeout = importantTaskExecutor.enqueue(executor,
                new ConnectTask(connectFuture),
                FutureDateUtil.in1Minute());

        // The external one supports timeout. (but is not mutable)
        // The internal one does not timeout.
        externalConnectFuture = futureThatSupportsTimeout;

        // Clean up the future.
        futureThatSupportsTimeout.addObserver(RESET_CONNECT_FUTURE);

        return externalConnectFuture;
    }

    @Override
    public synchronized ObservableFuture<Void> disconnect() {
        if (connectFuture != null) {
            connectFuture.cancel();
        }

        return signalConnection.disconnect();
    }

    @Override
    public synchronized ObservableFuture<SubscribeResult> subscribe(String sessionKey, String subscriptionId) {
        return subscribe(sessionKey, subscriptionId, null);
    }

    @Override
    public synchronized ObservableFuture<SubscribeResult> subscribe(String sessionKey, String subscriptionId, String scope) {
        if (!signalConnection.isConnected()) {
            return fail("Not connected");
        }

        String clientId = getClientId();
        if (StringUtil.isNullOrEmpty(clientId)) {
            return fail("ClientId not defined yet. Are you connected?");
        }

        if (StringUtil.isNullOrEmpty(subscriptionId)) {
            subscriptionId = sessionKey;
        }

        //
        // Make sure that any previous requests get cancelled.
        //
        SubscriptionRequest request = pendingSubscriptionRequests.remove(subscriptionId);
        if (request != null) {
            request.getFuture().cancel();
        }

        //
        // Nest the future within our result. The subscribe is a multi-step process. We need to make the webcall and then
        // wait until we receive a SubscriptionCompleteCommand.
        //
        return executeWithTimeout(
                new SignalSubscribeCallback(sessionKey, subscriptionId, scope), FutureDateUtil.in30Seconds());
    }

    @Override
    public ObservableFuture<Void> unsubscribe(final String sessionKey, final String subscriptionId) {
        if (!signalConnection.isConnected()) {
            return new FakeFailingObservableFuture<Void>(this, new IllegalStateException("Not connected"));
        }

        return executeWithTimeout(
                new SignalUnsubscribeCallback(sessionKey, subscriptionId), FutureDateUtil.in30Seconds());
    }

    @Override
    public synchronized ObservableFuture<Void> resetDisconnectAndConnect() {
        // TODO: protect threading (what happens if the connection cycles during this process)
        final MutableObservableFuture<Void> result = future();

        disconnect().addObserver(new Observer<ObservableFuture<Void>>() {
            @Override
            public void notify(Object sender, ObservableFuture<Void> item) {
                if (cascadeFailure(item, result)) {
                    return;
                }

                if (item.isSuccess()) {
                    result.setSuccess(null);
                    return;
                }

                throw new IllegalStateException("Not sure what state this is " + result);
            }
        });

        return result;
    }

    public ObservableFuture<String> ping() {
        if (pingFuture != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("PingFuture already existed. Not going to make a new one.");
            }

            return pingFuture;
        }

        if (externalConnectFuture != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Connect in progress, not attempting ping.");
            }

            return new FakeObservableFuture<String>(this, null);
        }

        final MutableObservableFuture<String> resultFuture = new DefaultObservableFuture<String>(this, executor);

        ObservableFuture<String> future = pingFuture = executeWithTimeout(
                new PingTask(signalConnection, resultFuture, pingCount),
                FutureDateUtil.inFuture(pingTimeoutSeconds, TimeUnit.SECONDS));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created pingFuture.");
        }

        future.addObserver(new Observer<ObservableFuture<String>>() {
            @Override
            public void notify(Object sender, ObservableFuture<String> item) {
                synchronized (SignalProviderImpl.this) {
                    pingFuture = null; // Self heal the pingFuture object

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Destroyed pingFuture.");
                    }

                    if (item.isCancelled()) {
                        LOGGER.warn("PingFuture was cancelled, not reconnecting.");
                        resultFuture.cancel();

                        return;
                    }

                    boolean doReconnect = false;
                    if (!item.isSuccess()) {
                        if (externalConnectFuture != null) {
                            LOGGER.debug("Ping failed but connect in progress; not forcing reconnect.");

                            resultFuture.setSuccess(item.getResult());
                            return;
                        }

                        LOGGER.error("Ping task failed! Attaching failure to future and reconnecting: " + item.getCause());

                        resultFuture.setFailure(item.getCause());
                        doReconnect = true;
                    } else {
                        if (item.getResult() != null) {
                            String payload = item.getResult();
                            try {
                                if (Long.parseLong(payload) != pingCount) {
                                    LOGGER.error("Pong mismatch! Reconnecting. %s vs %s", payload, pingCount);

                                    resultFuture.setFailure(new IllegalStateException(String.format("Wrong pong response (%s) for ping (%s)!", payload, pingCount)));
                                    doReconnect = true;
                                } else {
                                    LOGGER.debug("Received correct pong: " + payload);

                                    resultFuture.setSuccess(item.getResult());
                                    pingCount++;
                                }
                            } catch (Exception e) {
                                LOGGER.error("Error parsing server pong response! Reconnecting. " + e);

                                resultFuture.setFailure(e);
                                doReconnect = true;
                            }
                        }
                    }

                    if (doReconnect) {
                        pingCount = 0;

                        if (signalConnection.isConnected()) {
                            // the connection hasn't yet detected the disconnect
                            forceDisconnectAndReconnect();
                        } else {
                            signalConnection.connect();
                        }
                    }
                }
            }
        });

        return future;
    }

    private void forceDisconnectAndReconnect() {
        LOGGER.debug("Reconnect called. Disconnecting...");

        if (pingFuture != null) {
            LOGGER.debug("Cancelling existing pingFuture, as we're now reconnecting.");

            clearPingFuture();
        }

        signalConnection.disconnect().addObserver(new Observer<ObservableFuture<Void>>() {
            @Override
            public void notify(Object sender, ObservableFuture<Void> item) {
                LOGGER.debug("... now scheduling reconnect.");

                signalConnection.reconnect();
            }
        });
    }

    private Observer<ObservableFuture<Void>> RESET_CONNECT_FUTURE = new Observer<ObservableFuture<Void>>() {

        @Override
        public void notify(Object sender, ObservableFuture<Void> item) {
            // Clean up the variables.
            synchronized (SignalProviderImpl.this) {
                if (externalConnectFuture != item) {
                    LOGGER.debug(String.format("The futures did not match, so decided not to clear it out. %s/%s", connectFuture, item));
                    return;
                }

                externalConnectFuture = null;
                connectFuture = null;
            }
        }
    };

    private Observer<Void> issueBindRequestOnConnectCallback = new Observer<Void>() {
        @Override
        public void notify(Object sender, Void item) {
            if (bindFuture != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("(issueBindRequestOnConnectCallback) The bindFuture was not already null, so somebody else is already handling this.");
                }
                return;
            }

            executeBindRequest();
        }
    };

    private synchronized ObservableFuture<BindResult> executeBindRequest() {
        return executeBindRequest(clientId, token, true);
    }

    private synchronized ObservableFuture<BindResult> executeBindRequest(String clientId, String token, boolean reconnect) {
        if (bindFuture != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("BindRequest already existed. Not going to make a new one?!");
            }

            return bindFuture;
        }

        if (pingFuture != null) {
            LOGGER.debug("Initiating bind, so cancelling any existing pings.");

            clearPingFuture();
        }

        final BindRequest _bindRequest = new BindRequest(getUserAgent(), clientId, token);

        ObservableFuture<BindResult> future = bindFuture = executeWithTimeout(
                new BindTask(signalConnection, _bindRequest, eventExecutor, gson),
                FutureDateUtil.in30Seconds());

        if (reconnect) {
            future.addObserver(new ReconnectOnFailureObserver(future));
        } else {
            future.addObserver(new DisconnectOnFailureObserver(future));
        }

        // When this future is successful, we need to save the details
        future.addObserver(new Observer<ObservableFuture<BindResult>>() {
            @Override
            public void notify(Object sender, ObservableFuture<BindResult> item) {
                synchronized (SignalProviderImpl.this) {
                    if (bindFuture != item) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Avoided a bug? The futures were not the same!");
                        }

                        return;
                    }

                    // Self heal the bindFuture object
                    bindFuture = null;

                    if (!item.isSuccess()) {
                        LOGGER.error("Bind not successful! " + item.getCause());

                        if (pingFuture != null) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Clearing existing pingFuture.");
                            }

                            clearPingFuture();
                        }

                        return;
                    }

                    BindResult response = item.getResult();

                    setClientId(response.getClientId(), response.getToken());

                    bindEvent.notifyObservers(SignalProviderImpl.this, response);
                }
            }
        });

        return future;
    }

    @Override
    public Observable<BindResult> getBindEvent() {
        return bindEvent;
    }

    private final Observer<JsonElement> messageProcessingObserver = new Observer<JsonElement>() {
        @Override
        public void notify(Object sender, JsonElement element) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Parsing into json %s", element));
            }

            DeliveredMessage deliveredMessage;

            try {
                // parse the message, detect the type, throw the appropriate event
                deliveredMessage = gson.fromJson(element, DeliveredMessage.class);
            } catch (Exception e) {
                LOGGER.error("Failed to parse json", e);
                exceptionEvent.notifyObservers(SignalProviderImpl.this, new JsonParseException(element.toString(), e));
                return;
            }

            bufferedOrderedQueue.append(deliveredMessage);
        }
    };

    private final Observer<SignalServerEvent> serverEventProcessingObserver = new Observer<SignalServerEvent>() {
        @Override
        public void notify(Object sender, SignalServerEvent event) {
            synchronized (SignalProviderImpl.this) {

                switch (event.getCode()) {
                    case EMPTY_REQUEST:
                        LOGGER.error("Empty request!");
                        break;
                    case EMPTY_SIGNATURE:
                        LOGGER.error("Empty signature!");
                        break;
                    case CLIENT_ID_ALREADY_EXISTS:
                        LOGGER.error("clientId already exists!");
                        break;
                    case FAILED_TO_LOCK:
                        LOGGER.error("Failed to acquire required lock!");
                        break;
                    case NOT_CONNECTED:
                        LOGGER.error("Not connected!");
                        break;
                    case REQUEST_CANCELLED:
                        LOGGER.error("Request cancelled!");
                        break;
                    case ACK_TIMEOUT:
                        LOGGER.error("Ack timeout!");
                        break;
                    case EMPTY_CLIENT_ID:
                        LOGGER.error("Empty clientId!");
                        break;
                    case UNKNOWN_CLIENT_ID:
                        LOGGER.error("Unknown clientId!");
                        break;
                }
            }
        }
    };

    public final Observer<DeliveredMessage> releaseMessageObserver = new Observer<DeliveredMessage>() {
        @Override
        public void notify(Object sender, DeliveredMessage message) {
            // first check for system commands
            if (StringUtil.equalsIgnoreCase(message.getType(), "subscribe")) {
                handleSubscribeCommand(message);
            } else if (StringUtil.equalsIgnoreCase(message.getType(), "presence")) {
                presenceChangedEvent.notifyObservers(this, message);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Fire signalReceivedEvent.notifyObservers(this, message) %s", message));
                }

                signalReceivedEvent.notifyObservers(this, message);
            }
        }
    };

    private void handleSubscribeCommand(DeliveredMessage message) {
        if (StringUtil.equalsIgnoreCase(message.getEvent(), "complete")) {

            handleSubscribeComplete(message);
        } else {
            throw new IllegalStateException("Not sure what event this is: " + message.getEvent());
        }
    }

    private void handleSubscribeComplete(DeliveredMessage message) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Got SubscriptionComplete: " + message);
        }

        // the message is delivered directly to us.
        // Therefore the 'subscriptionIds' field will be null.
        SubscribeCompleteContent result = (SubscribeCompleteContent) message.getContent();
        SubscriptionRequest request = pendingSubscriptionRequests.remove(result.getSubscriptionId());
        if (request == null) {
            LOGGER.error("SubscriptionRequest was null!");
            return;
        }

        MutableObservableFuture<SubscribeResult> future = request.getFuture();
        SubscribeResult subscribeResult = new SubscribeResult();

        subscribeResult.setSubscriptionId(result.getSubscriptionId());
        subscribeResult.setSessionKey(request.getSessionKey());
        subscribeResult.setChannels(result.getAddresses());

        future.setSuccess(subscribeResult);

        subscribeEvent.notifyObservers(SignalProviderImpl.this, subscribeResult);
    }

    private void setClientId(String clientId, String token) {
        this.clientId = clientId;
        this.token = token;
    }

    private <T> ObservableFuture<T> executeWithTimeout(Callable<ObservableFuture<T>> task, Date date) {
        return importantTaskExecutor.enqueue(executor, task, date);
    }

    private <T> ObservableFuture<T> fail(Throwable throwable) {
        return new FakeFailingObservableFuture<T>(this, throwable);
    }

    private <T> ObservableFuture<T> fail(String message) {
        return fail(new IllegalStateException(message));
    }

    private <T> MutableObservableFuture<T> future() {
        return new DefaultObservableFuture<T>(this, eventExecutor, "Future:" + this.getClass().getName());
    }

    @Override
    public Observable<Event<Presence>> getPresenceChangedEvent() {
        return presenceChangedEvent;
    }

    public ObservableHelper<Void> getConnectionChangedEvent() {
        return connectionChangedEvent;
    }

    @Override
    public Observable<DeliveredMessage> getSignalReceivedEvent() {
        return signalReceivedEvent;
    }

    @Override
    public Observable<Throwable> getExceptionEvent() {
        return exceptionEvent;
    }

    @Override
    public Observable<SubscribeResult> getSubscribeEvent() {
        return subscribeEvent;
    }

    public ImportantTaskExecutor getImportantTaskExecutor() {
        return importantTaskExecutor;
    }

    public void setImportantTaskExecutor(ImportantTaskExecutor importantTaskExecutor) {
        this.importantTaskExecutor = importantTaskExecutor;
    }

    public BufferedOrderedQueue<DeliveredMessage> getBufferedOrderedQueue() {
        return bufferedOrderedQueue;
    }

    public void setBufferedOrderedQueue(BufferedOrderedQueue<DeliveredMessage> bufferedOrderedQueue) {
        if (this.bufferedOrderedQueue != null) {
            this.bufferedOrderedQueue.getItemEvent().removeObserver(releaseMessageObserver);
        }

        this.bufferedOrderedQueue = bufferedOrderedQueue;

        if (this.bufferedOrderedQueue != null) {
            this.bufferedOrderedQueue.getItemEvent().addObserver(releaseMessageObserver);
        }
    }

    @Override
    public UserAgent getUserAgent() {
        if (presence == null) {
            return null;
        }

        return presence.getUserAgent();
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public Observable<SubscribeResult> getUnsubscribeEvent() {
        return unsubscribeEvent;
    }

    public void setSignalsSubscribeActor(SignalsSubscribeActor signalsSubscribeActor) {
        this.signalsSubscribeActor = signalsSubscribeActor;
    }

    public SignalsSubscribeActor getSignalsSubscribeActor() {
        return signalsSubscribeActor;
    }

    @Override
    public boolean isConnected() {
        return signalConnection.isConnected();
    }

    public SignalConnection getSignalConnection() {
        return signalConnection;
    }

    public Gson getGson() {
        return gson;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    @Override
    protected void onDestroy() {

    }

    @Override
    public ObservableFuture<ObservableFuture<Object[]>> emit(String event, Object... objects) {
        return signalConnection.emit(event, objects);
    }

    public void setSignalConnection(SignalConnection signalConnection) {
        if (this.signalConnection != null) {
            this.signalConnection.getConnectEvent().removeObserver(connectionChangedEvent);
            this.signalConnection.getDisconnectEvent().removeObserver(connectionChangedEvent);
            this.signalConnection.getExceptionEvent().removeObserver(exceptionEvent);
            this.signalConnection.getMessageEvent().removeObserver(messageProcessingObserver);
            this.signalConnection.getServerEvent().removeObserver(serverEventProcessingObserver);
        }

        this.signalConnection = signalConnection;
        this.signalConnection.getConnectEvent().addObserver(issueBindRequestOnConnectCallback);

        if (this.signalConnection != null) {
            this.signalConnection.getConnectEvent().addObserver(connectionChangedEvent);
            this.signalConnection.getDisconnectEvent().addObserver(connectionChangedEvent);
            this.signalConnection.getExceptionEvent().addObserver(exceptionEvent);
            this.signalConnection.getMessageEvent().addObserver(messageProcessingObserver);
            this.signalConnection.getServerEvent().addObserver(serverEventProcessingObserver);
        }
    }

    private class SignalUnsubscribeCallback implements Callable<ObservableFuture<Void>> {

        private final String sessionKey;
        private final String subscriptionId;

        private SignalUnsubscribeCallback(String sessionKey, String subscriptionId) {
            this.sessionKey = sessionKey;
            this.subscriptionId = subscriptionId;
        }

        @Override
        public ObservableFuture<Void> call() throws Exception {
            // NOTE: the connected state is not something that is able to stay constant.
            // For example, we just checked isConnected() but it might have changed a nanosecond after we checked it!

            // If the server hasn't fully processed your subscription (and sent a SubscriptionCompleteCommand) then
            // we're making a noop call here. It's ok. I don't mind the extra call to the web.
            ObservableFuture<Void> future = signalsSubscribeActor.unsubscribe(getClientId(), sessionKey, subscriptionId);

            // If disconnected successfully, remove from local list.
            future.addObserver(new Observer<ObservableFuture<Void>>() {
                @Override
                public void notify(Object sender, ObservableFuture<Void> item) {
                    if (item.isFailed()) {
                        subscribeEvent.notifyObservers(SignalProviderImpl.this, new SubscribeResult(sessionKey, subscriptionId, item.getCause()));
                        return;
                    }

                    if (item.isCancelled()) {
                        subscribeEvent.notifyObservers(SignalProviderImpl.this, new SubscribeResult(sessionKey, subscriptionId, new CancellationException()));
                        return;
                    }

                    subscribeEvent.notifyObservers(SignalProviderImpl.this, new SubscribeResult(sessionKey, subscriptionId));
                }
            });

            return future;
        }
    }

    private class SignalSubscribeCallback implements Callable<ObservableFuture<SubscribeResult>> {

        private final String sessionKey;
        private final String subscriptionId;
        private final String scope;

        private SignalSubscribeCallback(String sessionKey, String subscriptionId) {
            this(sessionKey, subscriptionId, null);
        }

        private SignalSubscribeCallback(String sessionKey, String subscriptionId, String scope) {
            this.sessionKey = sessionKey;
            this.subscriptionId = subscriptionId;
            this.scope = scope;
        }

        @Override
        public ObservableFuture<SubscribeResult> call() throws Exception {
            MutableObservableFuture<SubscribeResult> result = future();
            final SubscriptionRequest request = new SubscriptionRequest(subscriptionId, sessionKey, result);

            // Make sure we're the only request that's processing.
            if (!addRequest(request)) {
                result.cancel();

                return result;
            }

            // Make the call to the server (async).
            ObservableFuture<Void> future = signalsSubscribeActor.subscribe(getClientId(), sessionKey, scope, subscriptionId);

            // We ONLY want to cascade the failure.
            // The success will come later when the SubscriptionCompleteCommand comes in.
            future.addObserver(new CascadeFailureObserver<Void>(result));

            // Clean up the pending request map.
            final String finalSubscriptionId = subscriptionId;
            future.addObserver(new Observer<ObservableFuture<Void>>() {
                @Override
                public void notify(Object sender, ObservableFuture<Void> item) {
                    if (item.isSuccess()) {
                        // We only want to clean up after failed requests.
                        // Otherwise, the SubscriptionComplete will clean up later.
                        return;
                    }

                    synchronized (SignalProviderImpl.this) {
                        SubscriptionRequest request1 = pendingSubscriptionRequests.get(finalSubscriptionId);

                        // Only remove if it's the same one.
                        if (request1 == request) {
                            pendingSubscriptionRequests.remove(finalSubscriptionId);

                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(String.format("Removed \"%s\" from pendingSubscriptionRequests", finalSubscriptionId));
                            }
                        }
                    }
                }
            });

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Finished requesting /signal/subscribe. Now we're waiting for a SubscriptionComplete to come down the wire!");
            }

            return result;
        }

        private boolean addRequest(SubscriptionRequest request) {
            //
            // Make sure we're the only request that's being processed right now.
            //
            synchronized (SignalProviderImpl.this) {
                SubscriptionRequest existingSubscriptionRequest = pendingSubscriptionRequests.get(subscriptionId);

                if (existingSubscriptionRequest != null) {
                    // Something is already cooking. Cancel ours.
                    request.getFuture().cancel();

                    return false;
                }

                pendingSubscriptionRequests.put(subscriptionId, request);
            }

            return true;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("SignalSubscribeCallback{");
            sb.append("sessionKey='").append(sessionKey).append('\'');
            sb.append(", subscriptionId='").append(subscriptionId).append('\'');
            sb.append(", scope='").append(scope).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    private static class CascadeFailureObserver<T> implements Observer<ObservableFuture<T>> {

        private final MutableObservableFuture result;

        public CascadeFailureObserver(MutableObservableFuture result) {
            this.result = result;
        }

        @Override
        public void notify(Object sender, ObservableFuture<T> item) {
            if (item.isFailed()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Sync failure from %s to %s", item, result));
                }

                NestedObservableFuture.syncFailure(item, result);
            }
        }
    }

    /**
     * Our job is to do 2 things.
     * <p/>
     * 1. Connect to the server.
     * 2. Bind to the server (send a BindRequest)
     */
    private class ConnectTask implements Callable<ObservableFuture<Void>> {

        private final MutableObservableFuture<Void> resultFuture;

        public ConnectTask(MutableObservableFuture<Void> resultFuture) {
            this.resultFuture = resultFuture;
        }

        @Override
        public ObservableFuture<Void> call() throws Exception {
            if (pingFuture != null) {
                LOGGER.debug("Cancelling existing pingFuture, as we're now connecting.");

                clearPingFuture();
            }

            ObservableFuture<Void> connectFuture = signalConnection.connect();

            connectFuture.addObserver(
                    new ThreadSafeObserver<Void>(
                            new Observer<ObservableFuture<Void>>() {
                                @Override
                                public void notify(Object sender, ObservableFuture<Void> item) {
                                    if (cascadeFailure(item, resultFuture)) {
                                        return;
                                    }

                                    // Our socket is connected, now we need to issue a bind request
                                    String _clientId = getClientId();
                                    String _token = token;

                                    final ObservableFuture<BindResult> _bindFuture = executeBindRequest(_clientId, _token, false);

                                    // When this future is successful, we need to save the details
                                    _bindFuture.addObserver(new Observer<ObservableFuture<BindResult>>() {
                                        @Override
                                        public void notify(Object sender, ObservableFuture<BindResult> item) {
                                            synchronized (SignalProviderImpl.this) {
                                                if (item.isFailed()) {
                                                    LOGGER.error("Bind future not successful!", item.getCause());
                                                    if (resultFuture != null) {
                                                        resultFuture.setFailure(new Exception("The bindFuture was not successful", item.getCause()));
                                                    }

                                                    return;
                                                } else if (item.isCancelled()) {
                                                    LOGGER.error("Bind future cancelled! " + item);
                                                    if (resultFuture != null) {
                                                        resultFuture.cancel();
                                                    }

                                                    return;
                                                }

                                                // Success!
                                                if (resultFuture != null) {
                                                    resultFuture.setSuccess(null);
                                                }
                                            }
                                        }
                                    });

                                    // I removed this because the connectionChangedEvent was firing twice.
                                    // Once from us, and once from the underlying library
                                    // connectionChangedEvent.notifyObservers(SignalProviderImpl.this, null);
                                }
                            }
                    )
            );

            return resultFuture;
        }
    }

    private class ThreadSafeObserver<T> implements Observer<ObservableFuture<T>> {

        private final Observer<ObservableFuture<T>> observer;

        private ThreadSafeObserver(Observer<ObservableFuture<T>> observer) {
            this.observer = observer;
        }

        @Override
        public void notify(final Object sender, final ObservableFuture<T> item) {
            runSynchronized(new Runnable() {
                @Override
                public void run() {
                    observer.notify(sender, item);
                }

                @Override
                public String toString() {
                    return "[ObserverAdapter:" + observer + "]";
                }
            });
        }
    }

    private void runSynchronized(final Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable");
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Synchronizing before running: " + runnable);
                }

                synchronized (SignalProviderImpl.this) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Running: " + runnable);
                    }

                    runnable.run();
                }
            }
        });
    }

    private void clearPingFuture() {
        pingFuture.cancel();
        pingFuture = null;
    }

    private class ReconnectOnFailureObserver implements Observer<ObservableFuture<BindResult>> {

        private final ObservableFuture<BindResult> bindFuture;

        private ReconnectOnFailureObserver(ObservableFuture<BindResult> bindFuture) {
            this.bindFuture = bindFuture;
        }

        @Override
        public void notify(Object sender, ObservableFuture<BindResult> item) {
            synchronized (SignalProviderImpl.this) {
                if (item.isSuccess()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Decided not to fire our ReconnectOnFailureObserver because future was successful.");
                    }

                    return;
                }

                if (bindFuture != item) {
                    LOGGER.warn(String.format("The bindFutures were not equal. Did we just prevent a bug? %s/%s", bindFuture, SignalProviderImpl.this.bindFuture));
                    return;
                }

                LOGGER.debug("Reconnecting because the bind future failed.");
                forceDisconnectAndReconnect();
            }
        }
    }

    private class DisconnectOnFailureObserver implements Observer<ObservableFuture<BindResult>> {

        private final ObservableFuture<BindResult> bindFuture;

        private DisconnectOnFailureObserver(ObservableFuture<BindResult> bindFuture) {
            this.bindFuture = bindFuture;
        }

        @Override
        public void notify(Object sender, ObservableFuture<BindResult> item) {
            synchronized (SignalProviderImpl.this) {
                if (item.isSuccess()) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Decided not to fire our DisconnectOnFailureObserver because future was successful.");
                    }

                    return;
                }

                if (bindFuture != item) {
                    LOGGER.warn(String.format("The bindFuture were not equal. Did we just prevent a bug? %s/%s", bindFuture, SignalProviderImpl.this.bindFuture));
                    return;
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Disconnecting the connection because the future failed.");
                }

                signalConnection.disconnect();
            }
        }
    }

    /**
     * This BindCallback allows us to wrap the event in a cancellable future.
     */
    private static class BindTask implements Callable<ObservableFuture<BindResult>> {

        private final BindRequest request;
        private final SignalConnection connection;
        private final MutableObservableFuture<BindResult> result;
        private final Gson gson;

        private BindTask(SignalConnection connection, BindRequest request, Executor executor, Gson gson) {
            this.request = request;
            this.connection = connection;
            this.result = new DefaultObservableFuture<BindResult>(this, executor, "BindFuture");
            this.gson = gson;
        }

        @Override
        public ObservableFuture<BindResult> call() throws Exception {
            ObservableFuture<ObservableFuture<Object[]>> emitFuture = connection.emit("bind", request);

            emitFuture.addObserver(new ObserveBindAcknowledgementObserver(gson, result));

            return result;
        }
    }

    private static class ObserveBindAcknowledgementObserver implements Observer<ObservableFuture<ObservableFuture<Object[]>>> {

        private final Gson gson;
        private final MutableObservableFuture<BindResult> resultFuture;

        private ObserveBindAcknowledgementObserver(Gson gson, MutableObservableFuture<BindResult> resultFuture) {
            this.gson = gson;
            this.resultFuture = resultFuture;
        }

        @Override
        public void notify(Object sender, ObservableFuture<ObservableFuture<Object[]>> item) {
            if (cascadeFailure(item, resultFuture)) {
                return;
            }

            ObservableFuture<Object[]> ackFuture = item.getResult();

            ackFuture.addObserver(new ProcessBindResponseObserver(gson, resultFuture));
        }
    }

    private static class ProcessBindResponseObserver implements Observer<ObservableFuture<Object[]>> {

        private final Gson gson;
        private final MutableObservableFuture<BindResult> resultFuture;

        private ProcessBindResponseObserver(Gson gson, MutableObservableFuture<BindResult> resultFuture) {
            this.gson = gson;
            this.resultFuture = resultFuture;
        }

        @Override
        public void notify(Object sender, ObservableFuture<Object[]> item) {
            if (cascadeFailure(item, resultFuture)) {
                return;
            }

            Object[] objects = item.getResult();

            processAcknowledgementFromServer(objects);
        }

        private void processAcknowledgementFromServer(Object[] args) {
            if (CollectionUtil.isNullOrEmpty(args)) {
                resultFuture.setFailure(new Exception("No bind response received"));
                return;
            }

            Object object = args[0];
            if (!(object instanceof JsonObject)) {
                resultFuture.setFailure(new Exception("Wrong type: " + object));
                return;
            }

            // TODO: Detect and handle failure
            BindResult response = gson.fromJson((JsonObject) args[0], BindResult.class);

            resultFuture.setSuccess(response);
        }
    }

    private static class PingTask implements Callable<ObservableFuture<String>> {

        private final SignalConnection connection;
        private final MutableObservableFuture<String> result;
        private final Long payload;

        private PingTask(SignalConnection connection, MutableObservableFuture<String> result, Long payload) {
            this.connection = connection;
            this.result = result;
            this.payload = payload;
        }

        @Override
        public ObservableFuture<String> call() throws Exception {
            if (!connection.isConnected()) {
                LOGGER.error("Can't ping, not connected!");

                return new FakeFailingObservableFuture<String>(this, null);
            }

            ObservableFuture<ObservableFuture<Object[]>> emitFuture = connection.emit("ping", payload);

            emitFuture.addObserver(new ObservePingAcknowledgementObserver(result));

            return result;
        }
    }

    private static class ObservePingAcknowledgementObserver implements Observer<ObservableFuture<ObservableFuture<Object[]>>> {

        private final MutableObservableFuture<String> resultFuture;

        private ObservePingAcknowledgementObserver(MutableObservableFuture<String> resultFuture) {
            this.resultFuture = resultFuture;
        }

        @Override
        public void notify(Object sender, ObservableFuture<ObservableFuture<Object[]>> item) {
            if (cascadeFailure(item, resultFuture)) {
                return;
            }

            ObservableFuture<Object[]> ackFuture = item.getResult();

            ackFuture.addObserver(new ProcessPingResponseObserver(resultFuture));
        }
    }

    private static class ProcessPingResponseObserver implements Observer<ObservableFuture<Object[]>> {

        private final MutableObservableFuture<String> resultFuture;

        private ProcessPingResponseObserver(MutableObservableFuture<String> resultFuture) {
            this.resultFuture = resultFuture;
        }

        @Override
        public void notify(Object sender, ObservableFuture<Object[]> item) {
            if (cascadeFailure(item, resultFuture)) {
                return;
            }

            Object[] objects = item.getResult();

            processAcknowledgementFromServer(objects);
        }

        private void processAcknowledgementFromServer(Object[] args) {
            if (CollectionUtil.isNullOrEmpty(args)) {
                resultFuture.setFailure(new Exception("No ping response received"));
                return;
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Processing ping response: " + args[0]);
            }

            try {
                String response = ((JsonObject) args[0]).get("data").toString();
                resultFuture.setSuccess(response);
            } catch (Exception e) {
                resultFuture.setFailure(e);
            }
        }
    }

    /**
     * Date: 8/22/13
     * Time: 3:42 PM
     *
     * @author Michael
     * @version 1
     */
    public static class SubscriptionRequest {

        private MutableObservableFuture<SubscribeResult> future;
        private String sessionKey;
        private String subscriptionId;

        public SubscriptionRequest(String subscriptionId, String sessionKey, MutableObservableFuture<SubscribeResult> future) {
            this.subscriptionId = subscriptionId;
            this.sessionKey = sessionKey;
            this.future = future;
        }

        public MutableObservableFuture<SubscribeResult> getFuture() {
            return future;
        }

        public void setFuture(MutableObservableFuture<SubscribeResult> future) {
            this.future = future;
        }

        public String getSessionKey() {
            return sessionKey;
        }

        public void setSessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SubscriptionRequest)) return false;

            SubscriptionRequest request = (SubscriptionRequest) o;

            if (!subscriptionId.equals(request.subscriptionId)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return subscriptionId.hashCode();
        }
    }

    /**
     * Helper function to check for failure.
     * <p/>
     * Return TRUE if the future was handled.
     *
     * @param source
     * @param destination
     * @return
     */
    private static boolean cascadeFailure(ObservableFuture<?> source, MutableObservableFuture<?> destination) {
        if (source.isCancelled()) {
            destination.cancel();
            return true;
        } else if (source.isFailed()) {
            destination.setFailure(source.getCause());
            return true;
        } else if (!source.isDone()) {
            return true;
        }

        return false;
    }

}
