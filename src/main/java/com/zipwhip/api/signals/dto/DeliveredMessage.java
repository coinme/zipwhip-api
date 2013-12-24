package com.zipwhip.api.signals.dto;

import com.zipwhip.api.signals.Event;
import com.zipwhip.signals2.timeline.TimelineEvent;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Set;

/**
 * Date: 8/22/13
 * Time: 4:46 PM
 *
 * @author Michael
 * @version 1
 */
public class DeliveredMessage<T extends Serializable> implements TimelineEvent, Comparable<DeliveredMessage>, Event<T> {

    private static final long serialVersionUID = 4614754472272105186L;

    private Set<String> subscriptionIds;
    private String id;
    private long timestamp;
    private String event;
    private String type;
    private T content;

    public DeliveredMessage() {
    }

    public Set<String> getSubscriptionIds() {
        return subscriptionIds;
    }

    public void setSubscriptionIds(Set<String> subscriptionIds) {
        this.subscriptionIds = subscriptionIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    @Override
    public int compareTo(DeliveredMessage o) {
        long timestamp1 = o == null ? 0 : o.getTimestamp();
        long timestamp2 = this.getTimestamp();

        if (timestamp1 == timestamp2) {
            return 0;
        } else if (timestamp1 > timestamp2) {
            return -1;
        } else {
            return 1;
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("subscriptionIds", subscriptionIds)
                .append("id", id)
                .append("timestamp", timestamp)
                .append("event", event)
                .append("type", type)
                .append("content", content)
                .toString();
    }
}
