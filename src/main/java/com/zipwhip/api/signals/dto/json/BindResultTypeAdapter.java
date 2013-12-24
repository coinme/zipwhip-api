package com.zipwhip.api.signals.dto.json;

import com.google.gson.*;
import com.zipwhip.api.signals.dto.BindResult;
import com.zipwhip.gson.GsonUtil;

import java.lang.reflect.Type;

/**
 * Date: 8/27/13
 * Time: 4:31 PM
 *
 * @author Michael
 * @version 1
 */
public class BindResultTypeAdapter implements JsonDeserializer<BindResult> {

    @Override
    public BindResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = (JsonObject)json;

        String clientId = GsonUtil.getString(object, "clientId");
        String token = GsonUtil.getString(object, "token");
        Long timestamp = GsonUtil.getLong(object.get("timestamp"));

        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }

        return new BindResult(clientId, token, timestamp);
    }
}
