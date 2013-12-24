package com.zipwhip.concurrent;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 9/2/12
 * Time: 1:34 PM
 */
public class TestUtil {

//    public static ConnectionHandle connect(final SignalConnection connection, boolean causedByNetwork) {
//        ObservableFuture<ConnectionHandle> future = connection.connect();
//
//        future.addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
//            @Override
//            public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
//                assertTrue(item.isSuccess());
//                assertFalse(item.getResult().getDisconnectFuture().isDone());
//                assertFalse(item.getResult().isDestroyed());
//                assertNotNull(connection.getConnectionHandle());
//                assertTrue(connection.getConnectionState() == ConnectionState.CONNECTED);
//            }
//        });
//        connection.getConnectEvent().addObserver(new Observer<ConnectionHandle>() {
//            @Override
//            public void notify(Object sender, ConnectionHandle item) {
//                assertFalse(item.isDestroyed());
//                assertTrue(connection.getConnectionState() == ConnectionState.CONNECTED);
//            }
//        });
//        connection.getDisconnectEvent().addObserver(new AssertDisconnectedStateObserver(causedByNetwork));
//
//        return awaitAndAssertSuccess(future);
//    }
//
//    public static ConnectionHandle connect(SignalConnection connection) {
//        return connect(connection, false);
//    }
//
//    public static ConnectionHandle connect(SocketSignalProvider signalProvider) {
//        ObservableFuture<ConnectionHandle> future = signalProvider.connect();
//
//        future.addObserver(new AssertSignalProviderConnectedStateObserver(signalProvider));
//
//        signalProvider.getConnectionChangedEvent().addObserver(new AssertCorrectConnectChangedStateObserver(signalProvider));
//
//        return awaitAndAssertSuccess(future);
//    }

    public static <T> T awaitAndAssertSuccess(ObservableFuture<T> future) {
        assertNotNull("future was null!!", future);
        try {
            assertTrue("future didn't finish!", future.await(9, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertTrue("future wasn't done!", future.isDone());
        assertNull(String.format("Future failure was %s", future.getCause()), future.getCause());
        assertFalse("future was cancelled!", future.isCancelled());
        assertTrue("future wasn't successful!", future.isSuccess());

        return future.getResult();
    }

//    public static void assertDisconnected(ConnectionHandle connectionHandle) {
//        assertNotNull(connectionHandle);
//        assertTrue(connectionHandle.isDestroyed());
//        // it MAY not be done yet. ( order of operations is notify observers before future)
////        assertTrue(connectionHandle.getDisconnectFuture().isDone());
//    }
//
//    public static void assertDisconnected(SocketSignalProvider signalProvider) {
////        assertNull(signalProvider.getCurrentConnectionHandle());
//        assertFalse(signalProvider.isConnected());
//        assertTrue(signalProvider.getConnectionState() == ConnectionState.DISCONNECTED);
//    }

    public static void assertSuccess(ObservableFuture<?> future) {
        assertNotNull(future);
        assertTrue(future.isDone());
        assertTrue(future.isSuccess());
        assertFalse(future.isCancelled());
        assertFalse(future.isFailed());
        assertNotNull(future.getResult());
    }

    public static void assertNotDone(ObservableFuture<?> future) {
        assertNotNull(future);
        assertFalse(future.isDone());
        assertFalse(future.isSuccess());
        assertFalse(future.isCancelled());
        assertFalse(future.isFailed());
        assertNull(future.getResult());
    }

//    public static void assertSignalConnectionDisconnected(SignalConnection signalConnection, ConnectionHandle connectionHandle, boolean causedByNetwork) {
//        if (connectionHandle != null) {
//            assertTrue(connectionHandle.isDestroyed());
//            assertSame("Network values must agree", connectionHandle.disconnectedViaNetwork(), causedByNetwork);
//            assertSuccess(connectionHandle.getDisconnectFuture());
//        }
//
//        assertNull("Should have been null: " + signalConnection.getConnectionHandle(), signalConnection.getConnectionHandle());
//        assertFalse(signalConnection.getConnectionState() == ConnectionState.CONNECTED);
//    }
}
