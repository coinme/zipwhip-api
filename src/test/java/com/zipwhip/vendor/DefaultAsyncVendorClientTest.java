package com.zipwhip.vendor;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.ZipwhipNetworkSupport;
import com.zipwhip.api.dto.Contact;
import com.zipwhip.api.dto.Conversation;
import com.zipwhip.api.dto.EnrollmentResult;
import com.zipwhip.api.dto.MessageToken;
import com.zipwhip.api.response.MessageListResult;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.MutableObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.SignTool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/12/11
 * Time: 10:39 AM
 */
public class DefaultAsyncVendorClientTest {

    AsyncVendorClient client;

    String apiKey = "jdo29chk";
    String secret = "anwcc99d-d152-ddw2-nmqp-oladwkn24dal90lot56s-9ns1-svm2-10b3-kd8bm21d9sl1";
    String deviceAddress = "device:/2069308934/0";
    String contactMobileNumber = "2063758020";

    public final static String VOID_RESULT = "{\"response\":null,\"sessions\":null,\"success\":true}";
    public final static String BOOLEAN_TRUE_RESULT = "{\"response\":true,\"sessions\":null,\"success\":true}";
    public final static String ENROLLMENT_RESULT = "{\"response\":{\"carbonEnabled\":true,\"carbonInstalled\":true,\"deviceNumber\":999},\"sessions\":null,\"success\":true}";
    public final static String CONTACT_LIST_RESULT = "{\"total\":1,\"response\":[{\"birthday\":null,\"state\":\"\",\"version\":4,\"dtoParentId\":270315,\"city\":\"\",\"id\":408050,\"phoneKey\":\"\",\"isZwUser\":false,\"vector\":\"\",\"thread\":\"20000002\",\"phoneId\":0,\"carrier\":\"Tmo\",\"firstName\":\"\",\"deviceId\":270315,\"lastName\":\"\",\"MOCount\":0,\"keywords\":\"\",\"zipcode\":\"\",\"ZOCount\":0,\"class\":\"com.zipwhip.website.data.dto.Contact\",\"lastUpdated\":\"2011-10-14T14:59:51-07:00\",\"loc\":\"\",\"targetGroupDevice\":-1,\"fwd\":\"20000102\",\"deleted\":false,\"latlong\":\"\",\"new\":false,\"email\":\"\",\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-10-14T14:58:54-07:00\",\"mobileNumber\":\"2533654478\",\"notes\":\"\",\"channel\":\"2\"}],\"sessions\":null,\"page\":1,\"pages\":1,\"success\":true}";
    public final static String CONTACT_SAVE_RESULT = "{\"response\":{\"birthday\":null,\"state\":\"\",\"version\":1,\"dtoParentId\":270315,\"city\":\"\",\"id\":408077,\"phoneKey\":\"\",\"isZwUser\":false,\"vector\":\"\",\"thread\":\"\",\"phoneId\":0,\"carrier\":\"\",\"firstName\":\"Jon\",\"deviceId\":270315,\"lastName\":\"Dow\",\"MOCount\":0,\"keywords\":\"\",\"zipcode\":\"\",\"ZOCount\":0,\"class\":\"com.zipwhip.website.data.dto.Contact\",\"lastUpdated\":\"2011-10-17T13:19:48-07:00\",\"loc\":\"\",\"targetGroupDevice\":-1,\"fwd\":\"\",\"deleted\":false,\"latlong\":\"\",\"new\":false,\"email\":\"\",\"address\":\"device:/5555555555/0\",\"dateCreated\":\"2011-10-17T13:19:48-07:00\",\"mobileNumber\":\"5555555555\",\"notes\":\"\",\"channel\":\"\"},\"sessions\":null,\"success\":true}";
    public final static String CONVERSATION_LIST_RESULT = "{\"total\":1,\"response\":[{\"lastContactFirstName\":\"\",\"lastContactLastName\":\"\",\"lastContactDeviceId\":0,\"unreadCount\":0,\"bcc\":\"\",\"lastUpdated\":\"2011-10-14T14:59:51-07:00\",\"class\":\"com.zipwhip.website.data.dto.Conversation\",\"deviceAddress\":\"device:/2063758020/0\",\"lastNonDeletedMessageDate\":\"2011-10-14T14:59:51-07:00\",\"deleted\":false,\"lastContactId\":408050,\"version\":2,\"lastMessageDate\":\"2011-10-14T14:59:51-07:00\",\"dtoParentId\":270315,\"lastContactMobileNumber\":\"2069308934\",\"id\":1912,\"fingerprint\":\"2216445311\",\"new\":false,\"lastMessageBody\":\"yr cool! \",\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-10-14T14:59:05-07:00\",\"cc\":\"\",\"deviceId\":270315}],\"sessions\":null,\"success\":true,\"size\":1}";
    public final static String USER_SAVE_RESULT = "{\"response\":{\"user\":{\"firstName\":\"Im\",\"lastName\":\"Cool\",\"mobileNumber\":\"2063758020\",\"fullName\":\"Im Cool\",\"phoneKey\":\"\",\"email\":\"\",\"notes\":\"\",\"birthday\":\"\",\"carrier\":\"Tmo\",\"loc\":\"\",\"dateCreated\":\"2011-10-14T14:57:35-07:00\",\"lastUpdated\":\"2011-10-17T13:40:01-07:00\"}},\"sessions\":null,\"success\":true}";
    public final static String MESSAGE_SEND_RESULT = "{\"response\":{\"fingerprint\":\"3969778241\",\"root\":\"7373193f-cb64-4e37-9ed6-a79d57fab524\",\"tokens\":[{\"message\":\"7373193f-cb64-4e37-9ed6-a79d57fab524\",\"fingerprint\":\"3969778241\",\"device\":270315,\"class\":\"com.zipwhip.outgoing.distributor.OutgoingMessageDistributorToken\",\"contact\":-1}],\"class\":\"com.zipwhip.outgoing.distributor.OutgoingMessageDistributorResponse\"},\"sessions\":null,\"success\":true}";
    public final static String CONTACT_GET_RESPONSE = "{\"response\":{\"birthday\":null,\"state\":\"\",\"version\":4,\"dtoParentId\":270315,\"city\":\"\",\"id\":408050,\"phoneKey\":\"\",\"isZwUser\":false,\"vector\":\"\",\"thread\":\"20000002\",\"phoneId\":0,\"carrier\":\"Tmo\",\"firstName\":\"\",\"deviceId\":270315,\"lastName\":\"\",\"MOCount\":0,\"keywords\":\"\",\"zipcode\":\"\",\"ZOCount\":0,\"class\":\"com.zipwhip.website.data.dto.Contact\",\"lastUpdated\":\"2011-10-14T14:59:51-07:00\",\"loc\":\"\",\"targetGroupDevice\":-1,\"fwd\":\"20000102\",\"deleted\":false,\"latlong\":\"\",\"new\":false,\"email\":\"\",\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-10-14T14:58:54-07:00\",\"mobileNumber\":\"2063758020\",\"notes\":\"\",\"channel\":\"2\"},\"sessions\":null,\"success\":true}";
    public final static String MESSAGE_LIST_RESULT = "{\"total\":10,\"response\":[{\"DCSId\":\"\",\"UDH\":\"\",\"address\":\"ptn:/5554443333\",\"advertisement\":\"\",\"bcc\":null,\"body\":\"Hi mom\",\"bodySize\":6,\"carbonedMessageId\":-1,\"carrier\":\"CellSouth\",\"cc\":null,\"channel\":\"\",\"class\":\"com.zipwhip.website.data.dto.LegacyMessage\",\"contactDeviceId\":128918006,\"contactId\":632328706,\"creatorId\":395081706,\"dateCreated\":\"2012-06-06T15:50:39-07:00\",\"deleted\":false,\"deliveryReceipt\":null,\"destAddress\":\"5554443333\",\"deviceId\":128918006,\"dishedToOpenMarket\":null,\"dtoParentId\":128918006,\"encoded\":false,\"errorState\":false,\"expectDeliveryReceipt\":false,\"fingerprint\":\"495418036\",\"firstName\":\"\",\"fromName\":\"\",\"fwd\":\"\",\"hasAttachment\":false,\"id\":\"210504002298974208\",\"isInFinalState\":false,\"isParent\":false,\"isRead\":true,\"isSelf\":false,\"lastName\":\"\",\"lastUpdated\":null,\"latlong\":\"\",\"loc\":\"\",\"messageConsoleLog\":\"\",\"metaDataId\":0,\"mobileNumber\":\"5554443333\",\"new\":false,\"openMarketMessageId\":\"\",\"parentId\":0,\"phoneKey\":\"\",\"scheduledDate\":null,\"smartForwarded\":false,\"smartForwardingCandidate\":false,\"sourceAddress\":\"2069308934\",\"statusCode\":0,\"statusDesc\":null,\"subject\":null,\"thread\":\"\",\"to\":null,\"transferedToCarrierReceipt\":null,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"DELIVERED\"},\"type\":\"ZO\",\"uuid\":\"210504002298974208\",\"version\":0,\"visible\":true},{\"DCSId\":\"\",\"UDH\":\"\",\"address\":\"ptn:/2068597896\",\"advertisement\":null,\"bcc\":null,\"body\":\"Message.\\n\\nSent via Zipwhip\",\"bodySize\":26,\"carbonedMessageId\":-1,\"carrier\":\"ATT\",\"cc\":null,\"channel\":\"\",\"class\":\"com.zipwhip.website.data.dto.LegacyMessage\",\"contactDeviceId\":128918006,\"contactId\":220583306,\"creatorId\":318144706,\"dateCreated\":\"2012-06-06T15:02:56-07:00\",\"deleted\":false,\"deliveryReceipt\":null,\"destAddress\":\"2069308934\",\"deviceId\":128918006,\"dishedToOpenMarket\":null,\"dtoParentId\":128918006,\"encoded\":false,\"errorState\":false,\"expectDeliveryReceipt\":false,\"fingerprint\":\"4233621183\",\"firstName\":\"Alan\",\"fromName\":null,\"fwd\":\"\",\"hasAttachment\":false,\"id\":\"210492010933985280\",\"isInFinalState\":false,\"isParent\":false,\"isRead\":true,\"isSelf\":false,\"lastName\":\"Capps\",\"lastUpdated\":null,\"latlong\":\"\",\"loc\":\"\",\"messageConsoleLog\":\"\",\"metaDataId\":0,\"mobileNumber\":\"2068597896\",\"new\":false,\"openMarketMessageId\":\"\",\"parentId\":0,\"phoneKey\":\"\",\"scheduledDate\":null,\"smartForwarded\":false,\"smartForwardingCandidate\":false,\"sourceAddress\":\"2068597896\",\"statusCode\":4,\"statusDesc\":null,\"subject\":\"\",\"thread\":\"\",\"to\":null,\"transferedToCarrierReceipt\":null,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"DELIVERED\"},\"type\":\"MO\",\"uuid\":\"210492010933985280\",\"version\":0,\"visible\":true},{\"DCSId\":\"\",\"UDH\":\"\",\"address\":\"ptn:/2068597896\",\"advertisement\":null,\"bcc\":null,\"body\":\"Test message.\\n\\nSent via Zipwhip\",\"bodySize\":31,\"carbonedMessageId\":-1,\"carrier\":\"ATT\",\"cc\":null,\"channel\":\"\",\"class\":\"com.zipwhip.website.data.dto.LegacyMessage\",\"contactDeviceId\":128918006,\"contactId\":220583306,\"creatorId\":318144706,\"dateCreated\":\"2012-06-06T14:57:45-07:00\",\"deleted\":false,\"deliveryReceipt\":null,\"destAddress\":\"2069308934\",\"deviceId\":128918006,\"dishedToOpenMarket\":null,\"dtoParentId\":128918006,\"encoded\":false,\"errorState\":false,\"expectDeliveryReceipt\":false,\"fingerprint\":\"4233621183\",\"firstName\":\"Alan\",\"fromName\":null,\"fwd\":\"\",\"hasAttachment\":false,\"id\":\"210490706102657024\",\"isInFinalState\":false,\"isParent\":false,\"isRead\":true,\"isSelf\":false,\"lastName\":\"Capps\",\"lastUpdated\":null,\"latlong\":\"\",\"loc\":\"\",\"messageConsoleLog\":\"\",\"metaDataId\":0,\"mobileNumber\":\"2068597896\",\"new\":false,\"openMarketMessageId\":\"\",\"parentId\":0,\"phoneKey\":\"\",\"scheduledDate\":null,\"smartForwarded\":false,\"smartForwardingCandidate\":false,\"sourceAddress\":\"2068597896\",\"statusCode\":4,\"statusDesc\":null,\"subject\":\"\",\"thread\":\"\",\"to\":null,\"transferedToCarrierReceipt\":null,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"DELIVERED\"},\"type\":\"MO\",\"uuid\":\"210490706102657024\",\"version\":0,\"visible\":true},{\"DCSId\":\"\",\"UDH\":\"\",\"address\":\"ptn:/2066319536\",\"advertisement\":null,\"bcc\":null,\"body\":\"Your order for a double espresso is completed. Get your texts on your desktop by downloading our PC, Mac, or Linux app at zipwhip.com.\",\"bodySize\":134,\"carbonedMessageId\":-1,\"carrier\":\"Tmo\",\"cc\":null,\"channel\":\"\",\"class\":\"com.zipwhip.website.data.dto.LegacyMessage\",\"contactDeviceId\":128918006,\"contactId\":254145806,\"creatorId\":318144706,\"dateCreated\":\"2012-06-06T10:08:53-07:00\",\"deleted\":false,\"deliveryReceipt\":null,\"destAddress\":\"2069308934\",\"deviceId\":128918006,\"dishedToOpenMarket\":null,\"dtoParentId\":128918006,\"encoded\":false,\"errorState\":false,\"expectDeliveryReceipt\":false,\"fingerprint\":\"1267762954\",\"firstName\":\"Textspresso\",\"fromName\":null,\"fwd\":\"\",\"hasAttachment\":false,\"id\":\"210418046543007744\",\"isInFinalState\":false,\"isParent\":false,\"isRead\":true,\"isSelf\":false,\"lastName\":\"\",\"lastUpdated\":null,\"latlong\":\"\",\"loc\":\"\",\"messageConsoleLog\":\"\",\"metaDataId\":0,\"mobileNumber\":\"2066319536\",\"new\":false,\"openMarketMessageId\":\"\",\"parentId\":0,\"phoneKey\":\"\",\"scheduledDate\":null,\"smartForwarded\":false,\"smartForwardingCandidate\":false,\"sourceAddress\":\"2066319536\",\"statusCode\":4,\"statusDesc\":null,\"subject\":\"\",\"thread\":\"\",\"to\":null,\"transferedToCarrierReceipt\":null,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"DELIVERED\"},\"type\":\"MO\",\"uuid\":\"210418046543007744\",\"version\":0,\"visible\":true},{\"DCSId\":\"\",\"UDH\":\"\",\"address\":\"ptn:/2066319536\",\"advertisement\":null,\"bcc\":null,\"body\":\"You ordered a double espresso with the 'local' cmd so get your cup under the  coffee nozzles quickly!\",\"bodySize\":101,\"carbonedMessageId\":-1,\"carrier\":\"Tmo\",\"cc\":null,\"channel\":\"\",\"class\":\"com.zipwhip.website.data.dto.LegacyMessage\",\"contactDeviceId\":128918006,\"contactId\":254145806,\"creatorId\":318144706,\"dateCreated\":\"2012-06-06T10:08:51-07:00\",\"deleted\":false,\"deliveryReceipt\":null,\"destAddress\":\"2069308934\",\"deviceId\":128918006,\"dishedToOpenMarket\":null,\"dtoParentId\":128918006,\"encoded\":false,\"errorState\":false,\"expectDeliveryReceipt\":false,\"fingerprint\":\"1267762954\",\"firstName\":\"Textspresso\",\"fromName\":null,\"fwd\":\"\",\"hasAttachment\":false,\"id\":\"210418017291931648\",\"isInFinalState\":false,\"isParent\":false,\"isRead\":true,\"isSelf\":false,\"lastName\":\"\",\"lastUpdated\":null,\"latlong\":\"\",\"loc\":\"\",\"messageConsoleLog\":\"\",\"metaDataId\":0,\"mobileNumber\":\"2066319536\",\"new\":false,\"openMarketMessageId\":\"\",\"parentId\":0,\"phoneKey\":\"\",\"scheduledDate\":null,\"smartForwarded\":false,\"smartForwardingCandidate\":false,\"sourceAddress\":\"2066319536\",\"statusCode\":4,\"statusDesc\":null,\"subject\":\"\",\"thread\":\"\",\"to\":null,\"transferedToCarrierReceipt\":null,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"DELIVERED\"},\"type\":\"MO\",\"uuid\":\"210418017291931648\",\"version\":0,\"visible\":true},{\"DCSId\":\"\",\"UDH\":\"\",\"address\":\"ptn:/2066319536\",\"advertisement\":null,\"bcc\":null,\"body\":\"Espresso Double local\",\"bodySize\":21,\"carbonedMessageId\":-1,\"carrier\":\"Tmo\",\"cc\":null,\"channel\":\"\",\"class\":\"com.zipwhip.website.data.dto.LegacyMessage\",\"contactDeviceId\":128918006,\"contactId\":254145806,\"creatorId\":318144706,\"dateCreated\":\"2012-06-06T10:08:38-07:00\",\"deleted\":false,\"deliveryReceipt\":null,\"destAddress\":\"2066319536\",\"deviceId\":128918006,\"dishedToOpenMarket\":null,\"dtoParentId\":128918006,\"encoded\":false,\"errorState\":false,\"expectDeliveryReceipt\":false,\"fingerprint\":\"1267762954\",\"firstName\":\"Textspresso\",\"fromName\":null,\"fwd\":\"\",\"hasAttachment\":false,\"id\":\"210417984165253120\",\"isInFinalState\":false,\"isParent\":false,\"isRead\":true,\"isSelf\":false,\"lastName\":\"\",\"lastUpdated\":null,\"latlong\":\"\",\"loc\":\"\",\"messageConsoleLog\":\"\",\"metaDataId\":0,\"mobileNumber\":\"2066319536\",\"new\":false,\"openMarketMessageId\":\"\",\"parentId\":0,\"phoneKey\":\"\",\"scheduledDate\":null,\"smartForwarded\":false,\"smartForwardingCandidate\":false,\"sourceAddress\":\"2069308934\",\"statusCode\":4,\"statusDesc\":null,\"subject\":\"\",\"thread\":\"\",\"to\":null,\"transferedToCarrierReceipt\":null,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"DELIVERED\"},\"type\":\"ZO\",\"uuid\":\"210417984165253120\",\"version\":0,\"visible\":true},{\"DCSId\":\"\",\"UDH\":\"\",\"address\":\"ptn:/2069308934\",\"advertisement\":null,\"bcc\":null,\"body\":\"oh yea\",\"bodySize\":6,\"carbonedMessageId\":-1,\"carrier\":\"Tmo\",\"cc\":null,\"channel\":\"17\",\"class\":\"com.zipwhip.website.data.dto.LegacyMessage\",\"contactDeviceId\":128918006,\"contactId\":220588106,\"creatorId\":318144706,\"dateCreated\":\"2012-06-05T23:52:19-07:00\",\"deleted\":false,\"deliveryReceipt\":null,\"destAddress\":\"2069308934\",\"deviceId\":128918006,\"dishedToOpenMarket\":null,\"dtoParentId\":128918006,\"encoded\":false,\"errorState\":false,\"expectDeliveryReceipt\":false,\"fingerprint\":\"2216445311\",\"firstName\":\"\",\"fromName\":null,\"fwd\":\"20000117\",\"hasAttachment\":false,\"id\":\"210262859534635008\",\"isInFinalState\":false,\"isParent\":false,\"isRead\":true,\"isSelf\":false,\"lastName\":\"\",\"lastUpdated\":null,\"latlong\":\"\",\"loc\":\"\",\"messageConsoleLog\":\"\",\"metaDataId\":0,\"mobileNumber\":\"2069308934\",\"new\":false,\"openMarketMessageId\":\"\",\"parentId\":0,\"phoneKey\":\"\",\"scheduledDate\":null,\"smartForwarded\":false,\"smartForwardingCandidate\":false,\"sourceAddress\":\"2069308934\",\"statusCode\":4,\"statusDesc\":null,\"subject\":\"\",\"thread\":\"20000017\",\"to\":null,\"transferedToCarrierReceipt\":null,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"DELIVERED\"},\"type\":\"MO\",\"uuid\":\"210262859534635008\",\"version\":0,\"visible\":true},{\"DCSId\":\"\",\"UDH\":\"\",\"address\":\"ptn:/2069308934\",\"advertisement\":\"\",\"bcc\":null,\"body\":\"oh yea\",\"bodySize\":6,\"carbonedMessageId\":-1,\"carrier\":\"Tmo\",\"cc\":null,\"channel\":\"17\",\"class\":\"com.zipwhip.website.data.dto.LegacyMessage\",\"contactDeviceId\":128918006,\"contactId\":220588106,\"creatorId\":417114506,\"dateCreated\":\"2012-06-05T23:52:16-07:00\",\"deleted\":false,\"deliveryReceipt\":null,\"destAddress\":\"2069308934\",\"deviceId\":128918006,\"dishedToOpenMarket\":null,\"dtoParentId\":128918006,\"encoded\":false,\"errorState\":false,\"expectDeliveryReceipt\":false,\"fingerprint\":\"2216445311\",\"firstName\":\"\",\"fromName\":\"\",\"fwd\":\"20000117\",\"hasAttachment\":false,\"id\":\"210262814849703936\",\"isInFinalState\":false,\"isParent\":false,\"isRead\":true,\"isSelf\":false,\"lastName\":\"\",\"lastUpdated\":null,\"latlong\":\"\",\"loc\":\"\",\"messageConsoleLog\":\"\",\"metaDataId\":0,\"mobileNumber\":\"2069308934\",\"new\":false,\"openMarketMessageId\":\"\",\"parentId\":0,\"phoneKey\":\"\",\"scheduledDate\":null,\"smartForwarded\":false,\"smartForwardingCandidate\":false,\"sourceAddress\":\"2069308934\",\"statusCode\":0,\"statusDesc\":null,\"subject\":null,\"thread\":\"20000017\",\"to\":null,\"transferedToCarrierReceipt\":null,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"DELIVERED\"},\"type\":\"ZO\",\"uuid\":\"210262814849703936\",\"version\":0,\"visible\":true},{\"DCSId\":\"\",\"UDH\":\"\",\"address\":\"ptn:/2069308934\",\"advertisement\":null,\"bcc\":null,\"body\":\"Party time beyatch\",\"bodySize\":18,\"carbonedMessageId\":-1,\"carrier\":\"Tmo\",\"cc\":null,\"channel\":\"17\",\"class\":\"com.zipwhip.website.data.dto.LegacyMessage\",\"contactDeviceId\":128918006,\"contactId\":220588106,\"creatorId\":318144706,\"dateCreated\":\"2012-06-05T23:51:54-07:00\",\"deleted\":false,\"deliveryReceipt\":null,\"destAddress\":\"2069308934\",\"deviceId\":128918006,\"dishedToOpenMarket\":null,\"dtoParentId\":128918006,\"encoded\":false,\"errorState\":false,\"expectDeliveryReceipt\":false,\"fingerprint\":\"2216445311\",\"firstName\":\"\",\"fromName\":null,\"fwd\":\"20000117\",\"hasAttachment\":false,\"id\":\"210262733214720000\",\"isInFinalState\":false,\"isParent\":false,\"isRead\":true,\"isSelf\":false,\"lastName\":\"\",\"lastUpdated\":null,\"latlong\":\"\",\"loc\":\"\",\"messageConsoleLog\":\"\",\"metaDataId\":0,\"mobileNumber\":\"2069308934\",\"new\":false,\"openMarketMessageId\":\"\",\"parentId\":0,\"phoneKey\":\"\",\"scheduledDate\":null,\"smartForwarded\":false,\"smartForwardingCandidate\":false,\"sourceAddress\":\"2069308934\",\"statusCode\":4,\"statusDesc\":null,\"subject\":\"\",\"thread\":\"20000017\",\"to\":null,\"transferedToCarrierReceipt\":null,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"DELIVERED\"},\"type\":\"MO\",\"uuid\":\"210262733214720000\",\"version\":0,\"visible\":true},{\"DCSId\":\"\",\"UDH\":\"\",\"address\":\"ptn:/2069308934\",\"advertisement\":\"\",\"bcc\":null,\"body\":\"Party time beyatch\",\"bodySize\":18,\"carbonedMessageId\":-1,\"carrier\":\"Tmo\",\"cc\":null,\"channel\":\"17\",\"class\":\"com.zipwhip.website.data.dto.LegacyMessage\",\"contactDeviceId\":128918006,\"contactId\":220588106,\"creatorId\":417114506,\"dateCreated\":\"2012-06-05T23:51:49-07:00\",\"deleted\":false,\"deliveryReceipt\":null,\"destAddress\":\"2069308934\",\"deviceId\":128918006,\"dishedToOpenMarket\":null,\"dtoParentId\":128918006,\"encoded\":false,\"errorState\":false,\"expectDeliveryReceipt\":false,\"fingerprint\":\"2216445311\",\"firstName\":\"\",\"fromName\":\"\",\"fwd\":\"20000117\",\"hasAttachment\":false,\"id\":\"210262700978413568\",\"isInFinalState\":false,\"isParent\":false,\"isRead\":true,\"isSelf\":false,\"lastName\":\"\",\"lastUpdated\":null,\"latlong\":\"\",\"loc\":\"\",\"messageConsoleLog\":\"\",\"metaDataId\":0,\"mobileNumber\":\"2069308934\",\"new\":false,\"openMarketMessageId\":\"\",\"parentId\":0,\"phoneKey\":\"\",\"scheduledDate\":null,\"smartForwarded\":false,\"smartForwardingCandidate\":false,\"sourceAddress\":\"2069308934\",\"statusCode\":0,\"statusDesc\":null,\"subject\":null,\"thread\":\"20000017\",\"to\":null,\"transferedToCarrierReceipt\":null,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"DELIVERED\"},\"type\":\"ZO\",\"uuid\":\"210262700978413568\",\"version\":0,\"visible\":true}],\"sessions\":null,\"success\":true,\"size\":2845}";

    @Before
    public void setUp() throws Exception {
//        client = AsyncVendorClientFactory.createViaApiKey(apiKey, secret);
        client = new DefaultAsyncVendorClient();
        client.setConnection(new MockApiConnection());
        client.enrollUser(deviceAddress);
    }

    @Test
    public void testEnrollUser() throws Exception {
        ObservableFuture<EnrollmentResult> result = client.enrollUser(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNotNull(result.getResult());
        Assert.assertTrue(result.getResult().isCarbonEnabled());
        Assert.assertTrue(result.getResult().isCarbonInstalled());
        Assert.assertEquals(result.getResult().getDeviceNumber(), 999);
    }

    @Test
    public void testDeactivateUser() throws Exception {
        ObservableFuture<Void> result = client.deactivateUser(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testUserExists() throws Exception {
        ObservableFuture<Boolean> result = client.userExists(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNotNull(result.getResult());
        Assert.assertTrue(result.getResult());
    }

    @Test
    public void testSuggestCarbon() throws Exception {
        ObservableFuture<Void> result = client.suggestCarbon(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testCarbonInstalled() throws Exception {
        ObservableFuture<Boolean> result = client.carbonInstalled(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertTrue(result.getResult());
    }

    @Test
    public void testCarbonEnabled() throws Exception {
        ObservableFuture<Boolean> result = client.carbonEnabled(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertTrue(result.getResult());
    }

    @Test
    public void testSendMessage() throws Exception {
        ObservableFuture<List<MessageToken>> result = client.sendMessage(deviceAddress, Collections.singleton("5554443333"), "Hi mom");
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNotNull(result.getResult());
        Assert.assertEquals(result.getResult().get(0).getMessageId(), "7373193f-cb64-4e37-9ed6-a79d57fab524");
    }

    @Test
    public void testSaveUser() throws Exception {
        Contact user = new Contact();
        user.setFirstName("Im");
        user.setLastName("Cool");
        ObservableFuture<Contact> result = client.saveUser(deviceAddress, user);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getResult().getFirstName(), "Im");
        Assert.assertEquals(result.getResult().getLastName(), "Cool");
    }

    @Test
    public void testReadMessages() throws Exception {
        Set<String> messages = new HashSet<String>();
        messages.add("123456");
        messages.add("654321");
        ObservableFuture<Void> result = client.readMessages(deviceAddress, messages);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testDeleteMessages() throws Exception {
        Set<String> messages = new HashSet<String>();
        messages.add("123456");
        messages.add("654321");
        ObservableFuture<Void> result = client.deleteMessages(deviceAddress, messages);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testReadConversation() throws Exception {
        ObservableFuture<Void> result = client.readConversation(deviceAddress, "123456");
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testDeleteConversation() throws Exception {
        ObservableFuture<Void> result = client.deleteConversation(deviceAddress, "123456");
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testListConversations() throws Exception {
        ObservableFuture<List<Conversation>> result = client.listConversations(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getResult().size(), 1);
        Assert.assertEquals(result.getResult().get(0).getDeviceAddress(), "device:/2063758020/0");
    }

    @Test
    public void testSaveContact() throws Exception {
        Contact contact = new Contact();
        contact.setFirstName("Dennis");
        contact.setLastName("Ritchie");
        contact.setAddress("ptn:/5555555555");
        ObservableFuture<Contact> result = client.saveContact(deviceAddress, contact);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getResult().getId(), 408077L);
    }

    @Test
    public void testDeleteContact() throws Exception {
        Set<String> contacts = new HashSet<String>();
        contacts.add("123456");
        contacts.add("654321");
        ObservableFuture<Void> result = client.deleteContacts(deviceAddress, contacts);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testListContacts() throws Exception {
        ObservableFuture<List<Contact>> result = client.listContacts(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getResult().size(), 1);
        Assert.assertEquals(result.getResult().get(0).getId(), 408050L);
    }

    @Test
    public void testGetContact() throws Exception {
        ObservableFuture<Contact> result = client.getContact(deviceAddress, contactMobileNumber);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getResult().getMobileNumber(), contactMobileNumber);
    }

    @Test
    public void testListMessages() throws Exception {
        ObservableFuture<MessageListResult> result = client.listMessages(deviceAddress, 0, 10);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(10, result.getResult().getTotal());
        Assert.assertEquals(2845, result.getResult().getSize());
    }

    public class MockApiConnection extends DestroyableBase implements ApiConnection {

        @Override
        public ObservableFuture<String> send(String method, Map<String, Object> params) throws Exception {

            MutableObservableFuture<String> result = new DefaultObservableFuture<String>(this);

            if (ZipwhipNetworkSupport.USER_ENROLL.equalsIgnoreCase(method)) {
                result.setSuccess(ENROLLMENT_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.USER_DEACT.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.USER_EXISTS.equalsIgnoreCase(method)) {
                result.setSuccess(BOOLEAN_TRUE_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CARBON_SUGGEST.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.MESSAGE_READ.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.MESSAGE_DELETE.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONVERSATION_READ.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONVERSATION_DELETE.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONTACT_DELETE.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONTACT_LIST.equalsIgnoreCase(method)) {
                result.setSuccess(CONTACT_LIST_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONTACT_SAVE.equalsIgnoreCase(method)) {
                result.setSuccess(CONTACT_SAVE_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONVERSATION_LIST.equalsIgnoreCase(method)) {
                result.setSuccess(CONVERSATION_LIST_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.USER_SAVE.equalsIgnoreCase(method)) {
                result.setSuccess(USER_SAVE_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.MESSAGE_SEND.equalsIgnoreCase(method)) {
                result.setSuccess(MESSAGE_SEND_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CARBON_ENABLED_VENDOR.equalsIgnoreCase(method)) {
                result.setSuccess(BOOLEAN_TRUE_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CARBON_INSTALLED.equalsIgnoreCase(method)) {
                result.setSuccess(BOOLEAN_TRUE_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONTACT_GET.equalsIgnoreCase(method)) {
                result.setSuccess(CONTACT_GET_RESPONSE);
                return result;
            }
            if (ZipwhipNetworkSupport.MESSAGE_LIST.equalsIgnoreCase(method)) {
                result.setSuccess(MESSAGE_LIST_RESULT);
                return result;
            }

            return result;
        }

        @Override
        public ObservableFuture<String> send(String method, Map<String, Object> params, List<File> files) throws Exception {
            return null;
        }

        @Override
        public void setSessionKey(String sessionKey) {

        }

        @Override
        public String getSessionKey() {
            return "";
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void setAuthenticator(SignTool authenticator) {

        }

        @Override
        public SignTool getAuthenticator() {
            return null;
        }

        @Override
        public void setHost(String host) {

        }

        @Override
        public String getHost() {
            return null;
        }

        @Override
        public void setApiVersion(String apiVersion) {

        }

        @Override
        public String getApiVersion() {
            return null;
        }

        @Override
        protected void onDestroy() {

        }

        @Override
        public ObservableFuture<InputStream> sendBinaryResponse(String method, Map<String, Object> params) throws Exception {
            return null;
        }

    }

}