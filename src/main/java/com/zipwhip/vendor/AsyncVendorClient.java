package com.zipwhip.vendor;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.dto.*;
import com.zipwhip.api.dto.EnrollmentResult;
import com.zipwhip.api.response.MessageListResult;
import com.zipwhip.concurrent.ObservableFuture;

import java.util.*;

/**
 * You must be authenticated as a Vendor to use this API. It will reject your request if you are authenticated as a user.
 * <p/>
 * Provides administrator tools for a Vendor to connect to and communicate with Zipwhip.
 */
public interface AsyncVendorClient {

    /**
     * Set the connection to the Zipwhip REST API.
     *
     * @param connection A transport to Zipwhip.
     */
    void setConnection(ApiConnection connection);

    /**
     * Get the connection to the Zipwhip REST API.
     *
     * @return A transport to Zipwhip.
     */
    ApiConnection getConnection();

    /**
     * Enroll a user in Zipwhip. If the user is already enrolled the this call will have no effect.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user who wishes to be enrolled in Zipwhip.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the enrollment.
     */
    ObservableFuture<EnrollmentResult> enrollUser(String deviceAddress);

    /**
     * Unenroll a user from Zipwhip. When a user is unenrolled their account is removed from Zipwhip.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user who wishes to be unenrolled from Zipwhip.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<Void> deactivateUser(String deviceAddress);

    /**
     * Query Zipwhip to see if a user already exists.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user to query if they are enrolled in Zipwhip.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     *         The result will be {@code TRUE} if the user exists, {@code FALSE} otherwise.
     */
    ObservableFuture<Boolean> userExists(String deviceAddress);

    /**
     * This method will tell the Zipwhip Network to send an SMS to the users phone containing a URL. The URL is clicked
     * Zipwhip will detect if the user's phone is supported by Carbon, and if it is the user will be redirected to a
     * download of the Carbon software. If the user's phone is not supported an error message is presented to the user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user who wishes to receive a DeviceCarbon link to their phone.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<Void> suggestCarbon(String deviceAddress);

    /**
     * This method will query the Zipwhip Network to as to whether Device Carbon is installed on the user's phone.
     * Returns a {@code true} result if it is installed, otherwise {@code false}.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user who wishes to receive a DeviceCarbon link to their phone.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<Boolean> carbonInstalled(String deviceAddress);

    /**
     * This method will query the Zipwhip Network to as to whether Device Carbon is enabled on the user's phone.
     * Returns a {@code true} result if it is enabled, otherwise {@code false}.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user who wishes to receive a DeviceCarbon link to their phone.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<Boolean> carbonEnabled(String deviceAddress);

    /**
     * Send a message via the Zipwhip network. The message is from the user represented by the {@code deviceAddress}.
     * The details of the message including the recipient is contained in the {@code Message} object.
     *
     * @param deviceAddress   The device address (device:/5555555555/0) of the user.
     * @param friendAddresses A list of mobile numbers of the recipients of the message.
     * @param body            The text of the message to be sent.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<List<MessageToken>> sendMessage(String deviceAddress, Set<String> friendAddresses, String body);

    /**
     * Send a message via the Zipwhip network. The message is from the user represented by the {@code deviceAddress}.
     * The details of the message including the recipient is contained in the {@code Message} object.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param friendAddress A mobile number of the recipients of the message.
     * @param body          The text of the message to be sent.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<List<MessageToken>> sendMessage(String deviceAddress, String friendAddress, String body);

    /**
     * List the messages for a given user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<MessageListResult> listMessages(String deviceAddress);

    /**
     * List the messages for a given user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param start         Index start from the beginning of the message list, zero based.
     * @param limit         Page size, maximum number of messages that will be returned.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<MessageListResult> listMessages(String deviceAddress, int start, int limit);

    /**
     * Save details for an existing user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param user          The user to save or update.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     *         The result is the saved user.
     */
    ObservableFuture<Contact> saveUser(String deviceAddress, Contact user);

    /**
     * Mark a set of messages, identified by their IDs, for a given user as read.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param ids           A list of message ids for the messages to be marked as read.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<Void> readMessages(String deviceAddress, Set<String> ids);

    /**
     * Delete a set of messages, identified by their IDs, for a given user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param ids           A list of message ids for the messages to be deleted.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<Void> deleteMessages(String deviceAddress, Set<String> ids);

    /**
     * Mark a conversation identified by its fingerprint as read, for a given user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param fingerprint   The fingerprint of the conversation to be marked as read.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<Void> readConversation(String deviceAddress, String fingerprint);

    /**
     * Mark a set of messages, identified by their fingerprint, for a given user as read.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param fingerprint   The fingerprint of the conversation to be deleted.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<Void> deleteConversation(String deviceAddress, String fingerprint);

    /**
     * Query the list of conversations for a given user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<List<Conversation>> listConversations(String deviceAddress);

    /**
     * Save the details of a contact for a given user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param contact       The contact to be saved in the user's contact list.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<Contact> saveContact(String deviceAddress, Contact contact);

    /**
     * Delete the details of a set of contacts for a given user.
     *
     * @param deviceAddress        The device address (device:/5555555555/0) of the user.
     * @param contactMobileNumbers A list of mobile numbers for the contacts to be deleted.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<Void> deleteContacts(String deviceAddress, Set<String> contactMobileNumbers);

    /**
     * Query a user's contact list.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<List<Contact>> listContacts(String deviceAddress);

    /**
     * Query a user's contact list for the specified contact.
     *
     * @param deviceAddress       The device address (device:/5555555555/0) of the user.
     * @param contactMobileNumber The mobile number of the contact to be queried.
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     */
    ObservableFuture<Contact> getContact(String deviceAddress, String contactMobileNumber);

    /**
     * @param phoneNumber
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     * @throws Exception
     */
    ObservableFuture<Void> textlineProvision(String phoneNumber) throws Exception;

    /**
     * @param phoneNumber
     * @param email
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     * @throws Exception
     */
    ObservableFuture<Void> textlineEnroll(String phoneNumber, String email) throws Exception;

    /**
     * @param phoneNumber
     * @return A {@code ObservableFuture} that will asynchronously report the result of the call.
     * @throws Exception
     */
    ObservableFuture<Void> textlineUnenroll(String phoneNumber) throws Exception;

}
