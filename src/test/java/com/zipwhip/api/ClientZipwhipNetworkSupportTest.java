package com.zipwhip.api;

import com.zipwhip.executors.SimpleExecutor;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/30/12
 * Time: 6:36 PM
 */
public class ClientZipwhipNetworkSupportTest {

    @Test
    public void testRan() throws Exception {
        final boolean[] run = {false};

        ClientZipwhipNetworkSupport client = new ClientZipwhipNetworkSupport(new HttpConnection(), new MockSignalProvider()) {
            @Override
            protected void onDestroy() {

            }
        };
        client.executor = new SimpleExecutor();

        client.runIfActive(new Runnable() {
            @Override
            public void run() {
                run[0] = true;
            }
        });

        assertTrue(run[0]);
    }

    @Test
    public void testNotRunBecauseSessionKey() throws Exception {
        final boolean[] run = {false};

        ClientZipwhipNetworkSupport client = new ClientZipwhipNetworkSupport(new HttpConnection(), new MockSignalProvider()) {
            @Override
            protected void onDestroy() {

            }
        };
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        final Executor executor = Executors.newSingleThreadExecutor();

        client.executor = new Executor() {
            @Override
            public void execute(final Runnable command) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            latch.await(4, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            fail(e.getMessage());
                            return;
                        }
                        command.run();
                        latch2.countDown();
                    }
                });
            }
        };

        client.connection.setSessionKey("0");

        client.runIfActive(new Runnable() {
            @Override
            public void run() {
                run[0] = true;
            }
        });

        client.connection.setSessionKey("1");

        latch.countDown();
        latch2.await(4, TimeUnit.SECONDS);

        assertFalse(run[0]);
    }

    @Test
    public void testNotRunBecauseClientId() throws Exception {
        final boolean[] run = {false};

        ClientZipwhipNetworkSupport client = new ClientZipwhipNetworkSupport(new HttpConnection(), new MockSignalProvider()) {
            @Override
            protected void onDestroy() {

            }
        };
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        final Executor executor = Executors.newSingleThreadExecutor();

        client.executor = new Executor() {
            @Override
            public void execute(final Runnable command) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            latch.await(4, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            fail(e.getMessage());
                            return;
                        }
                        command.run();
                        latch2.countDown();
                    }
                });
            }
        };

        ((MockSignalProvider)client.signalProvider).clientId = "0";

        client.runIfActive(new Runnable() {
            @Override
            public void run() {
                run[0] = true;
            }
        });

        ((MockSignalProvider)client.signalProvider).clientId = "1";

        latch.countDown();
        latch2.await(4, TimeUnit.SECONDS);

        assertFalse(run[0]);
    }
}
