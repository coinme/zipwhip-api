package com.zipwhip.api.signals;

import com.zipwhip.events.Observable;
import com.zipwhip.signals2.timeline.TimelineEvent;

/**
 * Date: 9/4/13
 * Time: 4:50 PM
 *
 * @author Michael
 * @version 1
 */
public interface BufferedOrderedQueue<T extends TimelineEvent> {

    /**
     * Append an event that has some timestamp. It will be "released" X seconds later via the "itemEvent"
     *
     * The implementation could go 1 of 2 ways.
     *
     * 1) We require complete "silence on the line" for X seconds before releasing any items
     * 2) Same as above, but with a backup timer. No event will wait more than X seconds, it will force release.
     *
     * @param event
     */
    void append(T event);

    /**
     * After X seconds, this event will fire.
     *
     * Events will be released in correct timestamp ordering
     *
     * @return
     */
    Observable<T> getItemEvent();

}
