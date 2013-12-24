package com.zipwhip.api.response;

/**
 * Created by IntelliJ IDEA.
 * Date: Jul 18, 2009
 * Time: 10:37:20 AM
 * <p/>
 * Represents a server response that was null.
 */
public class NullServerResponse extends ServerResponse {

    public NullServerResponse(String raw, boolean success) {
        super(raw, success);
    }

}
