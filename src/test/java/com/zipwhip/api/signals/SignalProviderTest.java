package com.zipwhip.api.signals;

import com.google.gson.Gson;
import com.ning.http.client.AsyncHttpClient;
import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.api.signals.dto.SubscribeResult;
import com.zipwhip.api.signals.dto.json.SignalProviderGsonBuilder;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.signals2.address.ClientAddress;
import com.zipwhip.signals2.presence.Presence;
import com.zipwhip.signals2.presence.UserAgent;
import com.zipwhip.signals2.presence.UserAgentCategory;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.StringUtil;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Date: 8/7/13
 * Time: 3:11 PM
 *
 * @author Michael
 * @version 1
 */
public class SignalProviderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignalProviderTest.class);

    SignalProviderImpl signalProvider;
    NingSignalsSubscribeActor actor = new NingSignalsSubscribeActor();
    ImportantTaskExecutor importantTaskExecutor = new ImportantTaskExecutor();
    Gson gson = SignalProviderGsonBuilder.getInstance();

    @Before
    public void setUp() throws Exception {
        SocketIoSignalConnection signalConnection = new SocketIoSignalConnection();
        signalConnection.setImportantTaskExecutor(importantTaskExecutor);
        signalConnection.setGson(gson);
        signalConnection.setUrl("http://localhost:8000/");

        signalProvider = new SignalProviderImpl();
        signalProvider.setSignalsSubscribeActor(actor);
        signalProvider.setBufferedOrderedQueue(new SilenceOnTheLineBufferedOrderedQueue<DeliveredMessage>());
        signalProvider.setGson(gson);
        signalProvider.setImportantTaskExecutor(importantTaskExecutor);
        signalProvider.setSignalConnection(signalConnection);

        actor.setUrl("http://localhost:8080/signal/subscribe");
        actor.setClient(new AsyncHttpClient());
    }

    @Test
    public void testDisconnectWhileAlreadyDisconnected() {
        assertFalse(signalProvider.isDestroyed());
        assertFalse(signalProvider.isConnected());
        assertTrue(signalProvider.disconnect().isDone());
        assertTrue(signalProvider.disconnect().isSuccess());
    }

    @Test
    public void testConnect() throws Exception {
        final String sessionKey = "7bd08322-b2d7-4ca3-880c-5a400cb3f429:677606";
        final String subscriptionId = "subscriptionId";
        final String clientId;

        assertTrue(StringUtil.isNullOrEmpty(signalProvider.getClientId()));

        final UserAgent userAgent = new UserAgent();

        userAgent.setCategory(UserAgentCategory.Phone);
        userAgent.setMakeModel("example-makeModel");
        userAgent.setVersion("2.3.3b");
        userAgent.setBuild("34-build");

        ObservableFuture<Void> future = signalProvider.connect(userAgent);

        await(future);

        assertTrue(future.isSuccess());

        assertTrue(StringUtil.exists(signalProvider.getClientId()));
        clientId = signalProvider.getClientId();

        assertNotNull(signalProvider.getUserAgent());
        final CountDownLatch subscribeCountDownLatch = new CountDownLatch(1);
        final CountDownLatch presenceCountDownLatch = new CountDownLatch(1);

        signalProvider.getPresenceChangedEvent().addObserver(new Observer<Event<Presence>>() {
            @Override
            public void notify(Object sender, Event<Presence> item) {
                assertNotNull(item);
                assertTrue(CollectionUtil.exists(item.getSubscriptionIds()));
                assertEquals(1, item.getSubscriptionIds().size());
                assertEquals(subscriptionId, item.getSubscriptionIds().iterator().next());

                assertEquals(new ClientAddress(clientId), item.getContent().getAddress());
                assertEquals(Boolean.TRUE, item.getContent().getConnected());
                assertEquals(userAgent, item.getContent().getUserAgent());

                presenceCountDownLatch.countDown();
            }
        });

        signalProvider.getSubscribeEvent().addObserver(new Observer<SubscribeResult>() {
            @Override
            public void notify(Object sender, SubscribeResult signalSubscribeResult) {
                assertNotNull(signalSubscribeResult);
                assertFalse(signalSubscribeResult.isFailed());
                assertEquals(signalSubscribeResult.getSessionKey(), sessionKey);
                assertEquals(signalSubscribeResult.getSubscriptionId(), subscriptionId);
                assertEquals(3, signalSubscribeResult.getChannels().size());

                subscribeCountDownLatch.countDown();
            }
        });

        ObservableFuture<SubscribeResult> future1 = signalProvider.subscribe(sessionKey, subscriptionId);

        signalProvider.getSignalReceivedEvent().addObserver(new Observer<DeliveredMessage>() {
            @Override
            public void notify(Object sender, DeliveredMessage item) {
                LOGGER.debug(String.format("Received a signal for subscriptionId(%s) : %s", item.getSubscriptionIds(), item.getContent()));
            }
        });

        SubscribeResult signalSubscribeResult = await(future1);

        await(subscribeCountDownLatch);
        await(presenceCountDownLatch);

        assertNotNull(signalSubscribeResult);
        assertFalse(signalSubscribeResult.isFailed());
        assertEquals(signalSubscribeResult.getSessionKey(), sessionKey);
        assertEquals(signalSubscribeResult.getSubscriptionId(), subscriptionId);
        assertEquals(3, signalSubscribeResult.getChannels().size());

        LOGGER.debug("Everything looks good, starting the wait cycle to allow messages to be received!");

        // we need to wait to consume messages
        Thread.sleep(500000);
    }

    private void await(CountDownLatch countDownLatch) throws InterruptedException {
        assertNotNull(countDownLatch);
        assertTrue(countDownLatch.await(50, TimeUnit.SECONDS));
    }

    @Test
    public void testSubscriptionCompleteNeverComesBack() throws Exception {
        signalProvider.setSignalsSubscribeActor(new MockSignalSubscribeActor());

        final String sessionKey = "sessionKey";
        final String subscriptionId = "subscriptionId";

        assertTrue(StringUtil.isNullOrEmpty(signalProvider.getClientId()));

        ObservableFuture<Void> future = signalProvider.connect(new UserAgent());

        await(future);

        assertTrue(future.isSuccess());

        assertTrue(StringUtil.exists(signalProvider.getClientId()));

        assertNotNull(signalProvider.getUserAgent());

        signalProvider.getSubscribeEvent().addObserver(new Observer<SubscribeResult>() {
            @Override
            public void notify(Object sender, SubscribeResult signalSubscribeResult) {
                assertNotNull(signalSubscribeResult);
                assertFalse(signalSubscribeResult.isFailed());
                assertEquals(signalSubscribeResult.getSessionKey(), sessionKey);
                assertEquals(signalSubscribeResult.getSubscriptionId(), subscriptionId);
                assertEquals(3, signalSubscribeResult.getChannels().size());
            }
        });

        ObservableFuture<SubscribeResult> future1 = signalProvider.subscribe(sessionKey, subscriptionId);

        try {
            fail("Should not have succeeded: " + future1.get());
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TimeoutException)) {
                fail("Wrong exception: " + e.getCause());
            }
        }

        assertTrue(future1.isFailed());
    }

    @Test
    public void testSubscribeWhenNotConnected() throws ExecutionException, InterruptedException {
        final String sessionKey = "sessionKey";
        final String subscriptionId = "subscriptionId";

        assertTrue(StringUtil.isNullOrEmpty(signalProvider.getClientId()));
        assertFalse(StringUtil.exists(signalProvider.getClientId()));
        assertNull(signalProvider.getUserAgent());

        signalProvider.getSubscribeEvent().addObserver(new Observer<SubscribeResult>() {
            @Override
            public void notify(Object sender, SubscribeResult signalSubscribeResult) {
                assertNotNull(signalSubscribeResult);
                assertFalse(signalSubscribeResult.isFailed());
                assertEquals(signalSubscribeResult.getSessionKey(), sessionKey);
                assertEquals(signalSubscribeResult.getSubscriptionId(), subscriptionId);
                assertEquals(3, signalSubscribeResult.getChannels().size());
            }
        });

        ObservableFuture<SubscribeResult> future1 = signalProvider.subscribe(sessionKey, subscriptionId);

        try {
            fail("Should not have succeeded: " + future1.get());
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof IllegalStateException)) {
                fail("Wrong exception: " + e.getCause());
            }
        }

        assertTrue(future1.isFailed());

    }

    @Test
    public void testSubscribeWhenWasConnected() throws ExecutionException, InterruptedException {
        final String sessionKey = "sessionKey";
        final String subscriptionId = "subscriptionId";

        assertTrue(StringUtil.isNullOrEmpty(signalProvider.getClientId()));

        ObservableFuture<Void> future = signalProvider.connect(new UserAgent());

        await(future);

        assertTrue(future.isSuccess());

        assertTrue(StringUtil.exists(signalProvider.getClientId()));

        assertNotNull(signalProvider.getUserAgent());

        signalProvider.getSubscribeEvent().addObserver(new Observer<SubscribeResult>() {
            @Override
            public void notify(Object sender, SubscribeResult signalSubscribeResult) {
                assertNotNull(signalSubscribeResult);
                assertFalse(signalSubscribeResult.isFailed());
                assertEquals(signalSubscribeResult.getSessionKey(), sessionKey);
                assertEquals(signalSubscribeResult.getSubscriptionId(), subscriptionId);
                assertEquals(3, signalSubscribeResult.getChannels().size());
            }
        });

        ObservableFuture<SubscribeResult> future1 = signalProvider.subscribe(sessionKey, subscriptionId);

        SubscribeResult signalSubscribeResult = await(future1);

        assertNotNull(signalSubscribeResult);
        assertFalse(signalSubscribeResult.isFailed());
        assertEquals(signalSubscribeResult.getSessionKey(), sessionKey);
        assertEquals(signalSubscribeResult.getSubscriptionId(), subscriptionId);
        assertEquals(2, signalSubscribeResult.getChannels().size());

        signalProvider.disconnect().await();

        ObservableFuture<SubscribeResult> future2 = signalProvider.subscribe(sessionKey, subscriptionId);

        try {
            fail("Should not have succeeded: " + future2.get());
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof IllegalStateException)) {
                fail("Wrong exception: " + e.getCause());
            }
        }

        assertTrue(future2.isFailed());
    }

    private <T> T await(ObservableFuture <T> future) throws ExecutionException, InterruptedException {
        assertTrue("Didn't finish in time", future.await(50, TimeUnit.SECONDS));

        if (future.isFailed()) {
            if (future.getCause() == null) {
                fail("Unknown exception. Future failed.");
            }

            fail(future.getCause().toString());
        }

        return future.get();
    }
}
