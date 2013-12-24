package com.zipwhip.api.signals.dto.json;

import com.google.gson.*;
import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.api.signals.dto.SubscribeCompleteContent;
import com.zipwhip.gson.GsonUtil;
import com.zipwhip.signals2.SignalContact;
import com.zipwhip.signals2.SignalConversation;
import com.zipwhip.signals2.SignalMessage;
import com.zipwhip.signals2.presence.Presence;
import com.zipwhip.util.StringUtil;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.TreeSet;

/**
 * Date: 8/22/13
 * Time: 4:56 PM
 *
 * @author Michael
 * @version 1
 */
public class DeliveredMessageTypeAdapter implements JsonSerializer<DeliveredMessage>, JsonDeserializer<DeliveredMessage> {

    @Override
    public DeliveredMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = (JsonObject)json;

        DeliveredMessage message = new DeliveredMessage();

        JsonArray array = object.getAsJsonArray("subscriptionIds");
        if (array != null && !array.isJsonNull() && array.size() > 0) {
            Set<String> strings = new TreeSet<String>();
            for (JsonElement e : array) {
                if (e == null || e.isJsonNull()) {
                    continue;
                }

                strings.add(e.getAsString());
            }

            message.setSubscriptionIds(strings);
        }

        message.setTimestamp(GsonUtil.getLong(object.get("timestamp")));
        message.setId(GsonUtil.getString(object.get("id")));
        message.setEvent(GsonUtil.getString(object.get("event")));
        message.setType(GsonUtil.getString(object.get("type")));

        JsonElement content = object.get("content");

        //
        // Try to parse some WELL KNOWN commands
        //
        if (StringUtil.equalsIgnoreCase(message.getType(), "subscribe")) {
            if (StringUtil.equalsIgnoreCase(message.getEvent(), "complete")) {
                message.setContent(context.<SubscribeCompleteContent>deserialize(content, SubscribeCompleteContent.class));
            }
        } else if (StringUtil.equalsIgnoreCase(message.getType(), "presence")) {
            message.setContent(context.<Presence>deserialize(content, Presence.class));
        } else if (StringUtil.equalsIgnoreCase(message.getType(), "message")) {
            if (StringUtil.equalsIgnoreCase(message.getEvent(), "progress")) {
                message.setContent(GsonUtil.getString(content));
            } else {
                message.setContent(context.<SignalMessage>deserialize(content, SignalMessage.class));
            }
        } else if (StringUtil.equalsIgnoreCase(message.getType(), "contact")) {
            message.setContent(context.<SignalContact>deserialize(content, SignalContact.class));
        } else if (StringUtil.equalsIgnoreCase(message.getType(), "conversation")) {
            message.setContent(context.<SignalConversation>deserialize(content, SignalConversation.class));
        }

        //
        //
        //
        if (message.getContent() == null && !GsonUtil.isNull(content)) {
            Object _content = GsonUtil.getDefaultValue(getClass().getClassLoader(), context, content);

            if (!(_content instanceof Serializable)) {
                throw new JsonParseException("The content returned for was not serializable! " + _content);
            } else {
                message.setContent((Serializable) _content);
            }
        }

        return message;
    }

    @Override
    public JsonElement serialize(DeliveredMessage src, Type typeOfSrc, JsonSerializationContext context) {
        return null;
    }
}
