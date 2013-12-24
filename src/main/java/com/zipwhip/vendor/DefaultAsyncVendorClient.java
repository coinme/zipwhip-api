package com.zipwhip.vendor;

import com.zipwhip.api.Address;
import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.ZipwhipNetworkSupport;
import com.zipwhip.api.dto.*;
import com.zipwhip.api.dto.EnrollmentResult;
import com.zipwhip.api.response.BooleanServerResponse;
import com.zipwhip.api.response.MessageListResult;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.MutableObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.InputRunnable;
import com.zipwhip.util.StringUtil;

import java.util.*;

public class DefaultAsyncVendorClient extends ZipwhipNetworkSupport implements AsyncVendorClient {

    /**
     * Create a new {@code DefaultAsyncVendorClient} with a default configuration.
     */
    public DefaultAsyncVendorClient() {

    }

    /**
     * Create a new {@code DefaultAsyncVendorClient} with the desired connection and no signalProvider.
     *
     * @param connection The connection to Zipwhip. This is mandatory, passing null will result in a {@code IllegalArgumentException} being thrown.
     */
    public DefaultAsyncVendorClient(ApiConnection connection) {
        super(connection);
    }

    @Override
    public ObservableFuture<EnrollmentResult> enrollUser(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.USER_ENROLL, params, true, new InputRunnable<ParsableServerResponse<EnrollmentResult>>() {
                @Override
                public void run(ParsableServerResponse<EnrollmentResult> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseEnrollmentResult(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> deactivateUser(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.USER_DEACT, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Boolean> userExists(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("mobileNumber", Address.stripToMobileNumber(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.USER_EXISTS, params, true, new InputRunnable<ParsableServerResponse<Boolean>>() {
                @Override
                public void run(ParsableServerResponse<Boolean> parsableServerResponse) {

                    if (parsableServerResponse.getServerResponse() instanceof BooleanServerResponse) {
                        parsableServerResponse.getFuture().setSuccess(((BooleanServerResponse) parsableServerResponse.getServerResponse()).getResponse());
                    } else {
                        parsableServerResponse.getFuture().setFailure(new Exception("Bad server response type"));
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> suggestCarbon(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.CARBON_SUGGEST, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Boolean> carbonInstalled(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.CARBON_INSTALLED, params, true, new InputRunnable<ParsableServerResponse<Boolean>>() {
                @Override
                public void run(ParsableServerResponse<Boolean> parsableServerResponse) {

                    if (parsableServerResponse.getServerResponse() instanceof BooleanServerResponse) {
                        parsableServerResponse.getFuture().setSuccess(((BooleanServerResponse) parsableServerResponse.getServerResponse()).getResponse());
                    } else {
                        parsableServerResponse.getFuture().setFailure(new Exception("Bad server response type"));
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Boolean> carbonEnabled(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.CARBON_ENABLED_VENDOR, params, true, new InputRunnable<ParsableServerResponse<Boolean>>() {
                @Override
                public void run(ParsableServerResponse<Boolean> parsableServerResponse) {

                    if (parsableServerResponse.getServerResponse() instanceof BooleanServerResponse) {
                        parsableServerResponse.getFuture().setSuccess(((BooleanServerResponse) parsableServerResponse.getServerResponse()).getResponse());
                    } else {
                        parsableServerResponse.getFuture().setFailure(new Exception("Bad server response type"));
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<List<MessageToken>> sendMessage(String deviceAddress, String friendAddress, String body) {
        return sendMessage(deviceAddress, Collections.singleton(friendAddress), body);
    }

    @Override
    public ObservableFuture<List<MessageToken>> sendMessage(String deviceAddress, Set<String> contactMobileNumbers, String body) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        if (CollectionUtil.isNullOrEmpty(contactMobileNumbers)) {
            return invalidArgumentFailureFuture("Must specify at least one contact number");
        }

        Set<String> contactAddresses = new HashSet<String>();

        for (String mobileNumber : contactMobileNumbers) {
            contactAddresses.add(getAddress(mobileNumber));
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("contacts", contactAddresses);
        params.put("body", body);

        try {
            return executeAsync(ZipwhipNetworkSupport.MESSAGE_SEND, params, true, new InputRunnable<ParsableServerResponse<List<MessageToken>>>() {
                @Override
                public void run(ParsableServerResponse<List<MessageToken>> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseMessageTokens(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<MessageListResult> listMessages(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.MESSAGE_LIST, params, true, new InputRunnable<ParsableServerResponse<MessageListResult>>() {
                @Override
                public void run(ParsableServerResponse<MessageListResult> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseMessagesListResult(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<MessageListResult> listMessages(String deviceAddress, final int start, final int limit) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("start", Integer.toString(start));
        params.put("limit", Integer.toString(limit));

        try {
            return executeAsync(ZipwhipNetworkSupport.MESSAGE_LIST, params, true, new InputRunnable<ParsableServerResponse<MessageListResult>>() {
                @Override
                public void run(ParsableServerResponse<MessageListResult> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseMessagesListResult(parsableServerResponse.getServerResponse()));
                        parsableServerResponse.getFuture().getResult().setStart(start);
                        parsableServerResponse.getFuture().getResult().setLimit(limit);
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Contact> saveUser(String deviceAddress, Contact user) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        if (user == null) {
            return invalidArgumentFailureFuture("User is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        if (StringUtil.exists(user.getFirstName())) {
            params.put("firstName", user.getFirstName());
        }
        if (StringUtil.exists(user.getLastName())) {
            params.put("lastName", user.getLastName());
        }
        if (StringUtil.exists(user.getNotes())) {
            params.put("notes", user.getNotes());
        }

        try {
            return executeAsync(ZipwhipNetworkSupport.USER_SAVE, params, true, new InputRunnable<ParsableServerResponse<Contact>>() {
                @Override
                public void run(ParsableServerResponse<Contact> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseUserAsContact(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> readMessages(String deviceAddress, Set<String> ids) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("message", ids);

        try {
            return executeAsync(ZipwhipNetworkSupport.MESSAGE_READ, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> deleteMessages(String deviceAddress, Set<String> ids) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("message", ids);

        try {
            return executeAsync(ZipwhipNetworkSupport.MESSAGE_DELETE, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> readConversation(String deviceAddress, String fingerprint) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("fingerprint", fingerprint);

        try {
            return executeAsync(ZipwhipNetworkSupport.CONVERSATION_READ, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> deleteConversation(String deviceAddress, String fingerprint) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("fingerprint", fingerprint);

        try {
            return executeAsync(ZipwhipNetworkSupport.CONVERSATION_DELETE, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<List<Conversation>> listConversations(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.CONVERSATION_LIST, params, true, new InputRunnable<ParsableServerResponse<List<Conversation>>>() {
                @Override
                public void run(ParsableServerResponse<List<Conversation>> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseConversations(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Contact> saveContact(String deviceAddress, Contact contact) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        if (contact == null) {
            return invalidArgumentFailureFuture("Contact is a required argument");
        }

        if (StringUtil.isNullOrEmpty(contact.getAddress())) {
            return invalidArgumentFailureFuture("Contact.address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        if (StringUtil.exists(contact.getFirstName())) {
            params.put("firstName", contact.getFirstName());
        }
        if (StringUtil.exists(contact.getLastName())) {
            params.put("lastName", contact.getLastName());
        }
        if (StringUtil.exists(contact.getAddress())) {
            params.put("address", getAddress(contact.getAddress()));
        }
        if (StringUtil.exists(contact.getNotes())) {
            params.put("notes", contact.getNotes());
        }

        try {
            return executeAsync(ZipwhipNetworkSupport.CONTACT_SAVE, params, true, new InputRunnable<ParsableServerResponse<Contact>>() {
                @Override
                public void run(ParsableServerResponse<Contact> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseContact(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> deleteContacts(String deviceAddress, Set<String> contactMobileNumbers) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        Set<String> contactAddresses = new HashSet<String>();

        for (String mobileNumber : contactMobileNumbers) {
            contactAddresses.add(getAddress(mobileNumber));
        }

        params.put("contact", contactAddresses);

        try {
            return executeAsync(ZipwhipNetworkSupport.CONTACT_DELETE, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<List<Contact>> listContacts(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.CONTACT_LIST, params, true, new InputRunnable<ParsableServerResponse<List<Contact>>>() {
                @Override
                public void run(ParsableServerResponse<List<Contact>> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseContacts(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Contact> getContact(String deviceAddress, String contactMobileNumber) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }
        if (StringUtil.isNullOrEmpty(contactMobileNumber)) {
            return invalidArgumentFailureFuture("Contact mobileNumber is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("address", getAddress(contactMobileNumber));

        try {
            return executeAsync(ZipwhipNetworkSupport.CONTACT_GET, params, true, new InputRunnable<ParsableServerResponse<Contact>>() {
                @Override
                public void run(ParsableServerResponse<Contact> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseContact(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> textlineProvision(String phoneNumber) throws Exception {

        if (StringUtil.isNullOrEmpty(phoneNumber)) {
            return invalidArgumentFailureFuture("Phone number is a required argument");
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("phoneNumber", phoneNumber);

        try {
            return executeAsync(ZipwhipNetworkSupport.TEXTLINE_PROVISION, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }

    }

    @Override
    public ObservableFuture<Void> textlineEnroll(String phoneNumber, String email) throws Exception {

        if (StringUtil.isNullOrEmpty(phoneNumber)) {
            return invalidArgumentFailureFuture("Phone number is a required argument");
        }
        if (StringUtil.isNullOrEmpty(email)) {
            return invalidArgumentFailureFuture("Email is a required argument");
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("phoneNumber", phoneNumber);
        params.put("email", email);

        try {
            return executeAsync(ZipwhipNetworkSupport.TEXTLINE_ENROLL, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }

    }

    @Override
    public ObservableFuture<Void> textlineUnenroll(String phoneNumber) throws Exception {

        if (StringUtil.isNullOrEmpty(phoneNumber)) {
            return invalidArgumentFailureFuture("Phone number is a required argument");
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("phoneNumber", phoneNumber);

        try {
            return executeAsync(ZipwhipNetworkSupport.TEXTLINE_UNENROLL, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }

    }

    private <T> ObservableFuture<T> invalidArgumentFailureFuture(String message) {
        return failureFuture(new IllegalAccessException(message));
    }

    private <T> ObservableFuture<T> failureFuture(Exception e) {
        MutableObservableFuture<T> future = new DefaultObservableFuture<T>(this);
        future.setFailure(e);
        return future;
    }

    private String getDeviceAddress(String deviceAddress) {
        return Address.encode("device", Address.stripToMobileNumber(deviceAddress), "0");
    }

    private String getAddress(String address) {
        return Address.encode("ptn", Address.stripToMobileNumber(address));
    }

    @Override
    protected void onDestroy() {

    }

}
