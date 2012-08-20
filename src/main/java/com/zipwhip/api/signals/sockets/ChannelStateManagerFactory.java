package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.sockets.netty.ChannelState;
import com.zipwhip.util.Directory;
import com.zipwhip.util.Factory;
import com.zipwhip.util.SetDirectory;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/15/12
 * Time: 11:13 AM
 *
 * For creating state managers for channels.
 */
public class ChannelStateManagerFactory implements Factory<StateManager<ChannelState>> {

    private static final ChannelStateManagerFactory INSTANCE = new ChannelStateManagerFactory();

    private Directory<ChannelState, ChannelState> directory;

    public ChannelStateManagerFactory() {
        directory = new SetDirectory<ChannelState, ChannelState>();

        // for connecting
        directory.add(ChannelState.NONE, ChannelState.CONNECTING);
        directory.add(ChannelState.CONNECTING, ChannelState.CONNECTED);
        directory.add(ChannelState.CONNECTING, ChannelState.DISCONNECTED);

        // for manual disconnecting
        directory.add(ChannelState.CONNECTED, ChannelState.DISCONNECTING);
        directory.add(ChannelState.DISCONNECTING, ChannelState.DISCONNECTED);

        // for abrupt changes (connection failed?)
        directory.add(ChannelState.CONNECTED, ChannelState.DISCONNECTED);
    }

    @Override
    public StateManager<ChannelState> create() throws Exception {
        StateManager<ChannelState> result = new StateManager<ChannelState>(directory);

        result.set(ChannelState.NONE);

        return result;
    }

    public static ChannelStateManagerFactory getInstance() {
        return INSTANCE;
    }

    public static StateManager<ChannelState> newStateManager() {
        try {
            return getInstance().create();
        } catch (Exception e) {
            // not possible.
            throw new RuntimeException(e);
        }
    }

}
