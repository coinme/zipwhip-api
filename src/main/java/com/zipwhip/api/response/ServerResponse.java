package com.zipwhip.api.response;

import com.zipwhip.api.signals.Signal;

import java.util.List;
import java.util.Map;

/**
 * Represents a ServerResponse that might be in some unknown format. We don't know if it's JSON or XML or binary encoded.
 */
public abstract class ServerResponse {

    private boolean success;
    private Map<String, Map<String, List<Signal>>> sessions;
    private String raw;

    public ServerResponse(String raw, boolean success, Map<String, Map<String, List<Signal>>> sessions) {
        this.raw = raw;
        this.success = success;
        this.sessions = sessions;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Map<String, Map<String, List<Signal>>> getSessions() {
        return sessions;
    }

    public void setSessions(Map<String, Map<String, List<Signal>>> sessions) {
        this.sessions = sessions;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

}
