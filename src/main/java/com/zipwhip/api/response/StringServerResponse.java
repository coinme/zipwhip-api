package com.zipwhip.api.response;

/**
 * Created by IntelliJ IDEA.
 * * Date: Jul 18, 2009
 * Time: 10:23:38 AM
 */
public class StringServerResponse extends ServerResponse {

    public String response;

    public StringServerResponse(String raw, boolean success, String response) {
        super(raw, success);
        this.response = response;
    }

}
