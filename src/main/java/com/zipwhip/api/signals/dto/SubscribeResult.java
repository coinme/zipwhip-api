package com.zipwhip.api.signals.dto;

import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.StringUtil;

import java.util.Set;

/**
 * Date: 8/20/13
 * Time: 4:44 PM
 *
 * @author Michael
 * @version 1
 */
public class SubscribeResult {

    private String sessionKey;
    private String subscriptionId;
    private Throwable cause;
    private Set<String> channels;

    public SubscribeResult() {

    }

    public SubscribeResult(String sessionKey, String subscriptionId, Throwable cause) {
        this.sessionKey = sessionKey;
        this.subscriptionId = subscriptionId;
        this.cause = cause;
    }

    public SubscribeResult(String sessionKey, String subscriptionId) {
        this.sessionKey = sessionKey;
        this.subscriptionId = subscriptionId;
    }

    public SubscribeResult(String sessionKey, String subscriptionId, Set<String> channels) {
        this.sessionKey = sessionKey;
        this.subscriptionId = subscriptionId;
        this.channels = channels;
    }

    public Set<String> getChannels() {
        return channels;
    }

    public void setChannels(Set<String> channels) {
        this.channels = channels;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public boolean isFailed() {
        return cause != null;
    }

    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SubscribeResult{");

        if (cause != null) {
            sb.append("cause=").append(cause);
        }

        if (StringUtil.exists(sessionKey)) {
            sb.append(", sessionKey='").append(sessionKey).append('\'');
        }

        if (StringUtil.exists(subscriptionId)) {
            sb.append(", subscriptionId='").append(subscriptionId).append('\'');
        }

        if (CollectionUtil.exists(channels)) {
            sb.append(", channels=").append(channels);
        }

        sb.append('}');

        return sb.toString();
    }
}