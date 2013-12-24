package com.zipwhip.api.signals.dto.json;

import com.google.gson.JsonObject;
import com.zipwhip.api.signals.dto.BindResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Date: 9/13/13
 * Time: 2:37 PM
 *
 * @author Michael
 * @version 1
 */
public class BindResultTypeAdapterTest {

    private BindResultTypeAdapter adapter = new BindResultTypeAdapter();

    @Test
    public void testAdapter() throws Exception {
        String clientId = "clientId";
        long timestamp = System.currentTimeMillis();
        String token = "asdf-asdf-asdf-sadf";

        JsonObject object = new JsonObject();

        object.addProperty("token", token);
        object.addProperty("timestamp", timestamp);
        object.addProperty("clientId", clientId);

        BindResult bindResult = adapter.deserialize(object, null, null);

        assertEquals(bindResult.getClientId(), clientId);
        assertEquals(bindResult.getTimestamp(), timestamp);
        assertEquals(bindResult.getToken(), token);
    }
}
