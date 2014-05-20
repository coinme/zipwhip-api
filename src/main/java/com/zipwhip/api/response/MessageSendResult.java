package com.zipwhip.api.response;

import com.zipwhip.api.dto.MessageToken;

import java.util.List;

/**
 * @author Michael
 * @date 5/20/2014
 *
 * This object is a little redundant/mungy for legacy reasons. Trying not to break the existing products.
 */
public class MessageSendResult {

    private String subscriberPhoneNumber;
    private String friendPhoneNumber;
    private String body;
    private String advertisement;

    private String root;
    private String fingerprint;
    private List<MessageToken> tokens;

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public List<MessageToken> getTokens() {
        return tokens;
    }

    public void setTokens(List<MessageToken> tokens) {
        this.tokens = tokens;
    }

    public String getSubscriberPhoneNumber() {
        return subscriberPhoneNumber;
    }

    public void setSubscriberPhoneNumber(String subscriberPhoneNumber) {
        this.subscriberPhoneNumber = subscriberPhoneNumber;
    }

    public String getFriendPhoneNumber() {
        return friendPhoneNumber;
    }

    public void setFriendPhoneNumber(String friendPhoneNumber) {
        this.friendPhoneNumber = friendPhoneNumber;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAdvertisement() {
        return advertisement;
    }

    public void setAdvertisement(String advertisement) {
        this.advertisement = advertisement;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MessageSendResult{");
        sb.append("subscriberPhoneNumber='").append(subscriberPhoneNumber).append('\'');
        sb.append(", friendPhoneNumber='").append(friendPhoneNumber).append('\'');
        sb.append(", advertisement='").append(advertisement).append('\'');
        sb.append(", root='").append(root).append('\'');
        sb.append(", fingerprint='").append(fingerprint).append('\'');
        sb.append(", tokens=").append(tokens);
        sb.append('}');
        return sb.toString();
    }
}
