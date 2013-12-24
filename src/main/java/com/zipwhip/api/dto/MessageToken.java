package com.zipwhip.api.dto;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * * Date: Jul 17, 2009
 * Time: 8:23:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessageToken implements Serializable {

    private static final long serialVersionUID = 5878234954952365L;

    /**
     * This is the message uuid
     */
    long messageId;
    long deviceId;
    long contactId;
    String fingerprint;
    /**
     * The message ID of the root message associated with this message token.  In situations
     * where the message token is the root message, the rootMessage and message will be the same.
     * In situations where a sendMessage call returns more than one MessageToken, the rootMessage will
     * be the same across all MessageTokens.
     */
    long rootMessageId;

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public long getRootMessageId() {
        return rootMessageId;
    }

    public void setRootMessageId(long rootMessageId) {
        this.rootMessageId = rootMessageId;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder("==> MessageToken details:");
        toStringBuilder.append("\nMessage: ").append(messageId);
        toStringBuilder.append("\nRoot Message: ").append(rootMessageId);
        toStringBuilder.append("\nDeviceId: ").append(deviceId);
        toStringBuilder.append("\nContactId: ").append(contactId);
        toStringBuilder.append("\nFingerprint: ").append(fingerprint);
        return toStringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageToken)) return false;

        MessageToken that = (MessageToken) o;

        if (contactId != that.contactId) return false;
        if (deviceId != that.deviceId) return false;
        if (messageId != that.messageId) return false;
        if (rootMessageId != that.rootMessageId) return false;
        if (fingerprint != null ? !fingerprint.equals(that.fingerprint) : that.fingerprint != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (messageId ^ (messageId >>> 32));
        result = 31 * result + (int) (deviceId ^ (deviceId >>> 32));
        result = 31 * result + (int) (contactId ^ (contactId >>> 32));
        result = 31 * result + fingerprint.hashCode();
        result = 31 * result + (int) (rootMessageId ^ (rootMessageId >>> 32));
        return result;
    }
}
