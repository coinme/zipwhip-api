package com.zipwhip.api;

import com.ning.http.client.AsyncHttpClient;
import com.zipwhip.api.settings.MemorySettingStore;
import com.zipwhip.api.settings.SettingsStore;
import com.zipwhip.api.signals.*;
import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.api.signals.dto.SubscribeResult;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.MutableObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.CommonExecutorFactory;
import com.zipwhip.executors.CommonExecutorTypes;
import com.zipwhip.executors.NamedThreadFactory;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.important.schedulers.TimerScheduler;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.reliable.retry.ExponentialBackoffRetryStrategy;
import com.zipwhip.reliable.retry.RetryStrategy;
import com.zipwhip.signals2.message.Message;
import com.zipwhip.signals2.presence.UserAgent;
import com.zipwhip.timers.HashedWheelTimer;
import com.zipwhip.timers.Timeout;
import com.zipwhip.timers.Timer;
import com.zipwhip.timers.TimerTask;
import com.zipwhip.util.FutureDateUtil;
import com.zipwhip.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Michael
 * @date 4/8/2014
 */
public class SimpleZipwhipClient extends CascadingDestroyableBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleZipwhipClient.class);

    private final ObservableHelper<ConnectionState> connectionChangedObservableHelper;
    private final ObservableHelper<Message> signalReceivedObservableHelper;

    private SettingsStore settingsStore;
    private CommonExecutorFactory executorFactory;
    private ImportantTaskExecutor importantTaskExecutor;
    private Timer timer;
    private SignalsSubscribeActor signalsSubscribeActor;
    private ExecutorService executorService;
    private ExecutorService eventExecutor = SimpleExecutor.getInstance();

    private RetryStrategy retryStrategy;

    private SignalProvider signalProvider;
    private UserAgent userAgent;
    private String url = "http://us1.signals.zipwhip.com";
    private String scope;

    private volatile int __unsafe_attemptCount;
    private volatile ObservableFuture<Void> __unsafe_innerConnectFuture;
    private volatile MutableObservableFuture<Void> __unsafe_externalConnectFuture;
    private volatile ConnectionState __unsafe_connectionState = ConnectionState.DISCONNECTED;

    public SimpleZipwhipClient() {
        connectionChangedObservableHelper = new ObservableHelper<ConnectionState>("connectionChanged");
        signalReceivedObservableHelper = new ObservableHelper<Message>("signalReceived");
    }

    private synchronized void init() {
        if (timer == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Injected timer was null. Creating our own internally. It will be destroyed/stopped when we are destroyed.");
            }

            timer = new HashedWheelTimer(new NamedThreadFactory("SimpleZipwhipClientTimer"));
            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    timer.stop();
                }
            });
        }

        if (executorFactory == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Injected executorFactory was null. Creating our own internally.");
            }

            executorFactory = new CommonExecutorFactory() {

                @Override
                public ExecutorService create(CommonExecutorTypes type, String name) {
                    return Executors.newSingleThreadExecutor(new NamedThreadFactory(name));
                }

                @Override
                public ExecutorService create() throws Exception {
                    return create(null, "");
                }
            };
        }

        if (executorService == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Injected executorService was null. Creating our own internally. It is setup as type BOSS");
            }

            executorService = executorFactory.create(CommonExecutorTypes.BOSS, "SimpleZipwhipClient");
        }

        if (importantTaskExecutor == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Injected importantTaskExecutor was null. Creating our own internally. It is setup using our timer.");
            }

            importantTaskExecutor = new ImportantTaskExecutor(new TimerScheduler(timer));
        }

        if (settingsStore == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Injected settingsStore was null. Creating our own internally. It is setup as type Memory (will be destroyed/cleared when the jvm is stopped)");
            }

            settingsStore = new MemorySettingStore();
        }

        if (signalsSubscribeActor == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Injected signalsSubscribeActor was null. Creating our own internally. It will use Ning (with defaults) internally and have its own threadpool. The threads will be shutdown when we are destroyed.");
            }

            final AsyncHttpClient client = new AsyncHttpClient();
            signalsSubscribeActor = new NingSignalsSubscribeActor(
                    client,
                    "http://network.zipwhip.com/signal/subscribe");

            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    client.close();
                }
            });
        }

        if (retryStrategy == null) {
            retryStrategy = new ExponentialBackoffRetryStrategy();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Injected retryStrategy was null. Creating our own internally. " + retryStrategy);
            }
        }
    }

    /**
     * Will connect to the signal server. If the signal server is down, will retry until it comes up. To stop this behavior,
     * call disconnect.
     *
     * @param sessionKey
     * @throws java.lang.Exception If already connected. Disconnect first.
     */
    public synchronized ObservableFuture<Void> connect(String sessionKey) throws Exception {
        // Just in case, auto init.
        init();

        // If they are already logged in. Kick them off.
        {
            final ConnectionState finalConnectionState = finalConnectionState();

            if (isLoggedIn()) {
                throw new Exception("Already logged in. Logout first.");
            } else if (finalConnectionState != ConnectionState.DISCONNECTED) {
                throw new Exception("The current state is not disconnected. " + finalConnectionState);
            }
        }

        // Basic parameter checking.
        {
            if (userAgent == null) {
                throw new NullPointerException("The userAgent cannot be null");
            } else if (StringUtil.isNullOrEmpty(sessionKey)) {
                throw new NullPointerException("The sessionKey cannot be null");
            }
        }

        final ObservableFuture<Void> existingFinalInnerConnectFuture = finalInnerConnectFuture();

        if (existingFinalInnerConnectFuture != null) {
            // I'm deciding to just reject this request entirely.
            // Maybe in the future we can do some sort of cleanup.
            throw new Exception("Already trying to connect.");
        }

        // Clear all the settings in the store, this is a fresh login.

        if (!__unsafe_isReturningUser(sessionKey)) {
            // Clear out any saved settings.
            // I would prefer to do settingsStore.clear(), but I don't want to screw up the caller.
            this.settingsStore.remove(SettingsStore.Keys.SESSION_KEY);
            this.settingsStore.remove(SettingsStore.Keys.CLIENT_ID);
            this.settingsStore.remove(SettingsStore.Keys.VERSIONS);
            this.settingsStore.remove(SettingsStore.Keys.EXPECTS_SUBSCRIPTION_COMPLETE);
            this.settingsStore.remove(SettingsStore.Keys.LAST_SUBSCRIBED_CLIENT_ID);
        }

        // We are changing the sessionKey on people. This needs to be executed in a synchronized block.
        this.settingsStore.put(SettingsStore.Keys.SESSION_KEY, sessionKey);

        // The connectionState cannot change, unless we change it.
        // Nobody can change it without holding the lock that we hold.
        assertConnectionState(ConnectionState.DISCONNECTED);

        // There should be no SignalProvider present.
        final SignalProvider finalSignalProvider = createSignalProvider();

        setSignalProvider(finalSignalProvider);

        __unsafe_connect();

        final MutableObservableFuture<Void> finalExternalConnectFuture = setExternalConnectFutureIfNull(future("externalConnectFuture"));

        // TODO: link together the innerConnectFuture and the externalConnectFuture
        finalExternalConnectFuture.addObserver(new Observer<ObservableFuture<Void>>() {
            @Override
            public void notify(Object sender, ObservableFuture<Void> item) {
                synchronized (SimpleZipwhipClient.this) {
                    clearExternalConnectFutureIf(item);
                }
            }
        });

        return finalExternalConnectFuture;
    }

    private MutableObservableFuture<Void> setExternalConnectFutureIfNull(MutableObservableFuture<Void> future) {
        final MutableObservableFuture<Void> finalExternalConnectFuture = finalExternalConnectFuture();

        if (finalExternalConnectFuture != null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("The finalExternalConnectFuture was not null, so not setting to: " + future);
            }
        } else {
            __unsafe_externalConnectFuture = future;
        }

        return future;
    }

    private void clearExternalConnectFutureIf(ObservableFuture<Void> item) {
        final MutableObservableFuture<Void> finalExternalConnectFuture = finalExternalConnectFuture();

        if (finalExternalConnectFuture != item) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("The (finalExternalConnectFuture:%s) was not equal to (future:%s). So not resetting the externalConnectFuture!", finalExternalConnectFuture, item));
            }

            return;
        }

        __unsafe_externalConnectFuture = null;
    }

    private MutableObservableFuture<Void> future(final String name) {
        return new DefaultObservableFuture<Void>(this, eventExecutor, name);
    }

    private ObservableFuture<Void> __unsafe_connect() {
        setConnectionState(ConnectionState.CONNECTING);

        final UserAgent userAgent = this.getFinalUserAgent();
        final String clientId = this.finalSetting(SettingsStore.Keys.CLIENT_ID);
        final String token = this.finalSetting(SettingsStore.Keys.CLIENT_ID_TOKEN);

        // On first connect, the clientId/token will be null. That's ok.
        // On reconnect with new sessionKey, the clientId/token will be null. That's ok.

        final ObservableFuture<Void> finalInnerConnectFuture = this.setConnectFuture(signalProvider.connect(userAgent, clientId, token));

        // We need to 'keep trying forever' if this fails.
        // The underlying signalProvider will not retry this error.
        // It's our job to notice the disconnect and do a reconnect.
        finalInnerConnectFuture
                .addObserver(
                        // This ConnectFutureObserver will only operate if the underlying signalProvider is not changed.
                        new ConnectFutureObserver(signalProvider));

        return finalInnerConnectFuture;
    }

    private void runOnSameSignalProvider(final Runnable runnable) {
        final SignalProvider signalProvider1 = getFinalSignalProvider();

        runOnSameSignalProvider(signalProvider1, runnable);
    }

    private void runOnSameSignalProvider(final SignalProvider signalProvider, final Runnable runnable) {
        run(new Runnable() {
            @Override
            public void run() {
                synchronized (SimpleZipwhipClient.this) {
                    assertSameSignalProvider(signalProvider);

                    runnable.run();
                }
            }
        });
    }


    private void assertSameSignalProvider(SignalProvider signalProvider) {
        final SignalProvider finalSignalProvider = getFinalSignalProvider();

        if (signalProvider != finalSignalProvider) {
            throw new RuntimeException(String.format("Expected signalProvider(%s) but found (%s)", signalProvider, finalSignalProvider));
        }
    }

    private void runOnSameConnectionState(final Runnable runnable) {
        final ConnectionState finalConnectionState = finalConnectionState();

        runOnSameConnectionState(finalConnectionState, runnable);
    }

    private void runOnSameConnectionState(final ConnectionState finalConnectionState, final Runnable runnable) {
        runOnSameAccount(new Runnable() {
            @Override
            public void run() {
                assertConnectionState(finalConnectionState);

                runnable.run();
            }
        });
    }

    private void assertConnectionState(ConnectionState connectionState) {
        final ConnectionState finalConnectionState = finalConnectionState();

        if (connectionState != finalConnectionState) {
            throw new RuntimeException(String.format("The connectionState was supposed to be %s, but was %s", connectionState, finalConnectionState));
        }
    }

    private String getFinalSessionKey() {
        accessSessionKey();

        return this.finalSetting(SettingsStore.Keys.SESSION_KEY);
    }

    private void accessSessionKey() {
        assertHoldsLock(this);
    }

    /**
     * You will be inside a synchronized block when you run.
     *
     * @param runnable
     */
    private void runOnSameAccount(final Runnable runnable) {
        final String originalFinalSessionKey = this.getFinalSessionKey();
        // Only Run on the same account.
        run(new Runnable() {
            @Override
            public void run() {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Running " + this);
                }

                synchronized (SimpleZipwhipClient.this) {
                    final String finalSessionKey = getFinalSessionKey();

                    if (!StringUtil.equalsIgnoreCase(originalFinalSessionKey, finalSessionKey)) {
                        LOGGER.error(String.format("sessionKey mismatch (%s/%s). Ignoring runnable: %s", originalFinalSessionKey, finalSessionKey, runnable));
                        return;
                    }

                    runnable.run();
                }
            }
        });
    }

    private void run(final Runnable runnable) {
        final long enqueueTime = System.currentTimeMillis();

        if (executorService == null) {
            throw new NullPointerException("The executorService was null. Did you forget to call .init()?");
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                long beforeRunTime = System.currentTimeMillis();
                long enqueueDuration = calculateRunTimeInMs(enqueueTime);

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("Running: (%s). Waited: (%s).", runnable, enqueueDuration));
                }

                try {
                    runnable.run();
                } finally {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(String.format("Ran: (%s). Waited(%s). Executed(%s).", runnable, enqueueDuration, calculateRunTimeInMs(beforeRunTime)));
                    }
                }
            }
        });
    }

    private long calculateRunTimeInMs(long beforeRunTime) {
        return System.currentTimeMillis() - beforeRunTime;
    }

    private synchronized SignalProvider createSignalProvider() {
        // We want all the events to fire in our boss thread.
        // Pass in the eventExecutor.
        final SignalProviderImpl finalSignalProvider = new SignalProviderImpl(this.executorService);

        SocketIoSignalConnection connection = new SocketIoSignalConnection();

        {
            final ExecutorService e1 = executorFactory.create(CommonExecutorTypes.EVENTS, "SignalProviderEvents");
            connection.setEventExecutor(e1);
            finalSignalProvider.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    e1.shutdown();
                }
            });
        }

        {
            final ExecutorService e2 = executorFactory.create(CommonExecutorTypes.BOSS, "SignalProviderBoss");
            connection.setExecutor(e2);
            finalSignalProvider.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    e2.shutdown();
                }
            });
        }

        connection.setImportantTaskExecutor(importantTaskExecutor);
        connection.setRetryStrategy(retryStrategy);
        connection.setTimer(timer);
        connection.setUrl(url);

        finalSignalProvider.setSignalConnection(connection);
        finalSignalProvider.setImportantTaskExecutor(importantTaskExecutor);
        finalSignalProvider.setBufferedOrderedQueue(new SilenceOnTheLineBufferedOrderedQueue<DeliveredMessage>(timer));
        finalSignalProvider.setSignalsSubscribeActor(signalsSubscribeActor);

        return finalSignalProvider;
    }

    private ObservableFuture<Void> setConnectFuture(ObservableFuture<Void> connectFuture) {
        accessConnectFuture();

        this.__unsafe_innerConnectFuture = connectFuture;

        return connectFuture;
    }

    private class ConnectFutureObserver implements Observer<ObservableFuture<Void>> {

        private final SignalProvider signalProvider;

        private ConnectFutureObserver(SignalProvider signalProvider) {
            this.signalProvider = signalProvider;
        }

        @Override
        public void notify(Object sender, final ObservableFuture<Void> future) {
            runOnSameSignalProvider(signalProvider, new Runnable() {
                @Override
                public void run() {
                    // Make sure we are still in the process of connecting.
                    assertConnectionState(ConnectionState.CONNECTING);

                    // We are now on the central thread (outside of the SignalProvider thread)
                    // We are not guaranteed that this is a single threaded executor.
                    // I will attempt to communicate that it needs to be.
                    final ObservableFuture<Void> finalInnerConnectFuture = finalInnerConnectFuture();

                    if (finalInnerConnectFuture != future) {
                        // This is weird.
                        LOGGER.error(String.format("The connectFuture was not the future we expected. Ignoring request. It was [%s] and we expected [%s]", finalInnerConnectFuture, future));
                        return;
                    }

                    // Clear it out, this means that another connect could be initiated.
                    clearInnerConnectFuture();

                    if (future.isSuccess()) {
                        setConnectionState(ConnectionState.CONNECTED);

                        __unsafe_subscribe(signalProvider);
                    } else {
                        // TODO: test me.
                        // Our job is to try to connect forever.
                        setConnectionState(ConnectionState.INTERRUPTED_WAITING_TO_RETRY);

                        __unsafe_reconnectLater();
                    }
                }
            });
        }
    }

    private void __unsafe_subscribe(SignalProvider signalProvider) {
        final ObservableFuture<SubscribeResult> finalSubscribeFuture = __unsafe_issueSubscribeRequest();

        // The subscribeFuture already has timeout baked in.
        // We do not need to worry about adding another layer of timeout.
        // On failure, we need to retry.
        // On success, we need to clear the future.

        finalSubscribeFuture.addObserver(new SubscribeObserver(signalProvider));
    }

    private class SubscribeObserver implements Observer<ObservableFuture<SubscribeResult>> {

        private final SignalProvider signalProvider;

        private SubscribeObserver(SignalProvider signalProvider) {
            this.signalProvider = signalProvider;
        }

        @Override
        public void notify(Object sender, ObservableFuture<SubscribeResult> innerSubscribeFuture) {
            // We are in our boss thread (because we passed in the boss thread to the SignalProvider constructor

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("The subscribeFuture finished: " + innerSubscribeFuture);
            }

            // We are in our boss thread, now need to capture the lock
            synchronized (SimpleZipwhipClient.this) {
                assertConnectionState(ConnectionState.SUBSCRIBING);

                if (innerSubscribeFuture.isFailed()) {
                    // The subscribe failed. We need to try again.
                    // Rather than tear everything down, let's just issue another one after a short delay.
                    LOGGER.error("Failed to subscribe. It may have timed out, or the web call failed.");

                    setConnectionState(ConnectionState.SUBSCRIBE_FAILED_WAITING_TO_RETRY);

                    // We pass in the signalProvider
                    issueSubscribeRequestLater(signalProvider);
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("The subscribe succeeded. " + innerSubscribeFuture.getResult());
                    }

                    setConnectionState(ConnectionState.SUBSCRIBED_AND_WORKING);

                    final MutableObservableFuture<Void> finalExternalConnectFuture = finalExternalConnectFuture();

                    // We should be good to go. Let's finish up the connectFuture if we haven't already.
                    if (finalExternalConnectFuture != null) {
                        // It will automatically be set to null after we call success or failure
                        finalExternalConnectFuture.setSuccess(null);
                    }

                    // We are connected and working.
                    clearAttemptCount();
                }
            }
        }
    }

    private void issueSubscribeRequestLater(final SignalProvider signalProvider) {
        // NOTE: This might be subscribing too aggressively.
        // The server might not be able to keep up and this would exacerbate the problem
        long retryMillis = getNextRetryInterval() * 2;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SubscribeLater: Subscribing in the future: " + FutureDateUtil.inFuture(retryMillis, TimeUnit.MILLISECONDS));
        }

        timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                runOnSameSignalProvider(signalProvider, new Runnable() {
                    @Override
                    public void run() {
                        // We are in the boss thread
                        // We are already synchronized
                        // Will throw if there is a problem with the state
                        assertConnectionState(ConnectionState.SUBSCRIBE_FAILED_WAITING_TO_RETRY);

                        // We are in a good state. Let's try to subscribe again.
                        __unsafe_subscribe(signalProvider);
                    }
                });
            }
        }, retryMillis, TimeUnit.MILLISECONDS);
    }

    private void clearAttemptCount() {
        assertHoldsLock(this);

        __unsafe_attemptCount = 0;
    }

    private String finalSetting(SettingsStore.Keys key) {
        assertHoldsLock(this);
        return this.settingsStore.get(key);
    }

    private SignalProvider setSignalProvider(SignalProvider signalProvider) {
        accessSignalProvider();

        final SignalProvider oldSignalProvider = this.signalProvider;

        if (oldSignalProvider != null) {
            detachFromSignalProvider(oldSignalProvider);
        }

        this.signalProvider = signalProvider;

        if (signalProvider != null) {
            // Attach listeners. We will swap out a new instance of SignalProvider each time
            attachToSignalProvider(signalProvider);
        }

        return signalProvider;
    }

    /**
     * @return an unchanging signal provider.
     */
    private SignalProvider getFinalSignalProvider() {
        accessSignalProvider();

        return signalProvider;
    }

    private void accessSignalProvider() {
        assertHoldsLock(this);
    }

    /**
     * This TimerTask is a singleton.
     * Its job is to trigger a connect() request once the time expires.
     * We need to detect if we are already connected. If so, then ignore this trigger.
     */
    private class ReconnectTimerTask implements TimerTask {

        private final SignalProvider signalProvider;

        private ReconnectTimerTask(SignalProvider signalProvider) {
            this.signalProvider = signalProvider;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            // A small delay has occurred.
            // It could have been 10 ms or 1 day, depending on the ReconnectStrategy state.

            // We are in some unknown timer thread. We need to convert over to the core thread.
            // We have no idea what the current SignalProvider/Session/ConnectionState is right now.
            runOnSameSignalProvider(signalProvider, new Runnable() {
                @Override
                public void run() {
                    // We are in the core thread.
                    // We are guaranteed that the connectionState was the same as it was when we started
                    //      (but we don't know what that state actually is).
                    final ConnectionState finalConnectionState = finalConnectionState();

                    if (finalConnectionState != ConnectionState.INTERRUPTED_WAITING_TO_RETRY) {
                        LOGGER.error("ReconnectTimerTask is giving up because the connectionState was not in a retry state. " + finalConnectionState);
                        return;
                    }

                    // Since we are in the RETRY ConnectionState, the SignalProvider should exist.
                    // It will only not exist if we are DISCONNECTED or have not connected ever before.
                    final SignalProvider finalSignalProvider = getFinalSignalProvider();

                    assert (finalSignalProvider != null);
                    assert (finalSignalProvider == ReconnectTimerTask.this.signalProvider);

                    // We now need to issue a reconnect.
                    __unsafe_connect();
                }
            });
        }
    }

    private ObservableFuture<Void> finalInnerConnectFuture() {
        accessConnectFuture();

        return __unsafe_innerConnectFuture;
    }

    private ObservableFuture<SubscribeResult> __unsafe_issueSubscribeRequest() {
        // We have a sessionId with socketIO, but we do not yet have a clientId.
        final String clientId = finalSetting(SettingsStore.Keys.CLIENT_ID);
        final String sessionKey = finalSetting(SettingsStore.Keys.SESSION_KEY);

        setConnectionState(ConnectionState.SUBSCRIBING);

        return signalProvider.subscribe(sessionKey, sessionKey, scope);
    }

//    private final Observer<ObservableFuture<Void>> subscribeRequestObserver = new Observer<ObservableFuture<Void>>() {
//        @Override
//        public void notify(Object sender, ObservableFuture<Void> future) {
//            synchronized (SimpleZipwhipClient.this) {
//                if (subscribeFuture != future) {
//                    LOGGER.error("The subscribe future changed underneath us, so we're not going to process the result.");
//                    return;
//                }
//
//                if (future.isSuccess()) {
//                    setState(ConnectionState.SUBSCRIBED);
//                } else {
//                    setState(ConnectionState.INTERRUPTED_WAITING_TO_RETRY);
//
//                    __unsafe_disconnectNowAndReconnectLater();
//                }
//            }
//
//        }
//    };

    private void __unsafe_disconnectNowAndReconnectLater() {

        final SignalProvider finalSignalProvider = getFinalSignalProvider();

        if (finalSignalProvider == null) {
            // There is no current signalProvider... That's weird... Go ahead and just try to reconnectLater.
//            __unsafe_reconnectLater();
        } else {
            // The current SignalProvider might be connected
            if (finalSignalProvider.isConnected())
                finalSignalProvider
                        .disconnect()
                        .addObserver(disconnectObserver);
        }

        try {
            if (signalProvider != null) {
                signalProvider.disconnect();
            }
        } finally {
//            __unsafe_reconnectLater();
        }
    }

    private Observer<ObservableFuture<Void>> disconnectObserver = new Observer<ObservableFuture<Void>>() {
        @Override
        public void notify(Object sender, ObservableFuture<Void> item) {
            // We may or may not be in the expected state. We need to check.
            // 1. Are we currently disconnected?
            // 2. Are we in a state that expects to reconnect?

//            __unsafe_reconnectLater();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("The signalProvider announced disconnected.");
            }
        }
    };

    private long getNextRetryInterval() {
        // handle some sort of retry.
        // we're not connected..
        final int attemptCount = incrementAttemptCount();
        final long nextRetryInterval = retryStrategy.getNextRetryInterval(attemptCount);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("Next retry. (attemptCount:%s) (nextRetryInterval:%s)", attemptCount, nextRetryInterval));
        }

        return nextRetryInterval;
    }

    private void __unsafe_reconnectLater() {
        long retryMillis = getNextRetryInterval();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ReconnectLater: Reconnecting at " + FutureDateUtil.inFuture(retryMillis, TimeUnit.MILLISECONDS));
        }

        timer.newTimeout(new ReconnectTimerTask(signalProvider), retryMillis, TimeUnit.MILLISECONDS);
    }

    private void clearInnerConnectFuture() {
        accessConnectFuture();

        __unsafe_innerConnectFuture = null;
    }

    private void accessConnectFuture() {
        assertHoldsLock(this);
    }

    private void assertHoldsLock(Object object) {
        if (!Thread.holdsLock(object)) {
            throw new IllegalStateException("Does not hold lock: " + object);
        }
    }

    private void setConnectionState(ConnectionState state) {
        final ConnectionState finalConnectionState = finalConnectionState();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("ConnectionState changed (%s -> %s)", finalConnectionState, state));
        }

        this.__unsafe_connectionState = state;

        // This will fire while we have our lock. Maybe not the best choice...
        connectionChangedObservableHelper.notifyObservers(this, state);
    }

    private void accessConnectionState() {
        assertHoldsLock(this);
    }

    private void attachToSignalProvider(SignalProvider signalProvider) {
        signalProvider.getConnectionChangedEvent().addObserver(connectionChangedEvent);
//        signalProvider.getPresenceChangedEvent().addObserver(presenceChangedEvent);
//        signalProvider.getSignalReceivedEvent().addObserver(signalReceivedEvent);
    }

    private final Observer<Void> connectionChangedEvent = new Observer<Void>() {
        @Override
        public void notify(final Object sender, Void item) {
            // We are already in the boss thread.
            synchronized (SimpleZipwhipClient.this) {
                // No one is able to change the state except for us, we hold the golden lock.
                final SignalProviderImpl finalSignalProvider = (SignalProviderImpl) getFinalSignalProvider();
                final SignalConnection finalSignalConnection = finalSignalProvider == null ? null : finalSignalProvider.getSignalConnection();

                // The sender could be either the connection or the provider.
                if (finalSignalConnection != sender && finalSignalProvider != sender) {
                    LOGGER.error("The underlying connection/provider changed while enqueued in the boss thread. Ignoring request.");
                    return;
                }

                if (finalSignalProvider == null || finalSignalConnection == null) {
                    // This is not a scenario we would detect.
                    return;
                }

                // If we are not currently connecting, then we need to watch it.
                final ConnectionState finalConnectionState = finalConnectionState();

                if (finalConnectionState == ConnectionState.SUBSCRIBED_AND_WORKING) {
                    setConnectionState(ConnectionState.INTERRUPTED_WAITING_TO_RETRY);

                    // TODO: issue retry?
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Announcing ConnectionState to listeners: " + finalConnectionState());
                }

                connectionChangedObservableHelper.notifyObservers(SimpleZipwhipClient.this, finalConnectionState());
            }
        }

//                private ConnectionState __unsafe_detectConnectionState() {
//                    final SignalProvider finalSignalProvider = getFinalSignalProvider();
//                    final SignalConnection finalSignalConnection = (finalSignalProvider == null) ? (null) : ((SignalProviderImpl)finalSignalProvider).getSignalConnection();
//                    boolean connected = finalSignalProvider != null && finalSignalProvider.isConnected();
//                    String clientId = finalSignalConnection == null ? null : finalSignalProvider.getClientId();
//                    String subscribedClientId = finalSetting(SettingsStore.Keys.LAST_SUBSCRIBED_CLIENT_ID);
//
////                        CONNECTING,
////                        CONNECTED,
////                        SUBSCRIBE_FAILED_WAITING_TO_RETRY,
////                        SUBSCRIBING,
////                        SUBSCRIBED_AND_WORKING,
////                        INTERRUPTED_WAITING_TO_RETRY,
////                        DISCONNECTING, (not detectable)
////                        DISCONNECTED (signalProvider will be null)
//
//                    if (finalInnerConnectFuture() != null) {
//                        // We are not yet connected with the initial connect.
//                        // This future gets cleared on subscribe.
//                    }
//
//                    if (!connected) {
//                        // We might be interrupted, etc.
//
//                        // DISCONNECTED means that we have no sessionKey and are not trying
//                        if (finalSignalProvider == null) {
//                            return ConnectionState.DISCONNECTED;
//                        }
//
//                        return ConnectionState.INTERRUPTED_WAITING_TO_RETRY;
//                    }
//
//                    if (StringUtil.isNullOrEmpty(clientId)) {
//                        // We are not yet subscribed to a clientId.
//                        return ConnectionState.SUBSCRIBING;
//                    }
//
//                    if (StringUtil.isNullOrEmpty(subscribedClientId)) {
//                        return ConnectionState.SUBSCRIBE_FAILED_WAITING_TO_RETRY;
//                    }
//
//                    // We are connected. Are we working?
//                    if (StringUtil.equalsIgnoreCase(subscribedClientId, clientId)) {
//                        return ConnectionState.SUBSCRIBED_AND_WORKING;
//                    }
//
//                    return null;
//                }
    };

    private void detachFromSignalProvider(SignalProvider signalProvider) {
        signalProvider.getConnectionChangedEvent().removeObserver(connectionChangedEvent);
//        signalProvider.getPresenceChangedEvent().removeObserver(presenceChangedEvent);
//        signalProvider.getSignalReceivedEvent().removeObserver(signalReceivedEvent);
    }

    private int incrementAttemptCount() {
        assertHoldsLock(this);
        __unsafe_attemptCount++;
        return __unsafe_attemptCount;
    }

    private boolean __unsafe_isReturningUser(String sessionKey) {
        return StringUtil.equalsIgnoreCase(sessionKey, getFinalSessionKey());
    }

    private synchronized boolean isLoggedIn() {
        return StringUtil.exists(getFinalSessionKey());
    }

    private String getSavedSessionKey() {
        return settingsStore.get(SettingsStore.Keys.SESSION_KEY);
    }

    private ConnectionState finalConnectionState() {
        return __finalObject(__unsafe_connectionState);
    }

    private MutableObservableFuture<Void> finalExternalConnectFuture() {
        return __finalObject(__unsafe_externalConnectFuture);
    }

    private <T> T __finalObject(T object) {
        assertHoldsLock(this);
        return object;
    }

    public void onSignalReceived(Observer<Message> observer) {
        signalReceivedObservableHelper.addObserver(observer);
    }

    public void onSignalsConnectionChanged() {

    }

    public void onSignalsDisconnected() {

    }

    public void onSignalsPresenceChanged() {

    }

    public SettingsStore getSettingsStore() {
        return settingsStore;
    }

    public void setSettingsStore(SettingsStore settingsStore) {
        this.settingsStore = settingsStore;
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(UserAgent userAgent) {
        this.userAgent = userAgent;
    }

    public CommonExecutorFactory getExecutorFactory() {
        return executorFactory;
    }

    public void setExecutorFactory(CommonExecutorFactory executorFactory) {
        this.executorFactory = executorFactory;
    }

    public ImportantTaskExecutor getImportantTaskExecutor() {
        return importantTaskExecutor;
    }

    public void setImportantTaskExecutor(ImportantTaskExecutor importantTaskExecutor) {
        this.importantTaskExecutor = importantTaskExecutor;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public RetryStrategy getRetryStrategy() {
        return retryStrategy;
    }

    public void setRetryStrategy(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

    public SignalsSubscribeActor getSignalsSubscribeActor() {
        return signalsSubscribeActor;
    }

    public void setSignalsSubscribeActor(SignalsSubscribeActor signalsSubscribeActor) {
        this.signalsSubscribeActor = signalsSubscribeActor;
    }

    /**
     * The subscription scope.
     *
     * @return
     */
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    protected void onDestroy() {

    }

    public UserAgent getFinalUserAgent() {
        accessUserAgent();

        return userAgent;
    }

    private void accessUserAgent() {
        assertHoldsLock(this);
    }

    public static enum ConnectionState {
        CONNECTING(false, false, false),
        CONNECTED(true, true, false),
        SUBSCRIBE_FAILED_WAITING_TO_RETRY(true, true, false),
        SUBSCRIBING(true, false, false),
        SUBSCRIBED_AND_WORKING(true, true, true),
        INTERRUPTED_WAITING_TO_RETRY(false, false, false),
        DISCONNECTING(false, false, false),
        DISCONNECTED(false, false, false);

        private boolean connected;
        private boolean subscribed;
        private boolean bound;

        ConnectionState(boolean connected, boolean bound, boolean subscribed) {
            this.connected = connected;
            this.bound = bound;
            this.subscribed = subscribed;
        }

        public boolean isBound() {
            return bound;
        }

        public boolean isConnected() {
            return connected;
        }

        public boolean isSubscribed() {
            return subscribed;
        }
    }

}
