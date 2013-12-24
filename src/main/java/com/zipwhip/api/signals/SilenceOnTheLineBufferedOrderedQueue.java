package com.zipwhip.api.signals;

import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.signals2.timeline.TimelineEvent;
import com.zipwhip.timers.HashedWheelTimer;
import com.zipwhip.timers.Timer;
import com.zipwhip.util.BufferedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.PriorityQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Date: 9/4/13
 * Time: 4:51 PM
 *
 * This implementation requires "complete silence on the line"
 *
 * If you have a steady stream of signals that occur faster than your timeout, then no signals will ever be released.
 *
 * @author Michael
 * @version 1
 */
public class SilenceOnTheLineBufferedOrderedQueue<T extends TimelineEvent> extends CascadingDestroyableBase implements BufferedOrderedQueue<T>, Runnable{

    private static final Logger LOGGER = LoggerFactory.getLogger(BufferedOrderedQueue.class);

    private final BufferedRunnable runnable;
    private final PriorityQueue<T> queue = new PriorityQueue<T>();
    private final ObservableHelper<T> itemEvent;

    public SilenceOnTheLineBufferedOrderedQueue(Executor eventExecutor, Timer timer, long delay, TimeUnit timeUnit) {
        this.runnable = new BufferedRunnable(timer, this, delay, timeUnit);
        this.itemEvent = new ObservableHelper<T>("BufferedOrderedQueue/itemEvent", eventExecutor);
    }

    public SilenceOnTheLineBufferedOrderedQueue(Timer timer, long delay, TimeUnit timeUnit) {
        this(SimpleExecutor.getInstance(), timer, delay, timeUnit);
    }

    public SilenceOnTheLineBufferedOrderedQueue(Timer timer) {
        this(timer, 1, TimeUnit.SECONDS);
    }

    public SilenceOnTheLineBufferedOrderedQueue() {
        this(new HashedWheelTimer());

        this.link(new DestroyableBase() {
            @Override
            protected void onDestroy() {
                runnable.getTimer();
                runnable.getTimer().stop();
            }
        });
    }

    @Override
    public synchronized void append(T event) {
        queue.add(event);
        runnable.run();
    }

    @Override
    public Observable<T> getItemEvent() {
        return itemEvent;
    }

    /**
     * Release all of the items in the queue (in order) until the queue is empty.
     *
     * Due to synchronization, you cannot append to the queue while it's processing work.
     */
    @Override
    public synchronized void run() {
        T item = queue.poll();

        while (item != null) {
            itemEvent.notifyObservers(this, item);

            item = queue.poll();
        }
    }

    @Override
    protected void onDestroy() {

    }
}
