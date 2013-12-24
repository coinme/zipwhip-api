package com.zipwhip.api.signals;

import com.google.gson.Gson;
import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.api.signals.dto.json.SignalProviderGsonBuilder;
import com.zipwhip.signals2.SignalContentConverter;
import com.zipwhip.signals2.SignalMessage;
import com.zipwhip.signals2.address.ClientAddress;
import com.zipwhip.signals2.message.DefaultMessage;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Date: 9/10/13
 * Time: 10:56 AM
 *
 * @author Michael
 * @version 1
 */
public class GsonParserTest {

    private Gson gson = SignalProviderGsonBuilder.getInstance();

    @Test
    public void testDevice() throws Exception {

    }

    @Test
    public void testConversation() throws Exception {

    }

    @Test
    public void testMessage() throws Exception {
        SignalMessage signal = new SignalMessage();

        signal.setBody("body");
        signal.setDateRead(435345245L);
        signal.setAdvertisement("advertisement");
        signal.setTransmissionState("DELIVERED");
//        signal.setContactId(4345345L);
        signal.setMessageType("MO");
        signal.setAddress("ptn:/234234");
        signal.setFingerprint("fingerprint");
        signal.setDeviceId(342343L);
        signal.setFromName("fromName");
        signal.setHasAttachments(true);
        signal.setDateCreated(453453453L);
        signal.setScheduledDate(null);
        signal.setContactDeviceId(3424324L);

        Map<String, Object> msg = new HashMap<String, Object>();

        msg.put("content", SignalContentConverter.toMap(signal));
        msg.put("address", new ClientAddress("clientId"));
        msg.put("timestamp", System.currentTimeMillis());
        msg.put("id", "34234234");
        msg.put("type", "message");
        msg.put("event", "receive");

        Map<String, Object> deliveredMessageMap = new HashMap<String, Object>();

        deliveredMessageMap.put("message", msg);

        runJsonTestOnDeliveredMessageMap(deliveredMessageMap);
    }

    private void runJsonTestOnDeliveredMessageMap(Map<String, Object> deliveredMessageMap) {
        Map<String, Object> messageMap = (Map<String, Object>) deliveredMessageMap.get("message");

        // convert into json for testing.
        String json = gson.toJson(deliveredMessageMap);

        // we have to specifically add the address field since gson isn't doing it right.
        DeliveredMessage message = gson.fromJson(json, DeliveredMessage.class);

        assertEquals(messageMap.get("timestamp"), message.getTimestamp());
        assertEquals(messageMap.get("event"), message.getEvent());
        assertEquals(messageMap.get("type"), message.getType());
        assertEquals(messageMap.get("id"), message.getId());

        SignalMessage messageContent = (SignalMessage)message.getContent();

        // this will pass when Russ finishes his part
        assertNotNull(messageContent);

        Object signal = messageMap.get("content");

        // leverage existing .equals() method
        assertEquals(signal, messageContent);
    }

    @Test
    public void testContact() throws Exception {
        String json = "";

        DefaultMessage defaultMessage = new DefaultMessage();
        defaultMessage.setTimestamp(23423423L);
        defaultMessage.setAddress(new ClientAddress("clientId"));
        defaultMessage.setEvent("event");
        defaultMessage.setType("type");
        defaultMessage.setId("id");


        DeliveredMessage message = new DeliveredMessage();
        message.setSubscriptionIds(new TreeSet<String>(Arrays.asList("subscriptionId1")));

        gson.fromJson(json, DeliveredMessage.class);

        fail("This should be improved");
    }
}
