package com.zipwhip.vendor;

import com.google.gson.*;
import com.zipwhip.api.dto.MessageToken;
import com.zipwhip.api.response.MessageSendResult;
import com.zipwhip.gson.GsonUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael
 * @date 5/20/2014
 */
public class MessageSendResultTypeAdapter implements JsonDeserializer<MessageSendResult> {

    @Override
    public MessageSendResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json == null || json.isJsonNull()) {
            return null;
        }

        MessageSendResult result = new MessageSendResult();

        result.setFingerprint(GsonUtil.getString(json, "fingerprint"));
        result.setRoot(GsonUtil.getString(json, "root"));
        result.setTokens(parseTokens(Long.valueOf(result.getRoot()), GsonUtil.getArray(json, "tokens")));

        return result;
    }

    private List<MessageToken> parseTokens(long rootMessageId, JsonArray tokens) {
        if (tokens == null || tokens.size() == 0) {
            return null;
        }

        List<MessageToken> result = new ArrayList<MessageToken>(tokens.size());

        for (JsonElement json : tokens) {
            MessageToken token = new MessageToken();

            token.setFingerprint(GsonUtil.getString(json, "fingerprint"));
            token.setContactId(GsonUtil.getLong(json, "contact"));
            token.setDeviceId(GsonUtil.getLong(json, "fingerprint"));
            token.setMessageId(Long.valueOf(GsonUtil.getString(json, "message")));
            token.setRootMessageId(rootMessageId);

            result.add(token);
        }

        return result;
    }
}
