package com.zipwhip.api.response;

import com.zipwhip.api.dto.*;
import com.zipwhip.util.JsonDateUtil;
import com.zipwhip.util.Parser;
import com.zipwhip.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: 7/6/11
 * Time: 3:04 PM
 * <p/>
 * Centralizes the act of parsing DTO's from JSON.
 */
public class JsonDtoParser {
    private static final long DEFAULT_LONG = 0l;
    private static final int DEFAULT_INT = 0;

    public final Parser<JSONObject, Message> MESSAGE_PARSER = new Parser<JSONObject, Message>() {
        @Override
        public Message parse(JSONObject jsonObject) throws Exception {
            return parseMessage(jsonObject);
        }
    };

    public final Parser<JSONObject, Contact> CONTACT_PARSER = new Parser<JSONObject, Contact>() {
        @Override
        public Contact parse(JSONObject jsonObject) throws Exception {
            return parseContact(jsonObject);
        }
    };

    public final Parser<JSONObject, Conversation> CONVERSATION_PARSER = new Parser<JSONObject, Conversation>() {
        @Override
        public Conversation parse(JSONObject jsonObject) throws Exception {
            return parseConversation(jsonObject);
        }
    };

    public final Parser<JSONObject, CarbonEvent> CARBON_PARSER = new Parser<JSONObject, CarbonEvent>() {
        @Override
        public CarbonEvent parse(JSONObject jsonObject) throws Exception {
            return parseCarbonEvent(jsonObject);
        }
    };

    public final Parser<JSONObject, Device> DEVICE_PARSER = new Parser<JSONObject, Device>() {
        @Override
        public Device parse(JSONObject jsonObject) throws Exception {
            return parseDevice(jsonObject);
        }
    };

    public final Parser<JSONObject, User> USER_PARSER = new Parser<JSONObject, User>() {
        @Override
        public User parse(JSONObject jsonObject) throws Exception {
            return parseUser(jsonObject);
        }
    };

    /**
     * Parse a Contact from a JSONObject if the object contains it.
     *
     * @param content JSONObject to be parsed.
     * @return A Contact object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public Contact parseContact(JSONObject content) throws JSONException {

        if (content == null) {
            return null;
        }

        Contact contact = parseBasicDto(new Contact(), content);

        contact.setDeviceId(parseLong(content, "deviceId"));
        contact.setAddress(parseString(content, "address"));
        contact.setMobileNumber(parseString(content, "mobileNumber"));
        contact.setState(parseString(content, "state"));
        contact.setCity(parseString(content, "city"));
        contact.setId(parseLong(content, "id"));
        contact.setPhoneKey(parseString(content, "phoneKey"));
        contact.setThread(parseString(content, "thread"));
        contact.setFwd(parseString(content, "fwd"));
        contact.setCarrier(parseString(content, "carrier"));
        contact.setFirstName(parseString(content, "firstName"));
        contact.setLastName(parseString(content, "lastName"));
        contact.setMoCount(content.optInt("MOCount"));
        contact.setZoCount(content.optInt("ZOCount"));
        contact.setZipcode(parseString(content, "zipcode"));
        contact.setLatlong(parseString(content, "latlong"));
        contact.setEmail(parseString(content, "email"));
        contact.setNotes(parseString(content, "notes"));
        contact.setChannel(parseString(content, "channel"));
        contact.setLoc(parseString(content, "loc"));
        contact.setDeleted(content.optBoolean("deleted"));

        return contact;
    }

    /**
     * Parse a Group from a JSONObject if the object contains it.
     *
     * @param content JSONObject to be parsed.
     * @return A Group object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public Group parseGroup(JSONObject content) throws JSONException {
        if (content == null) {
            return null;
        }

        final JSONObject jsonGroup = content.optJSONObject("group");
        if (jsonGroup == null) {
            return null;
        }

        final Group group = parseBasicDto(new Group(), content);

        group.setAddress(jsonGroup.optString("address"));
        group.setCachedContactsCount(parseInt(jsonGroup, "cachedContactsCount"));
        group.setChannel(jsonGroup.optString("channel"));
        group.setDeviceId(parseLong(jsonGroup, "deviceId"));
        group.setDisplayName(jsonGroup.optString("displayName"));
        group.setDtoParentId(parseLong(jsonGroup, "dtoParentId"));
        group.setId(parseLong(jsonGroup, "id"));
        group.setLinkedDeviceId(parseLong(jsonGroup, "linkedDeviceId"));
        group.setNewGroup(jsonGroup.optBoolean("new", false));
        group.setPhoneKey(jsonGroup.optString("phoneKey"));
        group.setTextline(jsonGroup.optString("textline"));
        group.setThread(jsonGroup.optString("thread"));
        group.setType(jsonGroup.optString("type"));
        group.setUserId(parseLong(jsonGroup, "userId"));
        group.setUuid(jsonGroup.optString("uuid"));

        return group;
    }

    /**
     * Parse a Contact from a JSONObject if the object contains it.
     *
     * @param content JSONObject to be parsed.
     * @return A Contact object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public User parseUser(JSONObject content) throws JSONException {

        if (content == null) {
            return null;
        }

        User user = parseBasicDto(new User(), content);

        user.setMobileNumber(parseString(content, "mobileNumber"));
        user.setPhoneKey(parseString(content, "phoneKey"));
        user.setCarrier(parseString(content, "carrier"));
        user.setFirstName(parseString(content, "firstName"));
        user.setLastName(parseString(content, "lastName"));
        user.setMoCount(content.optInt("MOCount"));
        user.setZoCount(content.optInt("ZOCount"));
        user.setZipcode(parseString(content, "zipcode"));
        user.setEmail(parseString(content, "email"));
        user.setNotes(parseString(content, "notes"));
        user.setLoc(parseString(content, "loc"));
        user.setWebsiteDeviceId(parseLong(content, "websiteDeviceId"));

        return user;
    }

    /**
     * Parse a Message from a JSONObject if the object contains it.
     *
     * @param response JSONObject to be parsed.
     * @return A Message object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public Message parseMessage(JSONObject response) throws JSONException {

        if (response == null) {
            return null;
        }

        Message message = new Message();

        JSONObject transmissionStateJson = response.optJSONObject("transmissionState");
        if (transmissionStateJson != null) {
            message.setTransmissionState(TransmissionState.parse(transmissionStateJson.optString("name")));
        }

        message.setId(parseLong(response, "id"));
        message.setUuid(parseString(response, "uuid"));
        message.setDeviceId(parseLong(response, "deviceId"));
        message.setContactId(parseLong(response, "contactId"));
        message.setContactDeviceId(parseLong(response, "contactDeviceId"));
        message.setAddress(parseString(response, "address"));
        message.setRead(response.optBoolean("isRead"));
        message.setDeleted(response.optBoolean("deleted"));
        message.setFingerprint(parseString(response, "fingerprint"));
        message.setCc(parseString(response, "cc"));
        message.setBcc(parseString(response, "bcc"));
        message.setErrorState(response.optBoolean("errorState"));
        message.setBody(parseString(response, "body"));
        message.setDateCreated(parseDate(response, "dateCreated"));
        message.setLastUpdated(parseDate(response, "lastUpdated"));
        message.setSourceAddress(parseString(response, "sourceAddress"));
        message.setDestinationAddress(parseString(response, "destAddress"));
        message.setStatusCode(response.optInt("statusCode"));
        message.setStatusDesc(parseString(response, "statusDesc"));
        message.setThread(parseString(response, "thread"));
        message.setChannel(parseString(response, "channel"));
        message.setFwd(parseString(response, "fwd"));
        message.setCarrier(parseString(response, "carrier"));
        message.setSubject(parseString(response, "subject"));
        message.setTo(parseString(response, "to"));
        message.setMobileNumber(parseString(response, "mobileNumber"));
        message.setFirstName(parseString(response, "firstName"));
        message.setLastName(parseString(response, "lastName"));
        message.setVersion(parseLong(response, "version"));
        message.setMessageType(parseString(response, "type"));
        message.setAdvertisement(parseString(response, "advertisement"));
        message.setHasAttachment(response.optBoolean("hasAttachment"));

        return message;
    }

    /**
     * Parse a List<MessageToken> from a JSONObject if the object contains it.
     *
     * @param array JSONObject to be parsed.
     * @return A CList<MessageToken>ontact object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public List<MessageToken> parseMessageTokens(JSONObject baseResponse, JSONArray array) throws JSONException {

        List<MessageToken> result = new ArrayList<MessageToken>();
        long rootMessageID = Long.valueOf(baseResponse.optString("root"));
        int len = array.length();
        for (int i = 0; i < len; i++) {

            JSONObject json = array.getJSONObject(i);
            MessageToken token = new MessageToken();

            token.setMessageId(Long.valueOf(json.optString("message")));
            token.setDeviceId(parseLong(json, "device")); // will be 0 if it is a self message
            token.setContactId(parseLong(json, "contact")); // will be 0 if it is a self message
            token.setFingerprint(json.optString("fingerprint"));
            token.setRootMessageId(rootMessageID);

            result.add(token);
        }

        return result;
    }

    /**
     * Parse a Conversation from a JSONObject if the object contains it.
     *
     * @param content JSONObject to be parsed.
     * @return A Conversation object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public Conversation parseConversation(JSONObject content) throws JSONException {

        if (content == null) {
            return null;
        }

        Conversation conversation = new Conversation();

        conversation.setId(parseLong(content, "id"));
        conversation.setDeviceId(parseLong(content, "deviceId"));
        conversation.setDeviceAddress(parseString(content, "deviceAddress"));
        conversation.setFingerprint(parseString(content, "fingerprint"));
        conversation.setAddress(parseString(content, "address"));
        conversation.setCc(parseString(content, "cc"));
        conversation.setBcc(parseString(content, "bcc"));
        conversation.setUnreadCount(content.optInt("unreadCount"));
        conversation.setLastContactId(parseLong(content, "lastContactId"));
        conversation.setNew(content.optBoolean("new"));
        conversation.setDeleted(content.optBoolean("deleted"));
        conversation.setVersion(content.optInt("version"));
        conversation.setLastContactDeviceId(parseLong(content, "lastContactDeviceId"));
        conversation.setLastMessageBody(parseString(content, "lastMessageBody"));
        conversation.setLastContactFirstName(parseString(content, "lastContactFirstName"));
        conversation.setLastContactLastName(parseString(content, "lastContactLastName"));
        conversation.setLastContactMobileNumber(parseString(content, "lastContactMobileNumber"));
        conversation.setLastMessageDate(parseDate(content, "lastMessageDate"));
        conversation.setLastNonDeletedMessageDate(parseDate(content, "lastNonDeletedMessageDate"));
        conversation.setDateCreated(parseDate(content, "dateCreated"));
        conversation.setLastUpdated(parseDate(content, "lastUpdated"));

        return conversation;
    }

    /**
     * Parse a MessageAttachment from a JSONObject if the object contains it.
     *
     * @param content JSONObject to be parsed.
     * @return A MessageAttachment object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public MessageAttachment parseMessageAttachment(JSONObject content) throws JSONException {

        if (content == null) {
            return null;
        }

        MessageAttachment attachment = new MessageAttachment();

        attachment.setDateCreated(parseDate(content, "dateCreated"));
        attachment.setDeviceId(parseLong(content, "deviceId"));
        attachment.setId(parseLong(content, "id"));
        attachment.setLastUpdated(parseDate(content, "lastUpdated"));
        attachment.setMessageId(parseLong(content, "messageId"));
        attachment.setMimeType(parseString(content, "mimeType"));
        attachment.setNew(content.optBoolean("new"));
        attachment.setStorageKey(parseString(content, "storageKey"));
        attachment.setVersion(parseLong(content, "version"));

        return attachment;
    }

    /**
     * Parse a Device from a JSONObject if the object contains it.
     *
     * @param response JSONObject to be parsed.
     * @return A Device object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public Device parseDevice(JSONObject response) throws JSONException {

        if (response == null) {
            return null;
        }

        Device device = new Device();

        device.setId(parseLong(response, "id"));
        device.setUuid(parseString(response, "uuid"));
        device.setAddress(parseString(response, "address"));
        device.setThread(parseString(response, "thread"));
        device.setVersion(response.optInt("version"));
        device.setLastUpdated(parseDate(response, "lastUpdated"));
        device.setDateCreated(parseDate(response, "dateCreated"));
        device.setUserId(parseLong(response, "userId"));
        device.setChannel(parseString(response, "channel"));
        device.setTextline(parseString(response, "textline"));
        device.setDisplayName(parseString(response, "displayName"));

        return device;
    }

    public CarbonEvent parseCarbonEvent(JSONObject content) throws JSONException {

        if (content == null) {
            return null;
        }

        CarbonEvent carbonEvent = new CarbonEvent();

        carbonEvent.carbonDescriptor = parseString(content, "carbonDescriptor");

        return carbonEvent;
    }

    private <T extends BasicDto> T parseBasicDto(T dto, JSONObject content) {
        dto.setLastUpdated(parseDate(content, "lastUpdated"));
        dto.setDateCreated(parseDate(content, "dateCreated"));
        dto.setVersion(parseLong(content, "version"));

        return dto;
    }

    private Date parseDate(final JSONObject response, final String key) {
        return JsonDateUtil.getDate(parseString(response, key));
    }

    private String parseString(final JSONObject response, final String key) {
        return (response == null || response.isNull(key)) ? StringUtil.EMPTY_STRING : response.optString(key);
    }

    private long parseLong(final JSONObject response, final String key) {
        return (response == null || response.isNull(key)) ? DEFAULT_LONG : response.optLong(key, DEFAULT_LONG);
    }

    private int parseInt(final JSONObject response, final String key) {
        return (response == null || response.isNull(key)) ? DEFAULT_INT : response.optInt(key, DEFAULT_INT);
    }
}
