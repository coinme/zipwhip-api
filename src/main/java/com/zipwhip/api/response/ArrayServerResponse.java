package com.zipwhip.api.response;

import org.json.JSONArray;

/**
 * Created by IntelliJ IDEA.
 * Date: Jul 20, 2009
 * Time: 5:41:19 PM
 * <p/>
 * Represents an Array of data.
 */
public class ArrayServerResponse extends ServerResponse {

    public JSONArray response;

    public ArrayServerResponse(String raw, boolean success, JSONArray response) {
        super(raw, success);
        this.response = response;
    }

}
