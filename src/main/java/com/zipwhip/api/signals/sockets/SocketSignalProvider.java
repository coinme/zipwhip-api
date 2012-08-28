package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.*;
import com.zipwhip.api.signals.commands.*;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.concurrent.FutureUtil;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.FakeObservableFuture;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.signals.address.ClientAddress;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/1/11 Time: 4:30 PM
 * <p/>
 * The SocketSignalProvider will connect to the Zipwhip SignalServer via TCP.
 * <p/>
 * This interface is intended to be used by 1 and only 1 ZipwhipClient object.
 * This is a very high level interaction where you connect for 1 user.
 */
public class SocketSignalProvider extends CascadingDestroyableBase implements SignalProvider {

    private static final Logger LOGGER = Logger.getLogger(SocketSignalProvider.class);

    private final ObservableHelper<PingEvent> pingEvent = new ObservableHelper<PingEvent>();
    private final ObservableHelper<Boolean> connectionChangedEvent = new ObservableHelper<Boolean>();
    private final ObservableHelper<String> newClientIdEvent = new ObservableHelper<String>();
    private final ObservableHelper<List<Signal>> signalEvent = new ObservableHelper<List<Signal>>();
    private final ObservableHelper<List<SignalCommand>> signalCommandEvent = new ObservableHelper<List<SignalCommand>>();
    private final ObservableHelper<String> exceptionEvent = new ObservableHelper<String>();
    private final ObservableHelper<Void> signalVerificationEvent = new ObservableHelper<Void>();
    private final ObservableHelper<VersionMapEntry> newVersionEvent = new ObservableHelper<VersionMapEntry>();
    private final ObservableHelper<Boolean> presenceReceivedEvent = new ObservableHelper<Boolean>();
    private final ObservableHelper<SubscriptionCompleteCommand> subscriptionCompleteEvent = new ObservableHelper<SubscriptionCompleteCommand>();
    private final ObservableHelper<Command> commandReceivedEvent = new ObservableHelper<Command>();

    private CountDownLatch connectLatch;
    private final SignalConnection connection;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("SocketSignalProvider-"));
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("SocketSignalProvider-scheduler-"));
    private ExecutorService eventExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("SocketSignalProvider-events-"));

    private final AuthenticationKeyChain authenticationKeyChain = new AuthenticationKeyChain();

    private final StateManager<SignalProviderState> stateManager;

    private String clientId;
    private String originalClientId; //So we can detect change

    private boolean connectionStateSwitch; // The previous connection state
    private boolean connectionNegotiated; // Have we finished negotiating with SignalServer

    private Presence presence;
    private Map<String, Long> versions = new HashMap<String, Long>();
    private final Map<String, SlidingWindow<Command>> slidingWindows = new HashMap<String, SlidingWindow<Command>>();


    public SocketSignalProvider() {
        this(new NettySignalConnection());
    }

    public SocketSignalProvider(SignalConnection conn) {
        if (conn == null) {
            this.connection = new NettySignalConnection();
        } else {
            this.connection = conn;
        }

        this.link(connection);
        this.link(authenticationKeyChain);
        this.link(pingEvent);
        this.link(connectionChangedEvent);
        this.link(newClientIdEvent);
        this.link(signalEvent);
        this.link(exceptionEvent);
        this.link(signalVerificationEvent);
        this.link(newVersionEvent);
        this.link(presenceReceivedEvent);
        this.link(subscriptionCompleteEvent);
        this.link(signalCommandEvent);
        this.link(commandReceivedEvent);

        StateManager<SignalProviderState> m;
        try {
            m = SignalProviderStateManagerFactory.getInstance().create();
        } catch (Exception e) {
            m = null;
        }
        stateManager = m;
        if (m == null){
            throw new RuntimeException("Failed to setup factory");
        }

        this.connection.onMessageReceived(new Observer<Command>() {
            /**
             * The NettySignalConnection will call this method when there's an
             * event from the remote SignalServer.
             *
             * @param sender The sender might not be the same object every time.
             * @param command Rich object representing the command received from the SignalServer.
             */
            @Override
            public void notify(Object sender, Command command) {


                // Check if this command has a valid version number associated with it...
                if (command.getVersion() != null && command.getVersion().getValue() > 0) {

                    String versionKey = command.getVersion().getKey();

                    synchronized (slidingWindows) {
                        if (!slidingWindows.containsKey(versionKey)) {

                            LOGGER.warn("Creating sliding window for key " + versionKey);

                            SlidingWindow<Command> newWindow = new SlidingWindow<Command>(versionKey);
                            newWindow.onHoleTimeout(signalHoleObserver);
                            newWindow.onPacketsReleased(packetReleasedObserver);

                            if (versions != null && versions.get(versionKey) != null) {
                                LOGGER.debug("Initializing sliding window index sequence to " + versions.get(versionKey));
                                newWindow.setIndexSequence(versions.get(versionKey));
                            }

                            slidingWindows.put(versionKey, newWindow);
                        }
                    }

                    // This list will be populated with the sequential packets that should be released
                    List<Command> commandResults = new ArrayList<Command>();

                    LOGGER.debug("Signal version " + command.getVersion().getValue());

                    SlidingWindow.ReceiveResult result = slidingWindows.get(versionKey).receive(command.getVersion().getValue(), command, commandResults);

                    switch (result) {
                        case EXPECTED_SEQUENCE:
                            LOGGER.debug("EXPECTED_SEQUENCE");
                            handleCommands(commandResults);
                            break;
                        case HOLE_FILLED:
                            LOGGER.debug("HOLE_FILLED");
                            handleCommands(commandResults);
                            break;
                        case DUPLICATE_SEQUENCE:
                            LOGGER.warn("DUPLICATE_SEQUENCE");
                            break;
                        case POSITIVE_HOLE:
                            LOGGER.warn("POSITIVE_HOLE");
                            break;
                        case NEGATIVE_HOLE:
                            LOGGER.debug("NEGATIVE_HOLE");
                            handleCommands(commandResults);
                            break;
                        default:
                            LOGGER.warn("UNKNOWN_RESULT");
                    }
                } else {
                    // Non versioned command, not windowed
                    handleCommand(command);
                }
            }
        });

        this.connection.onConnect(new Observer<Boolean>() {
            /*
                * The NettySignalConnection will call this method when a TCP socket connection is attempted.
                */
            @Override
            public void notify(Object sender, Boolean connected) {
                /*
                     * If we have a successful TCP connection then
                     * check if we need to send the connect command.
                     */
                if (connected) {
                    stateManager.transitionOrThrow(SignalProviderState.CONNECTED);
                    writeConnectCommand();
                }
            }
        });

        /*
              Forward connect events up to clients
              This is too early to notify about new connections. We need to wait until the connect command
              comes back.
           */
//        connection.onConnect(new Observer<Boolean>() {
//            @Override
//            public void notify(Object sender, Boolean connected) {
//                // If the state has changed then notify
//                if (connectionStateSwitch ^ connected) {
//                    connectionStateSwitch = connected;
//                    connectionChangedEvent.notifyObservers(sender, connected);
//                }
//            }
//        });

        /*
              Forward disconnect events up to clients
           */
        connection.onDisconnect(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean causedByNetwork) {
                // Ensure that the latch is in a good state for reconnect
                releaseLatch();

                connectionNegotiated = false;

                stateManager.transitionOrThrow(SignalProviderState.DISCONNECTED);

                // If the state has changed then notify
                if (connectionStateSwitch) {
                    connectionStateSwitch = false;
                    connectionChangedEvent.notifyObservers(sender, Boolean.FALSE);
                }
            }
        });

        /*
              Forward ping events up to clients
           */
        connection.onPingEvent(new Observer<PingEvent>() {
            @Override
            public void notify(Object sender, PingEvent item) {
                pingEvent.notifyObservers(sender, item);
            }
        });

        /*
              Forward connection exceptions up to clients
           */
        connection.onExceptionCaught(new Observer<String>() {
            @Override
            public void notify(Object sender, String message) {
                exceptionEvent.notifyObservers(sender, message);
            }
        });

        /*
              Observe our own version changed events so we can stay in sync internally
           */
        onVersionChanged(new Observer<VersionMapEntry>() {
            @Override
            public void notify(Object sender, VersionMapEntry version) {
                versions.put(version.getKey(), version.getValue());
            }
        });

        onNewClientIdReceived(new Observer<String>() {
            @Override
            public void notify(Object sender, String newClientId) {
                clientId = newClientId;
                originalClientId = newClientId;

                if (presence != null)
                    presence.setAddress(new ClientAddress(newClientId));
            }
        });

    }

    private void handleCommands(List<Command> commands) {
        for (Command command : commands) {
            handleCommand(command);
        }
    }

    private void handleCommand(Command command) {

        commandReceivedEvent.notifyObservers(this, command);

        if (command.getVersion() != null && command.getVersion().getValue() > 0) {
            newVersionEvent.notifyObservers(this, command.getVersion());
        }

        if (command instanceof ConnectCommand) {

            handleConnectCommand((ConnectCommand) command);

        } else if (command instanceof DisconnectCommand) {

            handleDisconnectCommand((DisconnectCommand) command);

        } else if (command instanceof SubscriptionCompleteCommand) {

            handleSubscriptionCompleteCommand((SubscriptionCompleteCommand) command);

        } else if (command instanceof SignalCommand) {

            handleSignalCommand((SignalCommand) command);

        } else if (command instanceof PresenceCommand) {

            handlePresenceCommand((PresenceCommand) command);

        } else if (command instanceof SignalVerificationCommand) {

            handleSignalVerificationCommand((SignalVerificationCommand) command);

        } else if (command instanceof NoopCommand) {

            LOGGER.debug("Received NoopCommand");

        } else {

            LOGGER.warn("Unrecognized command: " + command.getClass().getSimpleName());
        }
    }

    /*
	 * This method allows us to decouple connection.connect() from provider.connect() for
	 * cases when we have been notified by the connection that it has a successful connection.
	 */
    private void writeConnectCommand() {
        if ((connectLatch == null) || (connectLatch.getCount() == 0)) {
            connection.send(new ConnectCommand(clientId, versions));
        }
    }

    private void notifyConnected(boolean connected) {
        // If the state has changed then notify
        if (connectionStateSwitch ^ connected) {
            connectionStateSwitch = connected;
            connectionChangedEvent.notifyObservers(this, connected);
        }
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected() && connectionNegotiated;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public Presence getPresence() {
        return presence;
    }

    @Override
    public void setPresence(Presence presence) {
        this.presence = presence;
    }

    @Override
    public Map<String, Long> getVersions() {
        return versions;
    }

    @Override
    public void setVersions(Map<String, Long> versions) {
        this.versions = versions;
    }

    @Override
    public ObservableFuture<Boolean> connect() throws Exception {
        return connect(originalClientId, null, null);
    }

    @Override
    public ObservableFuture<Boolean> connect(String clientId) throws Exception {
        return connect(clientId, null, null);
    }

    @Override
    public ObservableFuture<Boolean> connect(String clientId, Map<String, Long> versions) throws Exception {
        return connect(clientId, versions, presence);
    }

    @Override
    public synchronized ObservableFuture<Boolean> connect(String clientId, Map<String, Long> versions, Presence presence) throws Exception {

        if (isConnected() || ((connectLatch != null) && (connectLatch.getCount() > 0))) {
            LOGGER.debug(String.format("Connect requested but already connected or connecting... negotiating(%b)connectLatch(%d)isConnected(%b)", !connectionNegotiated, connectLatch == null ? -1 : connectLatch.getCount(), connection.isConnected()));
            return new FakeObservableFuture<Boolean>(this, Boolean.TRUE);
        }

        // This will help us do the connect synchronously
        connectLatch = new CountDownLatch(1);

        // keep track of the original one, so we can detect change
        if (StringUtil.exists(clientId)) {
            originalClientId = clientId;
        }

        // Hold onto these objects for internal reconnect attempts
        if (presence != null) {
            this.presence = presence;
        }

        if (CollectionUtil.exists(versions)) {
            this.versions = versions;
        }

        // Connect our TCP socket
        final Future<Boolean> connectFuture;

        try {
            stateManager.transitionOrThrow(SignalProviderState.CONNECTING);
            connectFuture = connection.connect();
        } catch (Exception e) {
            // oh shit, we crashed!
            connectLatch.countDown();
            connectLatch = null;
            LOGGER.warn("Fixed the connectLatch deadlock bug. Killed the latch because got exception connecting.");
            throw e;
        }

        return FutureUtil.execute(executor, this, new Callable<Boolean>() {
            @Override
            public Boolean call() {

                try {
                    // Block until the TCP connection connects or times out
                    connectFuture.get(connection.getConnectTimeoutSeconds(), TimeUnit.SECONDS);

                    if (connection.isConnected()) {
                        connection.send(new ConnectCommand(originalClientId, SocketSignalProvider.this.versions));

                        // block while the signal server is thinking/hanging.
                        boolean countedDown = connectLatch.await(connection.getConnectTimeoutSeconds(), TimeUnit.SECONDS);

                        // If we timed out the latch might still blocking other threads
                        if (!countedDown) {
                            connectLatch.countDown();
                        }
                    } else {
                        // Need to make sure we always count down
                        connectLatch.countDown();
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception in connecting..." + e, e.getCause());

                    // Need to make sure we always count down
                    connectLatch.countDown();

                    // Cancel the execution of connection.connect()
                    connectFuture.cancel(true);

                    stateManager.transitionOrThrow(SignalProviderState.DISCONNECTED);
                }

                return isConnected();
            }
        });
    }

    @Override
    public synchronized ObservableFuture<Void> disconnect() throws Exception {
        if (isConnecting()) {
            // this is an unsafe operation, we're already trying to connect!
            // TODO: probably the best action is to tear everything down
            throw new IllegalStateException("We were connecting and you tried to call disconnect..");
        }

        if (!isConnected()) {
            // we are not currently connected and you called disconnect!
            throw new IllegalStateException("We are not connected and you tried to call disconnect..");
        }

        // i think we're safe to do the disconnect operation.

        for (String key : slidingWindows.keySet()) {
            slidingWindows.get(key).destroy();
        }
        slidingWindows.clear();

        return FutureUtil.execute(eventExecutor, this, connection.disconnect(false));
    }

    @Override
    public ObservableFuture<Void> disconnect(Boolean causedByNetwork) throws Exception {
        if (causedByNetwork) {
            return disconnect();
        } else {
            return FutureUtil.execute(eventExecutor, this, connection.disconnect(false));
        }
    }

    @Override
    public void nudge() {
        connection.keepalive();
    }

    @Override
    public void onSignalReceived(Observer<List<Signal>> observer) {
        signalEvent.addObserver(observer);
    }

    @Override
    public void onSignalCommandReceived(Observer<List<SignalCommand>> observer) {
        signalCommandEvent.addObserver(observer);
    }

    @Override
    public void onConnectionChanged(Observer<Boolean> observer) {
        connectionChangedEvent.addObserver(observer);
    }

    @Override
    public void onNewClientIdReceived(Observer<String> observer) {
        newClientIdEvent.addObserver(observer);
    }

    @Override
    public void onSubscriptionComplete(Observer<SubscriptionCompleteCommand> observer) {
        subscriptionCompleteEvent.addObserver(observer);
    }

    @Override
    public void onPhonePresenceReceived(Observer<Boolean> observer) {
        presenceReceivedEvent.addObserver(observer);
    }

    @Override
    public void onSignalVerificationReceived(Observer<Void> observer) {
        signalVerificationEvent.addObserver(observer);
    }

    @Override
    public void onVersionChanged(Observer<VersionMapEntry> observer) {
        newVersionEvent.addObserver(observer);
    }

    @Override
    public void onPingEvent(Observer<PingEvent> observer) {
        pingEvent.addObserver(observer);
    }

    @Override
    public void onExceptionEvent(Observer<String> observer) {
        exceptionEvent.addObserver(observer);
    }

    @Override
    public void onCommandReceived(Observer<Command> observer) {
        commandReceivedEvent.addObserver(observer);
    }

    @Override
    protected void onDestroy() {
        eventExecutor.shutdownNow();
        scheduler.shutdownNow();
        executor.shutdownNow();
    }

    private void handleConnectCommand(ConnectCommand command) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Handling ConnectCommand " + command.isSuccessful());

        boolean newClientId = false;

        if (command.isSuccessful()) {
            // copy it over for stale checking
            originalClientId = clientId;

            clientId = command.getClientId();

            if (!StringUtil.equals(clientId, originalClientId)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Received a new client id: " + clientId);
                }

                newClientId = true;
            }

            connectionNegotiated = true;
        } else {
            connectionNegotiated = false;
            // TODO: consider firing a disconnected event or forcing a disconnect here?
        }

        releaseLatch();

        if (command.isSuccessful()) {
            if (newClientId) {
                // not the same, lets announce
                // announce on a separate thread
                newClientIdEvent.notifyObservers(this, clientId);
            }

            notifyConnected(true);

            if (versions != null) {
                // Send a BackfillCommand for each version key - in practice
                // this is a single key/version
                for (String key : versions.keySet()) {
                    connection.send(new BackfillCommand(Collections.singletonList(versions.get(key)), key));
                }
            }
        }
    }

    private void releaseLatch() {
        if (connectLatch != null) {
            // we need to countDown the latch, when it hits zero (after this
            // call)
            // the connect ObservableFuture will complete. This gives the caller
            // a way to block on our connection
            connectLatch.countDown();
        }
    }

    private boolean isConnecting() {
        return (connectLatch != null) && (connectLatch.getCount() > 0);
    }

    private void handleDisconnectCommand(DisconnectCommand command) {

        LOGGER.debug("Handling DisconnectCommand");

        try {
            LOGGER.debug("Disconnecting (with network=false). There should not be any auto reconnect activity now.");
            disconnect();
        } catch (Exception e) {
            LOGGER.error("Error disconnecting", e);
        }

        if (command.isBan()) {
            LOGGER.warn("BANNED by SignalServer! Those jerks!");
        }

        // If the command has not said 'ban' or 'stop'
        if (!command.isStop() || !command.isBan()) {

            if (!StringUtil.EMPTY_STRING.equals(command.getHost())) {
                connection.setHost(command.getHost());
            }

            if (command.getPort() > 0) {
                connection.setPort(command.getPort());
            }

            LOGGER.debug(String.format("We are going to connect again %d seconds from now", command.getReconnectDelay()));

            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    try {

                        // Clear the clientId so we will re-up on connect
                        originalClientId = StringUtil.EMPTY_STRING;

                        LOGGER.debug("Executing the connect that was requested by the server. Nulled out the clientId...");
                        connect();

                    } catch (Exception e) {
                        LOGGER.error("Error connecting", e);
                    }
                }
            }, command.getReconnectDelay(), TimeUnit.SECONDS);
        }
    }

    private void handlePresenceCommand(PresenceCommand command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Handling PresenceCommand " + command.getPresence());
        }

        boolean selfPresenceExists = false;
        List<Presence> presenceList = command.getPresence();

        if (presenceList == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Nothing is known about us or our peers");
            }

            selfPresenceExists = false;

        } else {

            for (Presence presence : command.getPresence()) {
                if (clientId.equals(presence.getAddress().getClientId())) {
                    selfPresenceExists = true;
                }

                if (presence.getCategory().equals(PresenceCategory.Phone)) {
                    presenceReceivedEvent.notifyObservers(this, presence.getConnected());
                }
            }
        }
        if (!selfPresenceExists) {

            if (presence != null) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Reidentifying our presence object");
                }

                connection.send(new PresenceCommand(Collections.singletonList(presence)));

            } else {
                LOGGER.debug("Our presence object was empty, so we didn't share it");
            }
        }
    }

    private void handleSignalCommand(SignalCommand command) {
        LOGGER.debug("Handling SignalCommand");

        // Distribute the command and the raw signal to give client's flexibility regarding what data they need
        signalCommandEvent.notifyObservers(this, Collections.singletonList(command));
        signalEvent.notifyObservers(this, Collections.singletonList(command.getSignal()));
    }

    private void handleSubscriptionCompleteCommand(SubscriptionCompleteCommand command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Handling SubscriptionCompleteCommand " + command.toString());
        }

        if (!sendPresence(presence)) {
            LOGGER.warn("Tried and failed to send presence");
        }

        stateManager.transitionOrThrow(SignalProviderState.AUTHENTICATED);

//        Asserts.assertTrue(authenticationKeyChain.isAuthenticated(clientId, command.getSubscriptionId()), "This subscriptionId was already authenticated!");
//        // WARNING: We don't know which clientId this really came in for..
//        authenticationKeyChain.add(clientId, command.getSubscriptionId());

        subscriptionCompleteEvent.notifyObservers(this, command);
    }

    private boolean sendPresence(Presence presence) {
        if (presence != null) {
            if (presence.getAddress() == null) {
                presence.setAddress(new ClientAddress());
            }

            // Set our clientId in case its not already there
            presence.getAddress().setClientId(clientId);

            // TODO handle send future
            connection.send(new PresenceCommand(Collections.singletonList(presence)));
            return true;

        } else {
            return false;
        }
    }

    private void handleSignalVerificationCommand(SignalVerificationCommand command) {
        LOGGER.debug("Processing SignalVerificationCommand " + command.toString());
        signalVerificationEvent.notifyObservers(this, null);
    }

    private Observer<SlidingWindow.HoleRange> signalHoleObserver = new Observer<SlidingWindow.HoleRange>() {
        @Override
        public void notify(Object sender, SlidingWindow.HoleRange hole) {
            LOGGER.debug("Signal hole detected, requesting backfill for  " + hole.toString());
            connection.send(new BackfillCommand(hole.getRange(), hole.key));
        }
    };

    private Observer<List<Command>> packetReleasedObserver = new Observer<List<Command>>() {
        @Override
        public void notify(Object sender, List<Command> commands) {
            LOGGER.warn(commands.size() + " packets released due to timeout, leaving a hole.");
            handleCommands(commands);
        }
    };

}
