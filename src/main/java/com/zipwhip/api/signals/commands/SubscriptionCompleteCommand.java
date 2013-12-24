package com.zipwhip.api.signals.commands;

import java.io.Serializable;
import java.util.Collection;

public class SubscriptionCompleteCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private String subscriptionId;
    private Collection<String> channels;

    public SubscriptionCompleteCommand() {

    }

    public SubscriptionCompleteCommand(String subscriptionId, Collection<String> channels) {
        this();

        this.subscriptionId = subscriptionId;
        this.channels = channels;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setChannels(Collection<String> channels) {
        this.channels = channels;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public Collection<String> getChannels() {
        return channels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscriptionCompleteCommand)) return false;

        SubscriptionCompleteCommand that = (SubscriptionCompleteCommand) o;

        if (subscriptionId != null ? !subscriptionId.equals(that.subscriptionId) : that.subscriptionId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return subscriptionId != null ? subscriptionId.hashCode() : 0;
    }
}
