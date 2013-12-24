package com.zipwhip.api.response;

import com.zipwhip.api.dto.*;
import com.zipwhip.signals2.presence.Presence;
import com.zipwhip.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Date: Jul 18, 2009
 * Time: 10:22:28 AM
 * <p/>
 * Parses a ServerResponse from a Json string.
 */
public class JsonResponseParser implements ResponseParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonResponseParser.class);
    private static final String EMPTY_OBJECT = "{}";

    private JsonDtoParser parser = new JsonDtoParser();

    @Override
    public ServerResponse parse(String response) throws JSONException {

        LOGGER.debug("Parsing" + response);

        if (StringUtil.isNullOrEmpty(response)) {
            return null;
        } else if (StringUtil.equalsIgnoreCase("null", response)) {
            return null;
        }

        JSONObject thing = new JSONObject(response);
        String responseKey = "response";

        boolean success = thing.optBoolean("success");

        // IS THIS A COMPLEX OBJECT?
        JSONObject jsonObject = thing.optJSONObject(responseKey);
        if (jsonObject != null) {
            return new ObjectServerResponse(response, success, jsonObject);
        }

        // IS THIS AN ARRAY?
        JSONArray jsonArray = thing.optJSONArray(responseKey);
        if (jsonArray != null) {
            return new ArrayServerResponse(response, success, jsonArray);
        }

        // IS THIS A STRING?
        String string = thing.optString(responseKey, null);

        // Unfortunately the JSON libs in Android coerce bool into Strings
        if (string != null && !string.equalsIgnoreCase("true") && !string.equalsIgnoreCase("false")) {
            // a string
            return new StringServerResponse(response, success, string);
        }

        /// MIGHT BE A BOOLEAN
        try {
            boolean bool = thing.getBoolean(responseKey);
            return new BooleanServerResponse(response, success, bool);
        } catch (Exception e) {
            // NOPE, JUST RETURN THE RAW RESULT
            return new StringServerResponse(response, true, response);
        }
    }

    @Override
    public List<MessageToken> parseMessageTokens(ServerResponse serverResponse) throws Exception {

        List<MessageToken> result = null;

        if (serverResponse instanceof ObjectServerResponse) {
            ObjectServerResponse r = (ObjectServerResponse) serverResponse;
            JSONArray array = r.response.getJSONArray("tokens");
            result = parser.parseMessageTokens(r.response, array);
        } else {
            throw new Exception("ServerResponse must be an ObjectServerResponse");
        }

        return result;
    }

    @Override
    public Message parseMessage(ServerResponse serverResponse) throws Exception {
        Message result = null;
        if (serverResponse instanceof ObjectServerResponse) {
            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;

            return parser.parseMessage(cplx.response);
        }
        return result;
    }

    @Override
    public List<Message> parseMessages(ServerResponse serverResponse) throws Exception {

        if (!(serverResponse instanceof ArrayServerResponse)) {
            throw new Exception("ServerResponse must be an ArrayServerResponse");
        }

        List<Message> messages = new ArrayList<Message>();

        ArrayServerResponse array = (ArrayServerResponse) serverResponse;

        for (int i = 0; i < array.response.length(); i++) {
            messages.add(parser.parseMessage(array.response.getJSONObject(i)));
        }

        return messages;
    }

    @Override
    public MessageListResult parseMessagesListResult(ServerResponse serverResponse) throws Exception {

        if (!(serverResponse instanceof ArrayServerResponse)) {
            throw new Exception("ServerResponse must be an ArrayServerResponse");
        }

        List<Message> messages = new ArrayList<Message>();

        ArrayServerResponse array = (ArrayServerResponse) serverResponse;

        for (int i = 0; i < array.response.length(); i++) {
            messages.add(parser.parseMessage(array.response.getJSONObject(i)));
        }

        MessageListResult result = new MessageListResult();
        result.setMessages(messages);

        JSONObject rawObject = new JSONObject(serverResponse.getRaw());
        result.setTotal(rawObject.optInt("total", 0));
        result.setSize(rawObject.optInt("size", 0));

        return result;
    }

    @Override
    public List<Message> parseMessagesFromConversation(ServerResponse serverResponse) throws Exception {

        if (serverResponse instanceof ObjectServerResponse) {
            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;

            if (cplx.response.has("messages")) {
                List<Message> messages = new ArrayList<Message>();
                JSONArray json = cplx.response.optJSONArray("messages");

                if (json == null) {
                    return messages;
                }

                for (int i = 0; i < json.length(); i++) {
                    messages.add(parser.parseMessage(json.getJSONObject(i)));
                }
                return messages;
            }
        }
        return null;
    }

    @Override
    public Device parseDevice(ServerResponse serverResponse) throws Exception {
        if (serverResponse instanceof ObjectServerResponse) {
            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;

            return parser.parseDevice(cplx.response);
        }
        return null;
    }

    @Override
    public List<Device> parseDevices(ServerResponse serverResponse) throws Exception {

        if (!(serverResponse instanceof ArrayServerResponse)) {
            throw new Exception("ServerResponse must be an ArrayServerResponse");
        }

        List<Device> devices = new ArrayList<Device>();

        ArrayServerResponse array = (ArrayServerResponse) serverResponse;

        for (int i = 0; i < array.response.length(); i++) {
            devices.add(parser.parseDevice(array.response.getJSONObject(i)));
        }

        return devices;
    }

    @Override
    public String parseString(ServerResponse serverResponse) throws Exception {
        String result;
        if (serverResponse instanceof StringServerResponse) {
            StringServerResponse stringServerResponse = (StringServerResponse) serverResponse;
            result = stringServerResponse.response;
        } else {
            throw new Exception("Unknown or unexpected server response");
        }
        return result;
    }

    @Override
    public Contact parseContact(ServerResponse serverResponse) throws Exception {

        if (serverResponse instanceof ObjectServerResponse) {

            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;
            return parser.parseContact(cplx.response);

        } else {
            throw new Exception("ServerResponse must by an ObjectServerResponse");
        }
    }

    @Override
    public Group parseGroup(ServerResponse serverResponse) throws Exception {
        if (serverResponse instanceof ObjectServerResponse) {
            return parser.parseGroup(((ObjectServerResponse) serverResponse).response);
        } else {
            throw new Exception("ServerResponse must by an ObjectServerResponse");
        }
    }

    @Override
    public User parseUser(ServerResponse serverResponse) throws Exception {

        if (serverResponse instanceof ObjectServerResponse) {

            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;

            if (cplx.response.has("user")) {
                return parser.parseUser(cplx.response.optJSONObject("user"));
            } else {
                return null;
            }
        } else {
            throw new Exception("ServerResponse must by an ObjectServerResponse");
        }
    }


    @Override
    public Contact parseUserAsContact(ServerResponse serverResponse) throws Exception {

        if (serverResponse instanceof ObjectServerResponse) {

            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;

            if (cplx.response.has("user")) {
                return parser.parseContact(cplx.response.optJSONObject("user"));
            } else {
                return null;
            }
        } else {
            throw new Exception("ServerResponse must by an ObjectServerResponse");
        }
    }

    @Override
    public List<Contact> parseContacts(ServerResponse serverResponse) throws Exception {

        if (!(serverResponse instanceof ArrayServerResponse)) {
            throw new Exception("ServerResponse must be an ArrayServerResponse");
        }

        List<Contact> contacts = new ArrayList<Contact>();

        ArrayServerResponse array = (ArrayServerResponse) serverResponse;

        for (int i = 0; i < array.response.length(); i++) {
            contacts.add(parser.parseContact(array.response.getJSONObject(i)));
        }

        return contacts;
    }

    @Override
    public Conversation parseConversation(ServerResponse serverResponse) throws Exception {

        if (serverResponse instanceof ObjectServerResponse) {

            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;
            return parser.parseConversation(cplx.response);

        } else {
            throw new Exception("ServerResponse must by an ObjectServerResponse");
        }
    }

    @Override
    public List<Conversation> parseConversations(ServerResponse serverResponse) throws Exception {

        if (!(serverResponse instanceof ArrayServerResponse)) {
            throw new Exception("ServerResponse must be an ArrayServerResponse");
        }

        List<Conversation> conversations = new ArrayList<Conversation>();

        ArrayServerResponse array = (ArrayServerResponse) serverResponse;

        for (int i = 0; i < array.response.length(); i++) {
            conversations.add(parser.parseConversation(array.response.getJSONObject(i)));
        }

        return conversations;
    }

    @Override
    public DeviceToken parseDeviceToken(ServerResponse serverResponse) throws Exception {

        DeviceToken result = null;

        if (serverResponse instanceof ObjectServerResponse) {
            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;
            result = new DeviceToken();
            result.setDevice(new Device());

            LOGGER.debug(cplx.response.toString());

            if (cplx.response.has("device")) {
                result.getDevice().setAddress(cplx.response.getJSONObject("device").getString("address"));
                result.getDevice().setDeviceNumber(cplx.response.getJSONObject("device").getInt("deviceId"));
                result.getDevice().setId(cplx.response.getJSONObject("device").getLong("id"));
            } else {
                result.getDevice().setAddress(cplx.response.getString("address"));
                result.getDevice().setId(cplx.response.getLong("deviceId"));
                result.getDevice().setDeviceNumber(cplx.response.getInt("deviceNumber"));
            }

            result.setApiKey(cplx.response.getString("apiKey"));
            result.setSecret(cplx.response.getString("secret"));
            result.setSessionKey(cplx.response.getString("sessionKey"));
        }
        return result;
    }

    @Override
    public List<Presence> parsePresence(ServerResponse serverResponse) throws Exception {
        JSONObject raw = new JSONObject(serverResponse.getRaw());
        JSONObject response = raw.optJSONObject("response");
        JSONArray result = response.getJSONArray("result");

        if (result.length() > 1) {
            throw new Exception(String.format("More than one result array for this presence category. %s", result));
        }

        // TODO:
//        return PresenceUtil.getInstance().parse(result.optJSONObject(0).optJSONArray("presenceList"));
        return null;
    }

    @Override
    public List<MessageAttachment> parseAttachments(ServerResponse serverResponse) throws Exception {

        if (!(serverResponse instanceof ArrayServerResponse)) {
            throw new Exception("ServerResponse must be an ArrayServerResponse");
        }

        List<MessageAttachment> attachments = new ArrayList<MessageAttachment>();

        ArrayServerResponse array = (ArrayServerResponse) serverResponse;

        for (int i = 0; i < array.response.length(); i++) {
            attachments.add(parser.parseMessageAttachment(array.response.getJSONObject(i)));
        }

        return attachments;
    }

    @Override
    public EnrollmentResult parseEnrollmentResult(ServerResponse serverResponse) throws Exception {

        EnrollmentResult result = null;

        if (serverResponse instanceof ObjectServerResponse) {

            ObjectServerResponse objectServerResponse = (ObjectServerResponse) serverResponse;
            result = new EnrollmentResult();

            result.setCarbonEnabled(objectServerResponse.response.optBoolean("carbonEnabled"));
            result.setCarbonInstalled(objectServerResponse.response.optBoolean("carbonInstalled"));
            result.setDeviceNumber(objectServerResponse.response.optInt("deviceNumber"));
        }

        return result;
    }

    @Override
    public String parseFaceName(ServerResponse serverResponse) throws Exception {

        String name = StringUtil.EMPTY_STRING;

        if (serverResponse instanceof ObjectServerResponse) {
            ObjectServerResponse objectServerResponse = (ObjectServerResponse) serverResponse;
            name = objectServerResponse.response.optString("fullName");
        }

        return name;
    }

    @Override
    public Map<String, String> parseFaceNames(final ServerResponse serverResponse) throws Exception {
        if (serverResponse == null) return null;

        final JSONObject response = new JSONObject(serverResponse.getRaw());
        final Map<String, String> faceNameMap = new HashMap<String, String>(response.length());
        final Iterator itr = response.keys();
        String key = null;
        String value = null;

        while (itr.hasNext()) {
            key = (String) itr.next();
            value = response.optString(key);

            if (!StringUtil.isNullOrEmpty(value)) {
                faceNameMap.put(key, new JSONObject(value).optString("fullName"));
            }
        }

        return faceNameMap;
    }

    @Override
    public Map<String, Boolean> parseFaceImages(ServerResponse serverResponse) throws Exception {
        if (serverResponse == null) return null;

        final JSONObject response = new JSONObject(serverResponse.getRaw());
        final Map<String, Boolean> faceImageMap = new HashMap<String, Boolean>(response.length());
        final Iterator itr = response.keys();
        String key = null;
        boolean value;

        while (itr.hasNext()) {
            key = (String) itr.next();
            value = response.optBoolean(key);

            faceImageMap.put(key, value);
        }

        return faceImageMap;
    }

    @Override
    public Map<String, String> parseHostedContentSave(ServerResponse serverResponse) throws Exception {

        Map<String, String> result = new HashMap<String, String>();

        if (serverResponse instanceof ObjectServerResponse) {

            ObjectServerResponse objectServerResponse = (ObjectServerResponse) serverResponse;

            Iterator<?> iterator = objectServerResponse.response.keys();

            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                result.put(key, objectServerResponse.response.optString(key));
            }
        }

        return result;
    }

    @Override
    public TinyUrl parseTinyUrl(ServerResponse serverResponse) throws Exception {

        JSONObject jsonObject = new JSONObject(serverResponse.getRaw());

        TinyUrl result = new TinyUrl();
        result.setKey(jsonObject.optString("key"));
        result.setUrl(jsonObject.optString("url"));

        return result;
    }

}
