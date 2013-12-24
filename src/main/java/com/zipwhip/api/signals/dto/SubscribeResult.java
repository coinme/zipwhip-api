package com.zipwhip.api.signals.dto;

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
}