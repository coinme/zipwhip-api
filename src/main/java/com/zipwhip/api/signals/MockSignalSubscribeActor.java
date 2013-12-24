package com.zipwhip.api.signals;

import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.signals2.presence.UserAgent;

/**
 * Date: 8/23/13
 * Time: 1:50 PM
 *
 * @author Michael
 * @version 1
 */
public class MockSignalSubscribeActor implements SignalsSubscribeActor {

    @Override
    public ObservableFuture<Void> subscribe(String clientId, String sessionKey, String subscriptionId, UserAgent presence) {
        return new DefaultObservableFuture<Void>(this, SimpleExecutor.getInstance());
    }

    @Override
    public ObservableFuture<Void> unsubscribe(String clientId, String sessionKey, String subscriptionId) {
        return new DefaultObservableFuture<Void>(this, SimpleExecutor.getInstance());
    }
}
