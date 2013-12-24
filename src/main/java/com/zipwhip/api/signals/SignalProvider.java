package com.zipwhip.api.signals;

import com.zipwhip.api.signals.dto.BindResult;
import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.api.signals.dto.SubscribeResult;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.signals2.presence.Presence;
import com.zipwhip.signals2.presence.UserAgent;

/**
 * Date: 5/7/13
 * Time: 4:41 PM
 *
 * SignalProvider is the only class you need to talk with the signal server. The old design broke up the
 * /signals/connect and clientId work into 2 classes.
 *
 * @author Michael Smyers
 * @version 2
 */
public interface SignalProvider extends Destroyable {

    boolean isConnected();

    /**
     * Tell it to connect. This call is idempotent, so if multiple calls to
     * a connection provider (if already connected) will have no effect.
     *
     * If you do not have a presence object set on this, it will fail.
     *
     * You must have UserAgent information defined.
     *
     * @throws IllegalStateException If you do not have userAgent information defined.
     * @return a ObservableFuture task indicating if the connection was successful.
     * @throws Exception if an error is encountered when connecting
     */
    ObservableFuture<Void> connect(UserAgent userAgent) throws IllegalStateException;
    ObservableFuture<Void> connect(UserAgent userAgent, String clientId, String token) throws IllegalStateException;

    /**
     * Tell it to disconnect. Will not reconnect.
     *
     * @return A future that can be observed
     * @throws Exception if an I/O happens while disconnecting
     */
    ObservableFuture<Void> disconnect();

    /**
     * Bind a sessionKey+subscriptionId to a clientId. The server will respond with a SubscriptionCompleteCommand.
     *
     * The server will remember this binding permanently. You only need to do this during the initial login phase
     * of your app.
     *
     * @param sessionKey
     * @param subscriptionId
     * @return
     */
    ObservableFuture<SubscribeResult> subscribe(String sessionKey, String subscriptionId);

    /**
     *
     *
     * @param subscriptionId
     * @return
     */
    ObservableFuture<Void> unsubscribe(String sessionKey, String subscriptionId);

    /**
     * Will reset the state, followed by a disconnect, followed by an immediate reconnect.
     *
     * @throws Exception
     */
    ObservableFuture<Void> resetDisconnectAndConnect();

    /**
     * A "bind" event occurs when the sessionId is bound to the clientId
     *
     * @return
     */
    Observable<BindResult> getBindEvent();

    /**
     * A "subscribe" event occurs when the sessionKey is bound to a clientId
     *
     * @return
     */
    Observable<SubscribeResult> getSubscribeEvent();

    Observable<SubscribeResult> getUnsubscribeEvent();

    /**
     * Fires when ANY presence changes for ANY clientId associated with your channels.
     *
     * For example, clientId1 and clientId2 both are associated with user1. Both clients will hear each others presence
     * change events.
     *
     * @return
     */
    Observable<Event<Presence>> getPresenceChangedEvent();

    Observable<DeliveredMessage> getSignalReceivedEvent();

    /**
     * If any parsing exception occurs, or connection exception. Should generally test for the exception type
     *
     * JsonParseException - json exception
     * SocketIOException - connectivity exception
     *
     * @return
     */
    Observable<Throwable> getExceptionEvent();

        /**
         * The presence information that this client is conveying to cloud.
         *
         * @return
         */
    UserAgent getUserAgent();

    /**
     * The SignalServer uses a separate id to track you, because it's an Id
     * given to a TCP/IP connection, not a user.
     *
     * The server gives out clientIds
     *
     * @return the id that the SignalServer has for us
     */
    String getClientId();

}