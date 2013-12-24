package com.zipwhip.api.response;

public class BooleanServerResponse extends ServerResponse {

    private boolean response;

    public BooleanServerResponse(String raw, boolean success, boolean response) {
        super(raw, success);
        this.response = response;
    }

    public boolean getResponse() {
        return response;
    }

}
