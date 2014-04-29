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
public class SocketIoSignalConnection implements SignalConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketIoSignalConnection.class);

    private volatile SocketIO socketIO;
    private volatile ObservableFuture<Void> externalConnectFuture;
    private volatile MutableObservableFuture<Void> connectFuture;
    private volatile int retryCount = 0;

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
    private boolean reconnectScheduled = false;

    public SocketIoSignalConnection() {
        exceptionEvent = new ObservableHelper<Throwable>("ExceptionEvent", eventExecutor);
        connectEvent = new ObservableHelper<Void>("ConnectEvent", eventExecutor);
        disconnectEvent = new ObservableHelper<Void>("DisconnectEvent", eventExecutor);
        messageEvent = new ObservableHelper<JsonElement>("JsonMessageEvent", eventExecutor);
        serverEvent = new ObservableHelper<SignalServerEvent>("ServerEvent", eventExecutor);
    }

    @Override
    public synchronized ObservableFuture<Void> connect() {
        if (externalConnectFuture != null) {
            LOGGER.debug("Tried to connect but already had connect future. Returning that instead.");
            return externalConnectFuture;
        }

        final ObservableFuture<Void> result = externalConnectFuture = importantTaskExecutor.enqueue(executor, new ConnectTask(), FutureDateUtil.in30Seconds());

        result.addObserver(new Observer<ObservableFuture<Void>>() {
            @Override
            public void notify(Object sender, ObservableFuture<Void> item) {
                synchronized (SocketIoSignalConnection.this) {
                    if (item.isSuccess()) {
                        retryCount = 0;
                    }

                    connectFuture = null;
                    externalConnectFuture = null;
                    reconnectScheduled = false;
                }
            }
        });

        return result;
    }

    @Override
    public synchronized ObservableFuture<Void> disconnect() {
        if (socketIO == null) {
            return new FakeObservableFuture<Void>(this, null);
        }

        socketIO.disconnect();
        socketIO = null;

        return new FakeObservableFuture<Void>(this, null);
    }

    public void setRetryStrategy(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    private class ConnectTask implements Callable<ObservableFuture<Void>> {
        @Override
        public ObservableFuture<Void> call() throws Exception {
            synchronized (SocketIoSignalConnection.this) {
                connectFuture = new DefaultObservableFuture<Void>(this, eventExecutor);

                try {
                    socketIO = new SocketIO();
                    socketIO.setGson(gson);
                    socketIO.connect(url, callback);
                } catch (MalformedURLException e) {
                    connectFuture.setFailure(e);
                }

                return connectFuture;
            }
        }
    }

    private TimerTask reconnectTimerTask = new TimerTask() {
        @Override
        public void run(Timeout timeout) throws Exception {
            if (socketIO == null) {
                LOGGER.error("SocketIO was null, not attempting to reconnect.");
                reconnectScheduled = false;
                return;
            }

            if (socketIO.isConnected()) {
                LOGGER.debug("Was already connected, not attempting to reconnect.");
                reconnectScheduled = false;

                return;
            }

            if (connectFuture != null) {
                LOGGER.debug("Reconnect called, but there's an existing connectFuture. Aborting reconnect.");
                return;
            }

            LOGGER.debug("Reconnect called. Disconnecting...");

            disconnect().addObserver(new Observer<ObservableFuture<Void>>() {
                @Override
                public void notify(Object sender, ObservableFuture<Void> item) {
                    LOGGER.debug("... now connecting.");

                    connect().addObserver(new Observer<ObservableFuture<Void>>() {
                        @Override
                        public void notify(Object sender, ObservableFuture<Void> item) {
                            reconnectScheduled = false;

                            if (item.isSuccess()) {
                                LOGGER.debug("Successfully reconnected!");
                            } else {
                                LOGGER.error("Couldn't reconnect: " + item.getCause());
                                reconnect();
                            }
                        }
                    });
                }
            });
        }
    };

    private final IOCallback callback = new IOCallback() {
        @Override
        public void onDisconnect() {
            synchronized (SocketIoSignalConnection.this) {
                if (connectFuture != null) {
                    connectFuture.setFailure(new Exception("Disconnected"));
                }
            }

            disconnectEvent.notifyObservers(SocketIoSignalConnection.this, null);
        }

        @Override
        public void onConnect() {
            synchronized (SocketIoSignalConnection.this) {
                if (connectFuture != null) {
                    connectFuture.setSuccess(null);
                }
            }

            connectEvent.notifyObservers(SocketIoSignalConnection.this, null);
        }

        @Override
        public void onMessage(String data, IOAcknowledge ack) {
            onMessage(new JsonPrimitive(data), ack);
        }

        @Override
        public void onSessionId(String sessionId) {
        }

        @Override
        public void onMessage(JsonElement json, IOAcknowledge ack) {
            try {
                messageEvent.notifyObservers(SocketIoSignalConnection.this, json);
            } finally {
                if (ack != null) {
                    ack.ack();
                }
            }
        }

        @Override
        public void on(String event, IOAcknowledge ack, Object... args) {
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
        public void onState(int state) {
            LOGGER.debug("onState: " + state);

            if (state != IOConnection.STATE_INTERRUPTED && state != IOConnection.STATE_INVALID) {
                return;
            }

            LOGGER.warn("onState: STATE_INTERRUPTED or STATE_INVALID. Scheduling reconnect for later.");
            // Warning: commented out because it created circular synchronization... i.e. deadlock.
//            socketIO.disconnect();
//            socketIO = null;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Synchronizing on (this)");
            }

            synchronized (this) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Done synchronizing on (this)");
                }

                if (connectFuture == null) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("The connectFuture was null. We will now try to reconnect.");
                    }

                    reconnect();
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("The connectFuture was not null. We are already trying to connect, so this disconnect will be ignored (first connect is not retried)");
                    }

                    if (connectFuture != null) {
                        connectFuture.setFailure(new Exception("State changed to " + state));
                    }
                }
            }
        }
    };

    public synchronized void reconnect() {
        if (reconnectScheduled) {
            LOGGER.warn("Already scheduled reconnect, not scheduling another.");
            return;
        }

        long retryInSeconds = retryStrategy.getNextRetryInterval(retryCount);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Scheduling reconnect in %s seconds at %s.", retryInSeconds, FutureDateUtil.inFuture(retryInSeconds, TimeUnit.SECONDS)));
        }

        timer.newTimeout(reconnectTimerTask, retryInSeconds, TimeUnit.SECONDS);
        reconnectScheduled = true;
        retryCount++;
    }

    @Override
    public boolean isConnected() {
        if (socketIO == null) {
            return false;
        }

        return socketIO.isConnected();
    }

    @Override
    public ObservableFuture<ObservableFuture<Object[]>> emit(final String event, final Object... objects) {
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

            final MutableObservableFuture<Object[]> result = new DefaultObservableFuture<Object[]>(this, eventExecutor);
            socketIO.emit(event, new IOAcknowledge() {
                @Override
                public void ack(Object... args) {
                    result.setSuccess(args);
                }
            }, args);
            return result;
        }
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
    public Observable<JsonElement> getMessageEvent() { return messageEvent; }

    @Override
    public ObservableHelper<SignalServerEvent> getServerEvent() { return serverEvent; }
}
