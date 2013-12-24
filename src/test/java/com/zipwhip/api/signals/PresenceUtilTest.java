package com.zipwhip.api.signals;

import com.zipwhip.signals2.address.ClientAddress;
import com.zipwhip.signals2.presence.Presence;
import com.zipwhip.signals2.presence.UserAgent;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/29/11
 * Time: 5:01 PM
 */
public class PresenceUtilTest {

    public static final String PRESENCE_LIST_JSON = "[{\"category\":{\"name\":\"Phone\",\"enumType\":\"com.zipwhip.signals.presence.PresenceCategory\"},\"userAgent\":{\"product\":{\"name\":{\"name\":\"DEVICE_CARBON\",\"enumType\":\"com.zipwhip.signals.presence.ProductLine\"},\"class\":\"com.zipwhip.signals.presence.Product\",\"build\":\"Zipwhip\",\"version\":\"104\"},\"class\":\"com.zipwhip.signals.presence.UserAgent\",\"makeModel\":\"samsung Nexus S\",\"build\":\"4.0.3\"},\"status\":{\"name\":\"OFFLINE\",\"enumType\":\"com.zipwhip.signals.presence.PresenceStatus\"},\"address\":{\"class\":\"com.zipwhip.signals.address.ClientAddress\",\"clientId\":\"8c5d5e9f-f3e1-46ff-bb3e-5df0640e8ced\"},\"lastActive\":\"2012-01-05T21:45:58-08:00\",\"connected\":false,\"extraInfo\":null,\"class\":\"com.zipwhip.signals.presence.Presence\",\"subscriptionId\":null,\"ip\":\"208.54.32.186\"},{\"category\":{\"name\":\"Phone\",\"enumType\":\"com.zipwhip.signals.presence.PresenceCategory\"},\"userAgent\":{\"product\":{\"name\":{\"name\":\"DEVICE_CARBON\",\"enumType\":\"com.zipwhip.signals.presence.ProductLine\"},\"class\":\"com.zipwhip.signals.presence.Product\",\"build\":\"Zipwhip\",\"version\":\"104\"},\"class\":\"com.zipwhip.signals.presence.UserAgent\",\"makeModel\":\"samsung Nexus S\",\"build\":\"4.0.3\"},\"status\":{\"name\":\"OFFLINE\",\"enumType\":\"com.zipwhip.signals.presence.PresenceStatus\"},\"address\":{\"class\":\"com.zipwhip.signals.address.ClientAddress\",\"clientId\":\"ee841130-94a6-43f6-b81c-d3c9c183adad\"},\"lastActive\":\"2012-01-13T10:29:54-08:00\",\"connected\":false,\"extraInfo\":null,\"class\":\"com.zipwhip.signals.presence.Presence\",\"subscriptionId\":null,\"ip\":\"208.54.32.213\"},{\"category\":{\"name\":\"Phone\",\"enumType\":\"com.zipwhip.signals.presence.PresenceCategory\"},\"userAgent\":{\"product\":{\"name\":{\"name\":\"DEVICE_CARBON\",\"enumType\":\"com.zipwhip.signals.presence.ProductLine\"},\"class\":\"com.zipwhip.signals.presence.Product\",\"build\":\"Zipwhip\",\"version\":\"112\"},\"class\":\"com.zipwhip.signals.presence.UserAgent\",\"makeModel\":\"samsung Nexus S\",\"build\":\"4.0.3\"},\"status\":{\"name\":\"ONLINE\",\"enumType\":\"com.zipwhip.signals.presence.PresenceStatus\"},\"address\":{\"class\":\"com.zipwhip.signals.address.ClientAddress\",\"clientId\":\"164054844846510080\"},\"lastActive\":\"2012-01-30T12:15:31-08:00\",\"connected\":true,\"extraInfo\":null,\"class\":\"com.zipwhip.signals.presence.Presence\",\"subscriptionId\":\"/device/10c0f34c-9372-44b7-bd45-804f5439f277\",\"ip\":null}]";
    
    public static final String IP = "10.1.1.255";
    public static final String SUB_ID = "subscription__123456-123456";
    public static final String CLIENT_ID = "1234-1234-1234-1234";
    public static final String P_BUILD = "Zipwhip";
    public static final String VERSION = "1.0.1";
    public static final String U_BUILD = "2.3.4";
    public static final String MODEL = "HTC Wow";

    Presence presence;
    ClientAddress address;
    UserAgent userAgent;
//    Product product;
//
//    @Before
//    public void setUp() throws Exception {
//
//        presence = new Presence();
//        presence.setConnected(true);
//        presence.setIp(IP);
//        presence.setSubscriptionId(SUB_ID);
//        presence.setCategory(PresenceCategory.Phone);
//        presence.setStatus(PresenceStatus.ONLINE);
//
//        address = new ClientAddress();
//        address.setClientId(CLIENT_ID);
//        presence.setAddress(address);
//
//        product = new Product();
//        product.setBuild(P_BUILD);
//        product.setVersion(VERSION);
//        product.setName(ProductLine.ZIPGROUPS);
//
//        userAgent = new UserAgent();
//        userAgent.setProduct(product);
//        userAgent.setBuild(U_BUILD);
//        userAgent.setMakeModel(MODEL);
//        presence.setUserAgent(userAgent);
//    }
//
//    @Test
//    public void testSerializePresence() throws Exception {
//        JSONArray json = PresenceUtil.getInstance().serialize(Collections.singletonList(presence));
//        System.out.println(json.toString());
//    }
//
//    @Test
//    public void testDeserializePresence() throws Exception {
//
//        List<Presence> presences = PresenceUtil.getInstance().parse(PresenceUtil.getInstance().serialize(Collections.singletonList(presence)));
//
//        for (Presence p : presences) {
//            Assert.assertEquals(p.getAddress().getClientId(), CLIENT_ID);
//            Assert.assertEquals(p.getCategory(), PresenceCategory.Phone);
//            Assert.assertTrue(p.getConnected());
//            Assert.assertEquals(p.getIp(), IP);
//            Assert.assertEquals(p.getStatus(), PresenceStatus.ONLINE);
//            Assert.assertEquals(p.getSubscriptionId(), SUB_ID);
//            Assert.assertEquals(p.getUserAgent().getBuild(), U_BUILD);
//            Assert.assertEquals(p.getUserAgent().getMakeModel(), MODEL);
//            Assert.assertEquals(p.getUserAgent().getProduct().getBuild(), P_BUILD);
//            Assert.assertEquals(p.getUserAgent().getProduct().getName(), ProductLine.ZIPGROUPS);
//            Assert.assertEquals(p.getUserAgent().getProduct().getVersion(), VERSION);
//        }
//
//    }
//
//    @Test
//    public void testParsePresence() throws Exception {
//
//        JSONArray jsonArray = new JSONArray(PRESENCE_LIST_JSON);
//        Assert.assertTrue(jsonArray.length() == 3);
//
//        List<Presence> presences = PresenceUtil.getInstance().parse(jsonArray);
//
//        for (Presence p : presences) {
//            Assert.assertEquals(PresenceCategory.Phone, p.getCategory());
//            Assert.assertEquals(ProductLine.DEVICE_CARBON, p.getUserAgent().getProduct().getName());
//        }
//    }

}
