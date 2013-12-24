package com.zipwhip.util;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 8/27/12
 * Time: 3:52 PM
 */
public class StateManagerTest {

    enum ConnectionState {
        NONE, CONNECTED, DISCONNECTED, CONNECTING, DISCONNECTING
    }

    StateManager<ConnectionState> manager;

    @Before
    public void setUp() throws Exception {
        manager = new StateManager<ConnectionState>();
    }

    @Test
    public void testAdd() throws Exception {
        assertNull(manager.get());
        manager.add(ConnectionState.NONE, ConnectionState.CONNECTED);
        assertNull(manager.get());
        manager.set(ConnectionState.NONE);

        assertFalse(manager.transition(ConnectionState.DISCONNECTING));
        assertTrue(manager.transition(ConnectionState.CONNECTED));
    }

    @Test
    public void testSet() throws Exception {
        manager.set(ConnectionState.NONE);
        assertEquals(ConnectionState.NONE, manager.get());
    }

    @Test
    public void testTransition() throws Exception {
        manager.add(ConnectionState.NONE, ConnectionState.CONNECTED);
        manager.add(ConnectionState.CONNECTED, ConnectionState.DISCONNECTED);
        manager.set(ConnectionState.NONE);
        assertEquals(ConnectionState.NONE, manager.get());

        assertFalse(manager.transition(ConnectionState.CONNECTING));
        assertEquals(ConnectionState.NONE, manager.get());

        assertTrue(manager.transition(ConnectionState.CONNECTED));
        assertEquals(ConnectionState.CONNECTED, manager.get());
    }

    @Test
    public void testTransitionOrThrow() throws Exception {
        manager.add(ConnectionState.NONE, ConnectionState.CONNECTED);
        manager.add(ConnectionState.CONNECTED, ConnectionState.DISCONNECTED);
        manager.set(ConnectionState.NONE);
        assertEquals(ConnectionState.NONE, manager.get());

        manager.transitionOrThrow(ConnectionState.CONNECTED);
        assertEquals(ConnectionState.CONNECTED, manager.get());

        boolean threw = false;

        try {
            manager.transitionOrThrow(ConnectionState.CONNECTING);
        } catch (Exception e) {
            threw = true;
            assertEquals(ConnectionState.CONNECTED, manager.get());
        }

        assertTrue(threw);
    }

    @Test
    public void testEnsure() throws Exception {

        manager.set(ConnectionState.DISCONNECTED);
        assertEquals(ConnectionState.DISCONNECTED, manager.get());

        boolean threw = false;

        try {
            manager.ensure(ConnectionState.NONE);
        } catch (Exception e) {
            threw = true;
            assertEquals(ConnectionState.DISCONNECTED, manager.get());
        }

        assertTrue(threw);
    }
}