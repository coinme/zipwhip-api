package com.zipwhip.api.signals.dto;

import java.io.Serializable;
import java.util.Set;

/**
 * Date: 7/31/13
 * Time: 10:46 AM
 *
 * @author Michael
 * @version 1
 */
public class SubscribeCompleteContent implements Serializable {

    private static final long serialVersionUID = 4976833416056269393L;

    private String subscriptionId;
    private Set<String> addresses;

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Set<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(Set<String> addresses) {
        this.addresses = addresses;
    }
}
