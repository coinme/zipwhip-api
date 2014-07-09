package com.zipwhip.api.signals;

import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.signals2.timeline.TimelineEvent;
import com.zipwhip.util.BufferedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.PriorityQueue;
import java.util.concurrent.Executor;

/**
 * @author Michael
 * @date 7/8/2014
 */
public class NoWaitBufferedOrderedQueue<T extends TimelineEvent> implements BufferedOrderedQueue<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoWaitBufferedOrderedQueue.class);

    private final ObservableHelper<T> itemEvent;

    public NoWaitBufferedOrderedQueue(Executor eventExecutor) {
        this.itemEvent = new ObservableHelper<T>("BufferedOrderedQueue/itemEvent", eventExecutor);
    }

    public NoWaitBufferedOrderedQueue() {
        this.itemEvent = new ObservableHelper<T>("BufferedOrderedQueue/itemEvent", SimpleExecutor.getInstance());
    }

    @Override
    public void append(T event) {
        itemEvent.notifyObservers(this, event);
    }

    @Override
    public Observable<T> getItemEvent() {
        return itemEvent;
    }
}
