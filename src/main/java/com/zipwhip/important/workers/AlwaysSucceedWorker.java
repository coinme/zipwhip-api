package com.zipwhip.important.workers;

import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.MutableObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.important.Worker;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 12:46 PM
 */
public class AlwaysSucceedWorker implements Worker {

    @Override
    public ObservableFuture execute(Object o) throws Exception {
        MutableObservableFuture future = new DefaultObservableFuture(this);

        future.setSuccess(null);

        return future;
    }
}
