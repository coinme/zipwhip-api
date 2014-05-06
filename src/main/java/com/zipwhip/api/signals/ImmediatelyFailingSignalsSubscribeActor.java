package com.zipwhip.api.signals;

import com.zipwhip.concurrent.FakeFailingObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;

/**
 * @author Michael
 * @date 5/6/2014
 */
public class ImmediatelyFailingSignalsSubscribeActor implements SignalsSubscribeActor {

    @Override
    public ObservableFuture<Void> subscribe(String clientId, String sessionKey, String subscriptionId) {
        return new FakeFailingObservableFuture<Void>(this, new RuntimeException("fail"));
    }

    @Override
    public ObservableFuture<Void> subscribe(String clientId, String sessionKey, String scope, String subscriptionId) {
        return new FakeFailingObservableFuture<Void>(this, new RuntimeException("fail"));
    }

    @Override
    public ObservableFuture<Void> unsubscribe(String clientId, String sessionKey, String scope, String subscriptionId) {
        return new FakeFailingObservableFuture<Void>(this, new RuntimeException("fail"));
    }

    @Override
    public ObservableFuture<Void> unsubscribe(String clientId, String sessionKey, String subscriptionId) {
        return new FakeFailingObservableFuture<Void>(this, new RuntimeException("fail"));
    }
}
