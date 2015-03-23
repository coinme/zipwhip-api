package com.zipwhip.api.webhooks;

import com.google.gson.Gson;
import com.zipwhip.api.dto.Message;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.Converter;
import com.zipwhip.util.DataConversionException;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 3/23/2015
 * Time: 3:30 PM
 */
public class WebhookMessageConverter implements Converter<String, Message> {

    @Override
    @SuppressWarnings("unchecked")
    public Message convert(String json) throws DataConversionException {
        Map<String, Object> map = new Gson().fromJson(json, Map.class);

        Message message = new Message();

        if (CollectionUtil.containsAny(map, "id")) {
            message.setId(CollectionUtil.getLong(map, "id"));
        } else if (CollectionUtil.containsAny(map, "_id")) {
            message.setId(CollectionUtil.getLong(map, "_id"));
        }

        if (CollectionUtil.containsAny(map, "version")) {
            message.setVersion(CollectionUtil.getInteger(map, "version"));
        } else if (CollectionUtil.containsAny(map, "_version")) {
            message.setVersion(CollectionUtil.getInteger(map, "_version"));
        }

        message.setDateCreated(CollectionUtil.getDate(map, "dateCreated"));
        message.setLastUpdated(CollectionUtil.getDate(map, "lastUpdated"));

        if (CollectionUtil.containsAny(map, "statusCode")) {
            message.setStatusCode(CollectionUtil.getInteger(map, "statusCode"));
        }

        message.setAddress(CollectionUtil.getString(map, "address"));
        message.setAdvertisement(CollectionUtil.getString(map, "advertisement"));
        message.setBcc(CollectionUtil.getString(map, "bcc"));
        message.setCc(CollectionUtil.getString(map, "cc"));
        message.setBody(CollectionUtil.getString(map, "body"));
        message.setCarrier(CollectionUtil.getString(map, "carrier"));
        message.setChannel(CollectionUtil.getString(map, "channel"));

        if (CollectionUtil.containsAny(map, "contactDeviceId")) {
            message.setContactDeviceId(CollectionUtil.getLong(map, "contactDeviceId"));
        }

        if (CollectionUtil.containsAny(map, "contactId")) {
            message.setContactId(CollectionUtil.getLong(map, "contactId"));
        }

        if (CollectionUtil.containsAny(map, "deviceId")) {
            message.setDeviceId(CollectionUtil.getLong(map, "deviceId"));
        }

        message.setDestinationAddress(CollectionUtil.getString(map, "finalDestination"));
        message.setSourceAddress(CollectionUtil.getString(map, "finalSource"));
        message.setDirection(CollectionUtil.getString(map, "direction"));

        if (CollectionUtil.containsAny(map, "errorDesc")) {
            message.setErrorDesc(CollectionUtil.getString(map, "errorDesc"));
        }

        if (CollectionUtil.containsAny(map, "errorState")) {
            message.setErrorState(CollectionUtil.getBoolean(map, "errorState"));
        }

        message.setFingerprint(CollectionUtil.getString(map, "fingerprint"));
        message.setFirstName(CollectionUtil.getString(map, "firstName"));
        message.setLastName(CollectionUtil.getString(map, "lastName"));
        message.setUuid(CollectionUtil.getString(map, "uuid"));

        if (CollectionUtil.containsAny(map, "read")) {
            message.setRead(CollectionUtil.getBoolean(map, "read"));
        }

        if (CollectionUtil.containsAny(map, "hasAttachment")) {
            message.setHasAttachment(CollectionUtil.getBoolean(map, "hasAttachment"));
        }

        return message;
    }
}
