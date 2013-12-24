package com.zipwhip.api.signals.dto.json;

import com.google.gson.*;
import com.zipwhip.api.signals.dto.SubscribeCompleteContent;
import com.zipwhip.gson.GsonUtil;

import java.lang.reflect.Type;

/**
 * Date: 8/22/13
 * Time: 5:15 PM
 *
 * @author Michael
 * @version 1
 */
public class SubscribeCompleteContentTypeAdapter implements JsonDeserializer<SubscribeCompleteContent> {

    @Override
    public SubscribeCompleteContent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = (JsonObject)json;

        SubscribeCompleteContent content = new SubscribeCompleteContent();

        content.setAddresses(GsonUtil.getSet(object.get("addresses")));
        content.setSubscriptionId(GsonUtil.getString(object.get("subscriptionId")));

        return content;
    }
}
