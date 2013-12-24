package com.zipwhip.api.signals.dto.json;

import com.google.gson.*;
import com.zipwhip.api.signals.dto.BindResult;
import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.api.signals.dto.SubscribeCompleteContent;
import com.zipwhip.signals2.address.Address;

import java.lang.reflect.Type;
import java.util.Date;

/**
 * Date: 9/5/13
 * Time: 4:09 PM
 *
 * @author Michael
 * @version 1
 */
public class SignalProviderGsonBuilder {

    private static final Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(DeliveredMessage.class, new DeliveredMessageTypeAdapter())
            .registerTypeHierarchyAdapter(Address.class, new AddressTypeConverter())
            .registerTypeHierarchyAdapter(SubscribeCompleteContent.class, new SubscribeCompleteContentTypeAdapter())
            .registerTypeHierarchyAdapter(BindResult.class, new BindResultTypeAdapter())

                    // We support dates in the System.currentTimeMillis() format.
                    // I wonder how to support BOTH the string format AND the millis format.
            .registerTypeHierarchyAdapter(Date.class, new JsonDeserializer<Date>() {
                @Override
                public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws
                        JsonParseException {
                    return new Date(json.getAsJsonPrimitive().getAsLong());
                }
            })
//            .registerTypeHierarchyAdapter(Presence.class, new PresenceTypeAdapter())
            .create();

    public static Gson getInstance() {
        return gson;
    }

}
