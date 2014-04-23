package com.zipwhip.api.signals;

import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.signals2.presence.UserAgent;

/**
 * Date: 7/30/13
 * Time: 6:09 PM
 *
 * Executes /signals/connect against Zipwhip to subscribe a clientId + sessionKey.
 *
 * @author Michael
 * @version 1
 */
public interface SignalsSubscribeActor {

    ObservableFuture<Void> subscribe(String clientId, String sessionKey, String subscriptionId);

    ObservableFuture<Void> subscribe(String clientId, String sessionKey, String scope, String subscriptionId);

    ObservableFuture<Void> unsubscribe(String clientId, String sessionKey, String scope, String subscriptionId);

    ObservableFuture<Void> unsubscribe(String clientId, String sessionKey, String subscriptionId);

}
