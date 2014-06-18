package com.zipwhip.api.signals;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.zipwhip.api.signals.dto.json.SignalProviderGsonBuilder;
import com.zipwhip.concurrent.*;
import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.gson.GsonUtil;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.reliable.retry.RetryStrategy;
import com.zipwhip.signals2.SignalServerEvent;
import com.zipwhip.timers.Timeout;
import com.zipwhip.timers.Timer;
import com.zipwhip.timers.TimerTask;
import com.zipwhip.util.FutureDateUtil;
import com.zipwhip.util.StringUtil;
import io.socket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Date: 9/5/13
 * Time: 3:28 PM
 *
 * @author Michael
 * @version 1
 */
public class SocketIoSignalConnection extends CascadingDestroyableBase implements SignalConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketIoSignalConnection.class);

    private volatile SocketIO __unsafe_socketIO;
    private volatile ObservableFuture<Void> __unsafe_externalConnectFuture;
    private volatile MutableObservableFuture<Void> __unsafe_connectFuture;
    private volatile int __unsafe_retryCount = 0;

    private final ObservableHelper<JsonElement> messageEvent;
    private final ObservableHelper<SignalServerEvent> serverEvent;
    private final ObservableHelper<Void> disconnectEvent;
    private final ObservableHelper<Void> connectEvent;
    private final ObservableHelper<Throwable> exceptionEvent;

    private Executor eventExecutor = SimpleExecutor.getInstance();
    private Executor executor = SimpleExecutor.getInstance();

    private Gson gson = SignalProviderGsonBuilder.getInstance();
    private ImportantTaskExecutor importantTaskExecutor;
    private RetryStrategy retryStrategy;
    private Timer timer;

    private String url;
    private volatile boolean __unsafe_reconnectScheduled = false;

    public SocketIoSignalConnection() {
        exceptionEvent = new ObservableHelper<Throwable>("ExceptionEvent", eventExecutor);
        connectEvent = new ObservableHelper<Void>("ConnectEvent", eventExecutor);
        disconnectEvent = new ObservableHelper<Void>("DisconnectEvent", eventExecutor);
        messageEvent = new ObservableHelper<JsonElement>("JsonMessageEvent", eventExecutor);
        serverEvent = new ObservableHelper<SignalServerEvent>("ServerEvent", eventExecutor);
    }

    @Override
    public synchronized ObservableFuture<Void> connect() {
        LOGGER.debug("Entered connect() in SocketIoSignalConnection.");

        final ObservableFuture<Void> finalExternalConnectFuture = finalObject(__unsafe_externalConnectFuture);

        if (finalExternalConnectFuture != null) {
            LOGGER.debug("Tried to connect but already had connect future. Returning that instead.");
            return finalExternalConnectFuture;
        }

        final MutableObservableFuture<Void> finalConnectFuture = setConnectFuture(new DefaultObservableFuture<Void>(this, eventExecutor, "connectFuture"));

        final ObservableFuture<Void> result = __unsafe_externalConnectFuture = importantTaskExecutor.enqueue(executor, new ConnectTask(finalConnectFuture), FutureDateUtil.in30Seconds());

        result.addObserver(new Observer<ObservableFuture<Void>>() {
            @Override
            public void notify(Object sender, ObservableFuture<Void> item) {
                // This is run in "this.eventExecutor" thread pool
                synchronized (SocketIoSignalConnection.this) {
                    if (item.isSuccess()) {
                        setRetryCount(0);
                    }

                    clearConnectFutureIf(finalConnectFuture);
                    setReconnectScheduled(false);
                    // We don't need to do a "clearIf" pattern here, since we're the only one that changes it.
                    __unsafe_externalConnectFuture = null;
                }
            }
        });

        return result;
    }

    @Override
    public synchronized ObservableFuture<Void> disconnect() {
        final SocketIO finalSocketIO = finalObject(__unsafe_socketIO);

        if (finalSocketIO == null) {
            return new FakeObservableFuture<Void>(this, null);
        }

        finalSocketIO.disconnect();
        // This isn't safe. We know what we're doing...
        // Setting it null because we own it.
        __unsafe_socketIO = null;

        return new FakeObservableFuture<Void>(this, null);
    }

    public void setRetryStrategy(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    @Override
    protected void onDestroy() {

    }

    private class ConnectTask implements Callable<ObservableFuture<Void>> {

        private final MutableObservableFuture<Void> connectFuture;

        private ConnectTask(MutableObservableFuture<Void> connectFuture) {
            this.connectFuture = connectFuture;
        }

        @Override
        public ObservableFuture<Void> call() throws Exception {
            // This runs in the "this.executor" thread pool

            synchronized (SocketIoSignalConnection.this) {
                try {
                    final SocketIO finalSocketIO = setSocketIO(new SocketIO());

                    finalSocketIO.setGson(gson);
                    finalSocketIO.connect(url, callback);
                } catch (MalformedURLException e) {
                    // Will throw the event in the "eventExecutor" thread (by default is synchronous)
                    connectFuture.setFailure(e);
                }

                return connectFuture;
            }
        }
    }

    private SocketIO setSocketIO(SocketIO socketIO) {
        assertHoldsLock();

        __unsafe_socketIO = socketIO;

        return socketIO;
    }

    private TimerTask reconnectTimerTask = new TimerTask() {
        @Override
        public void run(Timeout timeout) throws Exception {
            // We are on the timer thread.
            // Let's synchronize around this stuff so we're allowed to access it.
            // I'm going to move over to the core boss thread to handle the work.
            performRunnable(new Runnable() {
                @Override
                public void run() {
                    final SocketIO finalSocketIO = finalObject(__unsafe_socketIO);

                    if (finalSocketIO != null && finalSocketIO.isConnected()) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Was already connected, not attempting to reconnect.");
                        }

                        setRetryCount(0);
                        setReconnectScheduled(false);

                        return;
                    }

                    ObservableFuture<Void> finalConnectFuture = finalConnectFuture();

                    if (finalConnectFuture != null) {
                        LOGGER.debug("Reconnect called, but there's an existing connectFuture. Aborting reconnect.");
                        return;
                    }

                    LOGGER.debug("Reconnect called. Disconnecting...");
                    // This runs in the 'this.executor' threadpool.

                    disconnect().addObserver(new Observer<ObservableFuture<Void>>() {
                        @Override
                        public void notify(Object sender, ObservableFuture<Void> item) {
                            // This runs in the 'this.eventExecutor' threadpool.
                            LOGGER.debug("... now connecting.");

                            connect().addObserver(new Observer<ObservableFuture<Void>>() {
                                @Override
                                public void notify(Object sender, ObservableFuture<Void> item) {
                                    // This runs in the 'this.eventExecutor' threadpool.
                                    synchronized (SocketIoSignalConnection.this) {
                                        setReconnectScheduled(false);

                                        if (item.isSuccess()) {
                                            LOGGER.debug("Successfully reconnected!");
                                            setRetryCount(0);
                                        } else {
                                            LOGGER.error("Couldn't reconnect: " + item.getCause());
                                            reconnect();
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
            });
        }
    };

    private void setRetryCount(int retryCount) {
        assertHoldsLock();

        this.__unsafe_retryCount = retryCount;
    }

    private void assertHoldsLock() {
        if (!Thread.holdsLock(this)) {
            throw new IllegalStateException("Failed to hold lock");
        }
    }

    private MutableObservableFuture<Void> finalConnectFuture() {
        return finalObject(__unsafe_connectFuture);
    }

    private <T> T finalObject(T object) {
        assertHoldsLock();

        return object;
    }

    private void performRunnable(final Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable");
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Synchronizing before running: " + runnable);
                }

                synchronized (SocketIoSignalConnection.this) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Running: " + runnable);
                    }

                    runnable.run();
                }
            }
        });
    }

    private final IOCallback callback = new IOCallback() {
        @Override
        public void onDisconnect() {
            performRunnable(new Runnable() {
                @Override
                public void run() {
                    final MutableObservableFuture<Void> finalConnectFuture = finalConnectFuture();

                    if (finalConnectFuture != null) {
                        finalConnectFuture.setFailure(new Exception("Disconnected"));
                    }

                    // If the "EventExecutor" is a SimpleExecutor, then this might cause a deadlock.
                    // The reason is that the observer might synchronize themselves and that would be a bad order
                    // of operations.
                    disconnectEvent.notifyObservers(SocketIoSignalConnection.this, null);
                }
            });
        }

        @Override
        public void onConnect() {
            performRunnable(new Runnable() {
                @Override
                public void run() {
                    final MutableObservableFuture<Void> finalConnectFuture = finalConnectFuture();

                    if (finalConnectFuture != null) {
                        finalConnectFuture.setSuccess(null);
                    }

                    connectEvent.notifyObservers(SocketIoSignalConnection.this, null);
                }
            });
        }

        @Override
        public void onMessage(String data, IOAcknowledge ack) {
            onMessage(new JsonPrimitive(data), ack);
        }

        @Override
        public void onSessionId(String sessionId) {

        }

        @Override
        public void onMessage(final JsonElement json, final IOAcknowledge ack) {
            performRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        messageEvent.notifyObservers(SocketIoSignalConnection.this, json);
                    } finally {
                        if (ack != null) {
                            ack.ack();
                        }
                    }
                }
            });
        }

        @Override
        public void on(final String event, final IOAcknowledge ack, final Object... args) {
            performRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (StringUtil.equals(event, "error")) {
                            for (Object arg : args) {
                                JsonObject object = (JsonObject) arg;

                                SignalServerEvent signalServerEvent =
                                        new SignalServerEvent(
                                                GsonUtil.getInt(object.get("code")),
                                                GsonUtil.getString(object.get("message")));

                                serverEvent.notifyObservers(SocketIoSignalConnection.this, signalServerEvent);
                            }
                        }
                    } finally {
                        if (ack != null) {
                            ack.ack();
                        }
                    }
                }
            });
        }

        @Override
        public void onError(SocketIOException socketIOException) {
            LOGGER.error("onError on socket! " + socketIOException);

            // The onError happens for a lot of reasons, including emit failure.
            // On a handshake error (initial connect) the socketIO library also calls onState()
            // If we disconnect due to a connection error, it also calls onState()
//            if (connectFuture != null) {
//                connectFuture.setFailure(socketIOException);
//            }

            exceptionEvent.notifyObservers(SocketIoSignalConnection.this, socketIOException);
        }

        @Override
        public void onState(final int state) {
            LOGGER.debug("onState: " + state);

            if (state != IOConnection.STATE_INTERRUPTED && state != IOConnection.STATE_INVALID) {
                return;
            }

            performRunnable(new Runnable() {
                @Override
                public void run() {
                    LOGGER.warn("onState: STATE_INTERRUPTED or STATE_INVALID. Scheduling reconnect for later.");
                    // Warning: commented out because it created a deadlock.
                    //            socketIO.disconnect();
                    //            socketIO = null;

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Synchronizing on (this)");
                    }

                    final MutableObservableFuture<Void> finalConnectFuture = finalConnectFuture();

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Done synchronizing on (this)");
                    }

                    if (finalConnectFuture == null) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("The connectFuture was null. We will now try to reconnect.");
                        }

                        reconnect();

                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("The connectFuture was not null. We are already trying to connect, so this disconnect will be ignored (first connect is not retried)");
                        }

                        finalConnectFuture.setFailure(new Exception("State changed to " + state));
                    }

                }
            });
        }
    };

    public synchronized void reconnect() {
        final boolean finalReconnectAlreadyScheduled = getFinalReconnectScheduled();

        if (finalReconnectAlreadyScheduled) {
            LOGGER.warn("Already scheduled reconnect, not scheduling another.");
            return;
        }

        // Casting to/from int/Integer/int. Oh well.
        final int finalRetryCount = finalObject(__unsafe_retryCount);

        long retryInSeconds = retryStrategy.retryIntervalInSeconds(finalRetryCount);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Scheduling reconnect in %s seconds at %s. (retryCount:%s)", retryInSeconds, FutureDateUtil.inFuture(retryInSeconds, TimeUnit.SECONDS), finalRetryCount));
        }

        timer.newTimeout(reconnectTimerTask, retryInSeconds, TimeUnit.SECONDS);

        setReconnectScheduled(true);
        setRetryCount(finalRetryCount + 1);
    }

    @Override
    public boolean isConnected() {
        // NOTE: Synchronizing on "isConnected()" seems unuseful.
        // Sure, we want to be able to tell them an unchanging truth state, though even if we sync'd on "this"
        // and returned a value, it would have no meaning to them because we released our synch when returning the result.
        // Therefore, the nanosecond after the result is returned, its accuracy is in question.
        // So just don't sync.
        final SocketIO UNSAFE_SOCKET_IO_TRANSIENT = this.__unsafe_socketIO;

        if (UNSAFE_SOCKET_IO_TRANSIENT == null) {
            return false;
        }

        return UNSAFE_SOCKET_IO_TRANSIENT.isConnected();
    }

    @Override
    public ObservableFuture<ObservableFuture<Object[]>> emit(final String event, final Object... objects) {
        final SocketIO socketIO = __unsafe_socketIO;

        if (socketIO == null) {
            return new FakeFailingObservableFuture<ObservableFuture<Object[]>>(this, new Exception("Not connected!"));
        }

        ObservableFuture<Object[]> ackFuture = importantTaskExecutor.enqueue(
                executor,
                new SendWithAckTask(socketIO, event, objects, eventExecutor),
                FutureDateUtil.in30Seconds());

        // the underlying library doesn't tell us when transmission is successful.
        // We have to just fake the "transmit" part of the future.
        return new FakeObservableFuture<ObservableFuture<Object[]>>(this, ackFuture);
    }

    private static class SendWithAckTask implements Callable<ObservableFuture<Object[]>> {

        private final SocketIO socketIO;
        private final String event;
        private final Object[] args;
        private final Executor eventExecutor;

        private SendWithAckTask(SocketIO socketIO, String event, Object[] args, Executor eventExecutor) {
            this.socketIO = socketIO;
            this.event = event;
            this.args = args;
            this.eventExecutor = eventExecutor;
        }

        @Override
        public ObservableFuture<Object[]> call() throws Exception {
            if (socketIO == null) {
                return new FakeFailingObservableFuture<Object[]>(this, new IllegalStateException("socketIO was null!"));
            }

            final MutableObservableFuture<Object[]> result = new DefaultObservableFuture<Object[]>(this, eventExecutor, "SendWithAckFuture");

            socketIO.emit(event, new IOAcknowledge() {
                @Override
                public void ack(Object... args) {
                    result.setSuccess(args);
                }
            }, args);

            return result;
        }
    }

    private boolean getFinalReconnectScheduled() {
        assertHoldsLock();

        return __unsafe_reconnectScheduled;
    }

    private boolean getReconnectScheduled() {
        return __unsafe_reconnectScheduled;
    }

    private void setReconnectScheduled(boolean reconnectScheduled) {
        assertHoldsLock();

        __unsafe_reconnectScheduled = reconnectScheduled;
    }

    private void clearConnectFutureIf(ObservableFuture<Void> comparisonFuture) {
        ObservableFuture<Void> finalConnectFuture = finalConnectFuture();

        if (comparisonFuture != finalConnectFuture) {
            throw new IllegalStateException(String.format("%s != %s", finalConnectFuture, comparisonFuture));
        }

        clearConnectFuture();
    }

    private void clearConnectFuture() {
        assertHoldsLock();

        setConnectFuture(null);
    }

    private MutableObservableFuture<Void> setConnectFuture(MutableObservableFuture<Void> connectFuture) {
        assertHoldsLock();

        this.__unsafe_connectFuture = connectFuture;

        return connectFuture;
    }


    @Override
    public Observable<Throwable> getExceptionEvent() {
        return exceptionEvent;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ImportantTaskExecutor getImportantTaskExecutor() {
        return importantTaskExecutor;
    }

    public void setImportantTaskExecutor(ImportantTaskExecutor importantTaskExecutor) {
        this.importantTaskExecutor = importantTaskExecutor;
    }

    public Gson getGson() {
        return gson;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Executor getEventExecutor() {
        return eventExecutor;
    }

    public void setEventExecutor(Executor eventExecutor) {
        this.eventExecutor = eventExecutor;
    }

    @Override
    public Observable<Void> getConnectEvent() {
        return connectEvent;
    }

    @Override
    public Observable<Void> getDisconnectEvent() {
        return disconnectEvent;
    }

    @Override
    public Observable<JsonElement> getMessageEvent() {
        return messageEvent;
    }

    @Override
    public ObservableHelper<SignalServerEvent> getServerEvent() {
        return serverEvent;
    }
}
