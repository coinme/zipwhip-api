package com.zipwhip.api.webhooks;

import com.zipwhip.api.dto.Message;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WebhookMessageConverterTest {

    WebhookMessageConverter converter = new WebhookMessageConverter();

    @Test
    public void testConvert() throws Exception {
        String json = "{\"body\":\"I am Groot.\",\"bodySize\":49,\"visible\":true,\"hasAttachment\":false,\"bcc\":null,\"finalDestination\":\"2066181419\",\"messageType\":\"MO\",\"deleted\":false,\"id\":580049996771368960,\"statusCode\":4,\"fingerprint\":\"529740286\",\"messageTransport\":2,\"address\":\"ptn:/6203098565\",\"read\":true,\"dateCreated\":\"2015-03-23T09:52:42-07:00\",\"cc\":null,\"finalSource\":\"6203098565\",\"deviceId\":144052806}";

        Message message = converter.convert(json);

        assertEquals("I am Groot.", message.getBody());
    }
}