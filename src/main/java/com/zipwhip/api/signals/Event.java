package com.zipwhip.api.signals;

import com.zipwhip.signals2.timeline.TimelineEvent;

import java.io.Serializable;
import java.util.Set;

/**
 * Date: 9/3/13
 * Time: 4:34 PM
 *
 * @author Michael
 * @version 1
 */
public interface Event<T extends Serializable> extends TimelineEvent {

    public long getTimestamp();

    public Set<String> getSubscriptionIds();

    public T getContent();

}
