package com.zipwhip.api.signals.dto;

import com.zipwhip.signals2.timeline.TimelineEvent;

/**
 * Date: 8/27/13
 * Time: 3:38 PM
 *
 * @author Michael
 * @version 1
 */
public class BindResult implements TimelineEvent {

    private static final long serialVersionUID = -41223725604503270L;

    private final String clientId;
    private final String token;
    private final long timestamp;

    public BindResult(String clientId, String token, long timestamp) {
        this.clientId = clientId;
        this.token = token;
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getClientId() {
        return clientId;
    }

    public String getToken() {
        return token;
    }

}
