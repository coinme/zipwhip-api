package com.zipwhip.util;

import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/12/12
 * Time: 2:11 PM
 *
 * Interrupt the thread if cancelled.
 */
public class InterruptThreadIfCancelledObserver<T> implements Observer<ObservableFuture<T>> {

    private final Thread thread;

    public InterruptThreadIfCancelledObserver(Thread thread) {
        this.thread = thread;
    }

    public InterruptThreadIfCancelledObserver() {
        this(Thread.currentThread());
    }

    @Override
    public void notify(Object sender, ObservableFuture<T> future) {
        if (future.isCancelled()) {
            thread.interrupt();
        }
    }
}
