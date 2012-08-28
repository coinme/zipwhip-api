package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.ApiConnectionFactory;
import com.zipwhip.api.ZipwhipClient;
import com.zipwhip.api.ZipwhipClientFactory;
import com.zipwhip.api.settings.MemorySettingStore;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SocketSignalProviderFactory;
import com.zipwhip.api.signals.reconnect.ExponentialBackoffReconnectStrategy;
import com.zipwhip.api.signals.sockets.netty.RawSocketIoChannelPipelineFactory;
import com.zipwhip.events.Observer;
import com.zipwhip.util.DownloadURL;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/28/12
 * Time: 1:51 PM
 */
public class SocketSignalProviderIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(SocketSignalProviderIntegrationTest.class);

//    private String sessionKey = "6c20b056-6843-404d-9fb4-b492d54efe75:142584301"; // evo 3d
//    private String sessionKey = "fc3890ba-a2c7-4449-a4c7-c80f57af228b:142584301"; // evo 3d
    private String host = "http://staging.zipwhip.com";

    ZipwhipClient client;

    @Before
    public void setUp() throws Exception {
        ApiConnectionFactory connectionFactory = ApiConnectionFactory.newInstance()
                .host(host);
//                .sessionKey(sessionKey);

        connectionFactory.setUsername("2062513225");
        connectionFactory.setPassword("pistons456");

        SocketSignalProviderFactory signalProviderFactory = SocketSignalProviderFactory.newInstance()
                .reconnectStrategy(new ExponentialBackoffReconnectStrategy())
                .channelPipelineFactory(new RawSocketIoChannelPipelineFactory(60, 5));

        ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory, signalProviderFactory);

        ZipwhipClient zipwhipClient = zipwhipClientFactory.create();

        zipwhipClient.setSettingsStore(new MemorySettingStore());

        client = zipwhipClient;
    }

    @Test
    public void testConnectAndVerify() throws Exception {
        client.connect().get(10, TimeUnit.SECONDS);

        String sessionKey = client.getConnection().getSessionKey();
//        assertEquals(client.getConnection().getSessionKey(), sessionKey);
        assertTrue(client.getSignalProvider().getClientId() != null);
        assertTrue(client.getSignalProvider().isConnected());

        final String requestId = UUID.randomUUID().toString();
        int index = new Random().nextInt();

        final CountDownLatch latch = new CountDownLatch(1);

        final Signal[] verifySignal = new Signal[1];

        client.getSignalProvider().onSignalReceived(new Observer<List<Signal>>() {
            @Override
            public void notify(Object sender, List<Signal> item) {
                for (Signal signal : item) {
                    if (requestId.equals(signal.getType())) {
                        verifySignal[0] = signal;
                    }
                }
                latch.countDown();
            }
        });

        assertEquals(client.getConnection().getSessionKey(), sessionKey);
        assertTrue(client.getSignalProvider().getClientId() != null);
        assertTrue(client.getSignalProvider().isConnected());

        LOGGER.debug(DownloadURL.get("http://staging.zipwhip.com/mvc/signals/signal?session=" + sessionKey + "&requestId=" + requestId + "&type=" + requestId + "&scope=" + index));

        latch.await(150, TimeUnit.SECONDS);

        assertNotNull(verifySignal[0]);
    }
}
