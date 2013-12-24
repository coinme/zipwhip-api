package com.zipwhip.api.response;

/**
 * Represents a ServerResponse that might be in some unknown format. We don't know if it's JSON or XML or binary encoded.
 */
public abstract class ServerResponse {

    private boolean success;
    private String raw;

    public ServerResponse(String raw, boolean success) {
        this.raw = raw;
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

}
