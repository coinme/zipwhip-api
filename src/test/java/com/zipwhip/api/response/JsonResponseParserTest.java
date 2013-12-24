package com.zipwhip.api.response;

import com.zipwhip.api.dto.*;
import com.zipwhip.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/14/11
 * Time: 1:37 PM
 */
public class JsonResponseParserTest {

    public JsonResponseParser parser;
    public ServerResponse response;

    public static final String ENROLLMENT_RESULT = "{\"response\":{\"carbonEnabled\":true,\"carbonInstalled\":true,\"deviceNumber\":999},\"sessions\":null,\"success\":true}";
    public static final String ENROLLMENT_RESULT_RESULT = "{\"carbonEnabled\":true,\"carbonInstalled\":true,\"deviceNumber\":999}";
    public final static String ATTACHMENT_RESULT = "{\"success\":true,\"response\":[{\"class\":\"com.zipwhip.website.data.dto.MessageAttachment\",\"dateCreated\":\"2012-04-24T15:42:25-07:00\",\"deviceId\":128918006,\"id\":160557306,\"lastUpdated\":null,\"messageId\":194919298488344576,\"messageType\":{\"enumType\":\"com.zipwhip.website.data.dto.MessageType\",\"name\":\"MO\"},\"new\":false,\"storageKey\":\"\",\"version\":0},{\"class\":\"com.zipwhip.website.data.dto.MessageAttachment\",\"dateCreated\":\"2012-04-24T15:42:25-07:00\",\"deviceId\":128918006,\"id\":160557406,\"lastUpdated\":null,\"messageId\":194919298488344576,\"messageType\":{\"enumType\":\"com.zipwhip.website.data.dto.MessageType\",\"name\":\"MO\"},\"new\":false,\"storageKey\":\"a011eacf-83a5-4b79-8999-81c0858591bd\",\"version\":0}]}";

    @Before
    public void setUp() throws Exception {
        parser = new JsonResponseParser();
    }

    @Test
    public void testParse() throws Exception {
        ServerResponse response = parser.parse(ENROLLMENT_RESULT);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccess());
        Assert.assertTrue(response instanceof ObjectServerResponse);
    }
//
//    @Test
//    public void testParseMessageTokens() throws Exception {
//
//    }
//
//    @Test
//    public void testParseMessage() throws Exception {
//
//    }
//
//    @Test
//    public void testParseString() throws Exception {
//
//    }
//
//    @Test
//    public void testParseContact() throws Exception {
//
//    }
//
//    @Test
//    public void testParseContacts() throws Exception {
//
//    }
//
//    @Test
//    public void testParseDeviceToken() throws Exception {
//
//    }
//
//    @Test
//    public void testParsePresence() throws Exception {
//
//    }

    @Test
    public void testParseEnrollResult() throws Exception {

        JSONObject o = new JSONObject(ENROLLMENT_RESULT_RESULT);
        response = new ObjectServerResponse(ENROLLMENT_RESULT, true, o);


        EnrollmentResult result = parser.parseEnrollmentResult(response);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isCarbonEnabled());
        Assert.assertTrue(result.isCarbonInstalled());
        Assert.assertEquals(result.getDeviceNumber(), 999);
    }

    @Test
    public void testParseMessageAttachment() throws Exception {
        JSONArray o = new JSONObject(ATTACHMENT_RESULT).optJSONArray("response");
        response = new ArrayServerResponse(ENROLLMENT_RESULT, true, o);

        List<MessageAttachment> result = parser.parseAttachments(response);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());

        MessageAttachment dto1 = result.get(0);
        MessageAttachment dto2 = result.get(1);

        Assert.assertNotNull(dto1.getDateCreated());
        Assert.assertEquals(128918006L, dto1.getDeviceId());
        Assert.assertEquals(160557306L, dto1.getId());
        Assert.assertEquals(0L, dto1.getVersion());
        Assert.assertTrue(StringUtil.isNullOrEmpty(dto1.getStorageKey()));
        Assert.assertEquals(194919298488344576L, dto1.getMessageId());

        Assert.assertNotNull(dto2.getDateCreated());
        Assert.assertEquals(128918006L, dto2.getDeviceId());
        Assert.assertEquals(160557406L, dto2.getId());
        Assert.assertEquals(0L, dto2.getVersion());
        Assert.assertEquals("a011eacf-83a5-4b79-8999-81c0858591bd", dto2.getStorageKey());
        Assert.assertEquals(194919298488344576L, dto2.getMessageId());
    }

    /**
     * Taking the response that we would get from a conversation/get call that contains at least one message.
     */
    @Test
    public void testParseMessagesFromConversationForMessages() throws Exception {
        String responseString = "{\"conversation\":{\"address\":\"ptn:/3134147502\",\"bcc\":\"\",\"cc\":\"\",\"class\":\"com.zipwhip.website.data.dto.Conversation\",\"dateCreated\":\"2011-09-20T17:21:12-07:00\",\"deviceAddress\":\"device:/3609900541/0\",\"deviceId\":2252293,\"dtoParentId\":2252293,\"fingerprint\":\"1189375339\",\"id\":195619401,\"lastContactDeviceId\":2252293,\"lastContactFirstName\":\"John\",\"lastContactId\":282195101,\"lastContactLastName\":\"Lauer\",\"lastContactMobileNumber\":\"3134147502\",\"lastMessageBody\":\"Sending you a text from your new tablet app.\\n\\nSent via Zipwhip\",\"lastMessageDate\":\"2012-04-29T15:42:19-07:00\",\"lastNonDeletedMessageDate\":\"2012-04-29T15:42:19-07:00\",\"lastUpdated\":\"2012-04-29T15:42:53-07:00\",\"new\":false,\"unreadCount\":0,\"version\":4},\"messages\":[{\"DCSId\":\"\",\"UDH\":\"\",\"address\":\"ptn:/3134147502\",\"advertisement\":null,\"bcc\":null,\"body\":\"Sending you a text from your new tablet app.\\n\\nSent via Zipwhip\",\"bodySize\":62,\"carbonedMessageId\":-1,\"carrier\":\"Sprint\",\"cc\":null,\"channel\":\"\",\"class\":\"com.zipwhip.website.data.dto.LegacyMessage\",\"contactDeviceId\":2252293,\"contactId\":282195101,\"creatorId\":1400020101,\"dateCreated\":\"2012-04-29T15:42:19-07:00\",\"deleted\":false,\"deliveryReceipt\":null,\"destAddress\":\"3609900541\",\"deviceId\":2252293,\"dishedToOpenMarket\":null,\"dtoParentId\":2252293,\"encoded\":false,\"errorState\":false,\"expectDeliveryReceipt\":false,\"fingerprint\":\"1189375339\",\"firstName\":\"John\",\"fromName\":null,\"fwd\":\"\",\"hasAttachment\":false,\"id\":\"196731185526153216\",\"isInFinalState\":false,\"isParent\":false,\"isRead\":true,\"isSelf\":false,\"lastName\":\"Lauer\",\"lastUpdated\":null,\"latlong\":\"\",\"loc\":\"\",\"messageConsoleLog\":\"\",\"metaDataId\":0,\"mobileNumber\":\"3134147502\",\"new\":false,\"openMarketMessageId\":\"\",\"parentId\":0,\"phoneKey\":\"\",\"scheduledDate\":null,\"smartForwarded\":false,\"smartForwardingCandidate\":false,\"sourceAddress\":\"3134147502\",\"statusCode\":4,\"statusDesc\":null,\"subject\":\"\",\"thread\":\"\",\"to\":null,\"transferedToCarrierReceipt\":null,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"DELIVERED\"},\"type\":\"MO\",\"uuid\":\"196731185526153216\",\"version\":0,\"visible\":true}]}";
        JSONObject o = new JSONObject(responseString);
        response = new ObjectServerResponse(responseString, true, o);

        List<Message> messages = parser.parseMessagesFromConversation(response);
        Assert.assertNotNull(messages);
        Assert.assertEquals(1, messages.size());
        
        //Look at a few values in the message object, to confirm that they're valid.
        Message m = messages.get(0);
        Assert.assertEquals(196731185526153216l, m.getId());
        Assert.assertEquals(2252293l, m.getDeviceId());
        Assert.assertNull(m.getDirection());
        Assert.assertTrue(m.isRead());
        Assert.assertFalse(m.isDeleted());
        Assert.assertEquals("1189375339", m.getFingerprint());
        Assert.assertEquals("Sprint", m.getCarrier());
        Assert.assertEquals("null", m.getCc());
        Assert.assertNull(m.getLastUpdated());
        Assert.assertFalse(m.isHasAttachment());
    }

    /**
     * Taking the response that we would get from a conversation/get call that contains no messages.
     */
    @Test
    public void testParseMessagesFromConversationForNoMessages() throws Exception {
        String responseString = "{\"conversation\":{\"address\":\"device:/2066319248/3\",\"bcc\":null,\"cc\":null,\"class\":\"com.zipwhip.website.data.dto.Conversation\",\"dateCreated\":\"2012-04-21T03:09:37-07:00\",\"deviceAddress\":null,\"deviceId\":2252293,\"dtoParentId\":2252293,\"fingerprint\":\"3122005387\",\"id\":1995255301,\"lastContactDeviceId\":2252293,\"lastContactFirstName\":\"\",\"lastContactId\":531279101,\"lastContactLastName\":\"\",\"lastContactMobileNumber\":\"device:/2066319248/3\",\"lastMessageBody\":\"You've .JOINed text group \\\"Synch Haigh\\\" with 3 members started by          206-631-9248.\\n\\nReply\\n.stop to stop\\n.help for info\\n.list for members\",\"lastMessageDate\":\"2012-04-21T03:33:53-07:00\",\"lastNonDeletedMessageDate\":\"2012-04-21T03:33:53-07:00\",\"lastUpdated\":\"2012-04-23T10:18:54-07:00\",\"new\":false,\"unreadCount\":0,\"version\":2},\"messages\":null}";
        JSONObject o = new JSONObject(responseString);
        response = new ObjectServerResponse(responseString, true, o);

        List<Message> messages = parser.parseMessagesFromConversation(response);
        Assert.assertNotNull(messages);
        Assert.assertEquals(0, messages.size());
    }

    /**
     * Test-parsing the response that we would get from a conversation/list call that had one more conversations.
     */
    @Test
    public void testConversationListResponseWithContents() throws Exception {
        String responseString = "[{\"MOCount\":0,\"ZOCount\":0,\"address\":\"ptn:/2\",\"birthday\":null,\"carrier\":\"Unknown\",\"channel\":\"\",\"city\":\"\",\"class\":\"com.zipwhip.website.data.dto.Contact\",\"dateCreated\":\"2012-04-02T10:10:45-07:00\",\"deleted\":false,\"deviceId\":2252293,\"dtoParentId\":2252293,\"email\":\"aaaaa@aaaaaq.com\",\"firstName\":\"Test\",\"fwd\":\"\",\"id\":513916101,\"isZwUser\":false,\"keywords\":\"\",\"lastName\":\"Contact\",\"lastUpdated\":\"2012-04-02T10:10:46-07:00\",\"latlong\":\"\",\"loc\":\"\",\"mobileNumber\":\"2\",\"new\":false,\"notes\":\"\",\"phoneId\":0,\"phoneKey\":\"\",\"state\":\"\",\"targetGroupDevice\":-1,\"thread\":\"\",\"vector\":\"\",\"version\":2,\"zipcode\":\"\"},{\"MOCount\":0,\"ZOCount\":0,\"address\":\"ptn:/20000\",\"birthday\":null,\"carrier\":\"Unknown\",\"channel\":\"\",\"city\":\"\",\"class\":\"com.zipwhip.website.data.dto.Contact\",\"dateCreated\":\"2011-10-19T11:55:52-07:00\",\"deleted\":false,\"deviceId\":2252293,\"dtoParentId\":2252293,\"email\":\"\",\"firstName\":\"\",\"fwd\":\"\",\"id\":327097301,\"isZwUser\":false,\"keywords\":\"\",\"lastName\":\"\",\"lastUpdated\":\"2012-04-12T16:35:28-07:00\",\"latlong\":\"\",\"loc\":\"\",\"mobileNumber\":\"20000\",\"new\":false,\"notes\":\"\",\"phoneId\":0,\"phoneKey\":\"lgMuziqPink\",\"state\":\"\",\"targetGroupDevice\":-1,\"thread\":\"\",\"vector\":\"\",\"version\":6,\"zipcode\":\"\"},{\"MOCount\":0,\"ZOCount\":0,\"address\":\"ptn:/20000001\",\"birthday\":null,\"carrier\":\"Unknown\",\"channel\":\"\",\"city\":\"\",\"class\":\"com.zipwhip.website.data.dto.Contact\",\"dateCreated\":\"2011-12-20T14:34:19-08:00\",\"deleted\":false,\"deviceId\":2252293,\"dtoParentId\":2252293,\"email\":\"\",\"firstName\":\"\",\"fwd\":\"\",\"id\":408203001,\"isZwUser\":false,\"keywords\":\"\",\"lastName\":\"\",\"lastUpdated\":\"2012-04-10T11:51:16-07:00\",\"latlong\":\"\",\"loc\":\"\",\"mobileNumber\":\"20000001\",\"new\":false,\"notes\":\"\",\"phoneId\":0,\"phoneKey\":\"\",\"state\":\"\",\"targetGroupDevice\":-1,\"thread\":\"\",\"vector\":\"\",\"version\":2,\"zipcode\":\"\"}]";
        JSONArray o = new JSONArray(responseString);
        response = new ArrayServerResponse(responseString, true, o);

        List<Conversation> conversations = parser.parseConversations(response);

        //Look at the contents of the covnersation list.
        Assert.assertNotNull(conversations);
        Assert.assertEquals(3, conversations.size());

        //Look at the contents of the first value in the conversation.
        Conversation c = conversations.get(0);
        Assert.assertEquals(513916101l, c.getId());
        Assert.assertEquals(0, c.getUnreadCount());
        Assert.assertFalse(c.isDeleted());
        Assert.assertEquals(2l, c.getVersion());
        Assert.assertEquals("ptn:/2", c.getAddress());
        Assert.assertEquals("", c.getFingerprint());
        Assert.assertEquals(0l, c.getLastContactDeviceId());
        Assert.assertEquals(2252293l, c.getDeviceId());
    }

    /**
     * Test-parsing the response that we would get from a conversation/list call that did not have any conversations in it.
     */
    @Test
    public void testConversationListResponseWithoutContents() throws Exception {
        String responseString = "[ ]";
        JSONArray o = new JSONArray(responseString);
        response = new ArrayServerResponse(responseString, true, o);
        
        List<Conversation> conversations = parser.parseConversations(response);
        
        Assert.assertNotNull(conversations);
        Assert.assertEquals(0, conversations.size());
    }

    /**
     * Tests parsing a list of contacts that contains at least one contact.
     */
    @Test
    public void testContactListResponseWithContents() throws Exception {
        String responseString = "[{\"address\":\"device:/4252466220/25\",\"bcc\":\"\",\"cc\":\"\",\"class\":\"com.zipwhip.website.data.dto.Conversation\",\"dateCreated\":\"2012-04-19T16:21:53-07:00\",\"deviceAddress\":null,\"deviceId\":2252293,\"dtoParentId\":2252293,\"fingerprint\":\"107924374\",\"id\":1964179401,\"lastContactDeviceId\":2252293,\"lastContactFirstName\":\"\",\"lastContactId\":530094101,\"lastContactLastName\":\"\",\"lastContactMobileNumber\":\"device:/4252466220/25\",\"lastMessageBody\":\"Gjfhyyy\",\"lastMessageDate\":\"2012-04-19T16:21:45-07:00\",\"lastNonDeletedMessageDate\":\"2012-04-19T16:21:45-07:00\",\"lastUpdated\":\"2012-04-19T16:21:53-07:00\",\"new\":false,\"unreadCount\":0,\"version\":1},{\"address\":\"ptn:/20000094\",\"bcc\":\"\",\"cc\":\"\",\"class\":\"com.zipwhip.website.data.dto.Conversation\",\"dateCreated\":\"2011-08-16T13:44:56-07:00\",\"deviceAddress\":\"device:/3609900541/0\",\"deviceId\":2252293,\"dtoParentId\":2252293,\"fingerprint\":\"1102725892\",\"id\":144798901,\"lastContactDeviceId\":0,\"lastContactFirstName\":\"\",\"lastContactId\":232190801,\"lastContactLastName\":\"\",\"lastContactMobileNumber\":\"20000094\",\"lastMessageBody\":\"aaaaa: Hhhh\\n\\nCC: Craig, Add\\n(Fhdt)\",\"lastMessageDate\":\"2011-08-16T13:59:42-07:00\",\"lastNonDeletedMessageDate\":\"2011-08-16T13:59:42-07:00\",\"lastUpdated\":\"2012-02-23T12:00:52-08:00\",\"new\":false,\"unreadCount\":0,\"version\":5}]";
        JSONArray o = new JSONArray(responseString);
        response = new ArrayServerResponse(responseString, true, o);

        List<Contact> contacts = parser.parseContacts(response);

        Assert.assertNotNull(contacts);
        Assert.assertEquals(2, contacts.size());
        
        Contact c = contacts.get(0);

        Assert.assertEquals(1964179401l, c.getId());
        Assert.assertEquals(2252293l, c.getDeviceId());
        Assert.assertEquals(0l, c.getMoCount());
        Assert.assertEquals("", c.getEmail());
        Assert.assertEquals(1l, c.getVersion());
        Assert.assertEquals("device:/4252466220/25", c.getAddress());
    }

    /**
     * Tests parsing a list of contacts that does not contain any contents.
     */
    @Test
    public void testContactListResponseWithoutContents() throws Exception {
        String responseString = "[ ]";
        JSONArray o = new JSONArray(responseString);
        response = new ArrayServerResponse(responseString, true, o);

        List<Contact> contacts = parser.parseContacts(response);

        Assert.assertNotNull(contacts);
        Assert.assertEquals(0, contacts.size());
    }
    
    @Test
    public void testUserGetResponseParsingAsUser() throws Exception {
        String responseString = "{\"user\": {\"firstName\": \"Craig\",\"lastName\": \"Erickson\",\"mobileNumber\": \"3609900541\",\"carrier\": \"Tmo\",\"fullName\": \"Craig Erickson\",\"phoneKey\": \"PHONE KEY???\",\"email\": \"craigerick@gmail.com\",\"zipcode\": \"\",\"birthday\": \"\",\"loc\": \"Seattle,WA\",\"notes\": \"First test of the revised user save functionality.\",\"websiteDeviceKey\": \"89cbcf8f-7a22-42ec-86e0-27300ff24774\",\"websiteDeviceId\": 2252293,\"MOCount\": 121,\"ZOCount\": 0},\"settings\": {\"smartForwarding_enabled\": false,\"smartForwarding_timeout\": 0,\"smartForwarding_instant\": false,\"corkboard_send\": false,\"corkboard_receive\": true,\"riser_enabled\": true,\"riser_volume\": 50,\"sendMessage_keepOpen\": false,\"sendMessage_ding\": true,\"mobilePhone_share\": true,\"login_persist\": true,\"login_timeout\": 30,\"contacts_connected\": true,\"contacts_autoAdd\": true,\"signature_mode\": \"firstinitial\",\"signature_custom\": \"\",\"textSuggest_enabled\": true, \"sendMessage_characterCounterAlert\": true, \"wizard_seen\": true},\"groups\": [ ]}";
        JSONObject o = new JSONObject(responseString);
        response = new ObjectServerResponse(responseString, true, o);
        
        User user = parser.parseUser(response);

        Assert.assertNotNull(user);
        Assert.assertEquals("3609900541", user.getMobileNumber());
        Assert.assertEquals("Craig", user.getFirstName());
        Assert.assertEquals("Erickson", user.getLastName());
        Assert.assertNull(user.getDateCreated());
        Assert.assertEquals("Tmo", user.getCarrier());
        Assert.assertEquals(121l, user.getMoCount());
        Assert.assertEquals("", user.getZipcode());
        Assert.assertEquals(2252293l, user.getWebsiteDeviceId());
        Assert.assertEquals("Seattle,WA", user.getLoc());
        Assert.assertEquals(0l, user.getVersion());

    }

    @Test
    public void testUserGetResponseParsingAsContact() throws Exception {
        String responseString = "{\"user\": {\"firstName\": \"Craig\",\"lastName\": \"Erickson\",\"mobileNumber\": \"3609900541\",\"carrier\": \"Tmo\",\"fullName\": \"Craig Erickson\",\"phoneKey\": \"PHONE KEY???\",\"email\": \"craigerick@gmail.com\",\"zipcode\": \"\",\"birthday\": \"\",\"loc\": \"Seattle,WA\",\"notes\": \"First test of the revised user save functionality.\",\"websiteDeviceKey\": \"89cbcf8f-7a22-42ec-86e0-27300ff24774\",\"websiteDeviceId\": 2252293,\"MOCount\": 121,\"ZOCount\": 0},\"settings\": {\"smartForwarding_enabled\": false,\"smartForwarding_timeout\": 0,\"smartForwarding_instant\": false,\"corkboard_send\": false,\"corkboard_receive\": true,\"riser_enabled\": true,\"riser_volume\": 50,\"sendMessage_keepOpen\": false,\"sendMessage_ding\": true,\"mobilePhone_share\": true,\"login_persist\": true,\"login_timeout\": 30,\"contacts_connected\": true,\"contacts_autoAdd\": true,\"signature_mode\": \"firstinitial\",\"signature_custom\": \"\",\"textSuggest_enabled\": true, \"sendMessage_characterCounterAlert\": true, \"wizard_seen\": true},\"groups\": [ ]}";
        JSONObject o = new JSONObject(responseString);
        response = new ObjectServerResponse(responseString, true, o);

        Contact user = parser.parseUserAsContact(response);

        Assert.assertNotNull(user);
        Assert.assertEquals("3609900541", user.getMobileNumber());
        Assert.assertEquals("Craig", user.getFirstName());
        Assert.assertEquals("Erickson", user.getLastName());
        Assert.assertNull(user.getDateCreated());
        Assert.assertEquals("Tmo", user.getCarrier());
        Assert.assertEquals(121l, user.getMoCount());
        Assert.assertEquals("", user.getZipcode());
        Assert.assertEquals("Seattle,WA", user.getLoc());
        Assert.assertEquals(0l, user.getVersion());
    }
    
    @Test
    public void testFaceEcosystemNameParsing() throws Exception {
        String responseString = "{\"firstName\":\"Craig\",\"lastName\":\"Erickson\",\"fullName\":\"Craig Erickson\"}";
        JSONObject o = new JSONObject(responseString);
        response = new ObjectServerResponse(responseString, true, o);

        String name = parser.parseFaceName(response);
        
        Assert.assertEquals("Craig Erickson", name);
        
    }

    @Test
    public void testMessageSendResponseParsing() throws Exception {
        //Test for situations in which we receive a response containing one MessageToken.
        String responseStringSingularToken = "{\"class\":\"com.zipwhip.outgoing.distributor.OutgoingMessageDistributorResponse\",\"fingerprint\":\"228782999\",\"root\":\"215924819606511616\",\"tokens\":[{\"class\":\"com.zipwhip.outgoing.distributor.OutgoingMessageDistributorToken\",\"contact\":1167259903,\"device\":119021003,\"fingerprint\":\"228782999\",\"message\":\"215924819606511616\"}]}";
        JSONObject o = new JSONObject(responseStringSingularToken);
        response = new ObjectServerResponse(responseStringSingularToken, true, o);
        List<MessageToken> tokens = parser.parseMessageTokens(response);

        Assert.assertEquals(tokens.size(), 1);

        Assert.assertEquals(tokens.get(0).getMessageId(), 215924819606511616L);
        Assert.assertEquals(tokens.get(0).getFingerprint(), "228782999");
        Assert.assertEquals(tokens.get(0).getContactId(), (long)1167259903);
        Assert.assertEquals(tokens.get(0).getDeviceId(), (long)119021003);
        Assert.assertEquals(tokens.get(0).getRootMessageId(), 215924819606511616L);
        
        
        //Test for situations in which we receive a response containing multiple MessageTokens.
        String resposneStringMultipleTokens = "{\"class\":\"com.zipwhip.outgoing.distributor.OutgoingMessageDistributorResponse\",\"fingerprint\":null,\"root\":\"215923266879361026\",\"tokens\":[{\"class\":\"com.zipwhip.outgoing.distributor.OutgoingMessageDistributorToken\",\"contact\":1260602403,\"device\":119021003,\"fingerprint\":\"1206227859\",\"message\":\"215923266879361024\"},{\"class\":\"com.zipwhip.outgoing.distributor.OutgoingMessageDistributorToken\",\"contact\":1260602503,\"device\":119021003,\"fingerprint\":\"4284805295\",\"message\":\"215923266879361025\"}]}";
        o = new JSONObject(resposneStringMultipleTokens);
        response = new ObjectServerResponse(resposneStringMultipleTokens, true, o);
        tokens = parser.parseMessageTokens(response);

        //I'm making the assumption that the tokens will always be returned in the same order.
        Assert.assertEquals(tokens.size(), 2);

        Assert.assertEquals(tokens.get(0).getMessageId(), 215923266879361024L);
        Assert.assertEquals(tokens.get(0).getFingerprint(), "1206227859");
        Assert.assertEquals(tokens.get(0).getContactId(), (long)1260602403);
        Assert.assertEquals(tokens.get(0).getDeviceId(), (long)119021003);
        Assert.assertEquals(tokens.get(0).getRootMessageId(), 215923266879361026L);

        Assert.assertEquals(tokens.get(1).getMessageId(), 215923266879361025L);
        Assert.assertEquals(tokens.get(1).getFingerprint(), "4284805295");
        Assert.assertEquals(tokens.get(1).getContactId(), (long)1260602503);
        Assert.assertEquals(tokens.get(1).getDeviceId(), (long)119021003);
        Assert.assertEquals(tokens.get(1).getRootMessageId(), 215923266879361026L);
        
    }
}
