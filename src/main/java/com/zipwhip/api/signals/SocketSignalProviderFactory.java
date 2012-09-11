package com.zipwhip.api.signals;

import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.SocketSignalProvider;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.util.Factory;
import org.jboss.netty.channel.ChannelPipelineFactory;

import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/12/11 Time: 6:54 PM
 * <p/>
 * Create signalProviders that connect via Sockets
 */
public class SocketSignalProviderFactory implements Factory<SignalProvider> {

    private ReconnectStrategy reconnectStrategy = null;
    private ChannelPipelineFactory channelPipelineFactory = null;
    private Factory<ExecutorService> executorFactory = null;
    private SocketAddress address;

    public SocketSignalProviderFactory() {

    }

    public static SocketSignalProviderFactory newInstance() {
        return new SocketSignalProviderFactory();
    }

    @Override
    public SignalProvider create() {
//        NettySignalConnection connection = new NettySignalConnection(reconnectStrategy, channelPipelineFactory);
        NettySignalConnection connection = new NettySignalConnection(executorFactory, reconnectStrategy, channelPipelineFactory);
        connection.setConnectTimeoutSeconds(10);
        if (address != null) {
            connection.setAddress(address);
        }

        return new SocketSignalProvider(connection);
    }

    public SocketSignalProviderFactory reconnectStrategy(ReconnectStrategy reconnectStrategy) {
        this.reconnectStrategy = reconnectStrategy;
        return this;
    }

    public SocketSignalProviderFactory channelPipelineFactory(ChannelPipelineFactory channelPipelineFactory) {
        this.channelPipelineFactory = channelPipelineFactory;
        return this;
    }

    public SocketSignalProviderFactory executorFactory(Factory<ExecutorService> executorFactory) {
        this.executorFactory = executorFactory;
        return this;
    }

    public SocketSignalProviderFactory address(SocketAddress address) {
        this.address = address;
        return this;
    }
}
