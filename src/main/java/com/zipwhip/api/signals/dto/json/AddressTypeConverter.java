package com.zipwhip.api.signals.dto.json;

import com.google.gson.*;
import com.zipwhip.gson.GsonUtil;
import com.zipwhip.signals2.address.Address;
import com.zipwhip.signals2.address.ChannelAddress;
import com.zipwhip.signals2.address.ClientAddress;
import com.zipwhip.util.StringUtil;

import java.lang.reflect.Type;

/**
 * Date: 8/22/13
 * Time: 5:05 PM
 *
 * @author Michael
 * @version 1
 */
public class AddressTypeConverter implements JsonSerializer<Address>, JsonDeserializer<Address> {

    @Override
    public Address deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = (JsonObject)json;

        String clientId = GsonUtil.getString(object.get("clientId"));
        if (StringUtil.exists(clientId)) {
            return new ClientAddress(clientId);
        }

        String channel = GsonUtil.getString(object.get("channel"));
        if (StringUtil.exists(channel)) {
            return new ChannelAddress(channel);
        }

        throw new JsonParseException("Unknown type! " + json.toString());
    }

    @Override
    public JsonElement serialize(Address src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();

        if (src instanceof ClientAddress) {
            object.addProperty("clientId", ((ClientAddress) src).getClientId());
        } else if (src instanceof ChannelAddress) {
            object.addProperty("channel", ((ChannelAddress) src).getChannel());
        } else {
            throw new RuntimeException("Not sure what type : " + src);
        }

        return object;
    }
}
