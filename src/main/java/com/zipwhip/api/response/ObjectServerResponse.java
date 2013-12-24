package com.zipwhip.api.response;

import org.json.JSONObject;

/**
 * Represents a JSON Object ServerResponse.
 */
public class ObjectServerResponse extends ServerResponse {

    public JSONObject response;

    public ObjectServerResponse(String raw, boolean success, JSONObject response) {
        super(raw, success);
        this.response = response;
    }

}
