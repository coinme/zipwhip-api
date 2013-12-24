package com.zipwhip.api.signals;

import com.google.gson.JsonElement;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;

/**
 * Date: 9/5/13
 * Time: 3:27 PM
 *
 * @author Michael
 * @version 1
 */
public interface SignalConnection {

    ObservableFuture<Void> connect();

    ObservableFuture<Void> disconnect();

    Observable<Throwable> getExceptionEvent();

    Observable<Void> getConnectEvent();

    Observable<Void> getDisconnectEvent();

    Observable<JsonElement> getMessageEvent();

    boolean isConnected();

    /**
     * Send something to the server.
     *
     * The outer future is for the transmission.
     *
     * The inner future is for the acknowledgement from the server.
     *
     * Both futures should support timeout.
     *
     * @param event
     * @param objects
     * @return
     */
    ObservableFuture<ObservableFuture<Object[]>> emit(String event, Object... objects);

    void reconnect();
}
