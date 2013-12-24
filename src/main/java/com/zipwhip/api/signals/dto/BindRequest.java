package com.zipwhip.api.signals.dto;

import com.zipwhip.signals2.presence.UserAgent;

import java.io.Serializable;

/**
 * Date: 8/27/13
 * Time: 2:39 PM
 *
 * A bind request is how you talk to the Signal Server.
 *
 * @author Michael
 * @version 1
 */
public class BindRequest implements Serializable {

    private static final long serialVersionUID = 7083052542742811939L;

    private final String clientId;
    private final String token;
    private final UserAgent userAgent;
    private final double version = 1;

    public BindRequest(UserAgent userAgent, String clientId, String token) {
        this.clientId = clientId;
        this.token = token;
        this.userAgent = userAgent;
    }

    public BindRequest(UserAgent userAgent) {
        this(userAgent, null, null);
    }

    public String getClientId() {
        return clientId;
    }

    public String getToken() {
        return token;
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public double getVersion() {
        return version;
    }
}
