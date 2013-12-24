package com.zipwhip.api;

import com.zipwhip.api.dto.*;
import com.zipwhip.api.settings.SettingsStore;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.signals2.presence.Presence;
import com.zipwhip.signals2.presence.UserAgentCategory;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Implementations of this class provide a high level way to communicate with the Zipwhip web API.
 * The communication takes place over a {@code ApiConnection} connection.
 *
 * @author Michael
 */
public interface ZipwhipClient extends Destroyable {

    /**
     * Send a message via Zipwhip.
     *
     * @param message A {@code Message} object from which to send the message.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(Message message) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address Zipwhip {@link Address} scheme.
     * @param body    The body of the message to be sent.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(Address address, String body) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address  Zipwhip {@link Address} scheme.
     * @param body     The body of the message to be sent.
     * @param fromName The name of the sender of the message.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(Address address, String body, String fromName) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address The address, generally the mobile number, of the message recipient.
     * @param body    The body of the message to be sent.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(String address, String body) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address The address, generally the mobile number, of the message recipient.
     * @param body    The body of the message to be sent.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(Collection<String> address, String body) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address  The address, generally the mobile number, of the message recipient.
     * @param body     The body of the message to be sent.
     * @param fromName The name of the sender of the message.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(Collection<String> address, String body, String fromName) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address       The address, generally the mobile number, of the message recipient.
     * @param body          The body of the message to be sent.
     * @param fromName      The name of the sender of the message.
     * @param advertisement A code indicating to Zipwhip what should be appended to the message.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(Collection<String> address, String body, String fromName, String advertisement) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address       The address, generally the mobile number, of the message recipient.
     * @param body          The body of the message to be sent.
     * @param fromName      The name of the sender of the message.
     * @param advertisement A code indicating to Zipwhip what should be appended to the message.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(String address, String body, String fromName, String advertisement) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address     The address, generally the mobile number, of the message recipient.
     * @param body        The body of the message to be sent.
     * @param fromAddress The send strategy.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(String address, String body, int fromAddress) throws Exception;

    /**
     * Send an MMS message via Zipwhip.
     * <p/>
     * This method will send an MMS with attachments that have been pre-uploaded to Zipwhip via
     * {@code saveHostedContent}. The argument {@code storageKeys} are the result of that method.
     *
     * @param addresses The address, generally the mobile number, of the message recipient.
     * @param body      The body of the message to be sent.
     * @param urls      A list of public URLs to attach to the message.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(Collection<String> addresses, String body, List<String> urls) throws Exception;

    /**
     * Create a new group.
     *
     * @param type          The type of group, eg. reply-all.
     * @param advertisement A code indicating to Zipwhip what should be appended to the message.
     * @return A {@link Contact} representing the new group.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Contact saveGroup(String type, String advertisement) throws Exception;

    /**
     * Returns the Group for the provided device address
     *
     * @param address - the group address
     * @return
     */
    Group getGroup(String address) throws Exception;

    /**
     * Create a new group.
     *
     * @return A {@link Contact} representing the new group.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Contact saveGroup() throws Exception;

    /**
     * Save or update the user's information.
     *
     * @param contact A {@link Contact} object representing the user to be saved.
     * @return The {@link Contact} object representing the user that has been saved.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Contact saveUser(Contact contact) throws Exception;

    /**
     * Saves or updates the user information.  For all values for which null is passed in, that value will remain unchanged,
     * relative to the website.
     *
     * @param firstName The first name of the user.
     * @param lastName  The last name of the user.
     * @param email     The email of the user.
     * @param phoneKey  Indicates the user's phone type.
     * @param location  The user's location.
     * @param notes     Free text.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    void saveUser(String firstName, String lastName, String email, String phoneKey, String location, String notes) throws Exception;

    /**
     * Get the user associated with the currently authenticated sessionKey for this client.
     *
     * @return The the user associated with the currently authenticated sessionKey for this client.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    User getUser() throws Exception;

    /**
     * Returns a Message object
     *
     * @param uuid - message uuid
     * @return A Message DTO matching the uuid.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     * @deprecated Use {@code getMessage(Long id)}
     */
    Message getMessage(String uuid) throws Exception;

    /**
     * Returns a Message object
     *
     * @param id - message id
     * @return A Message DTO matching the id.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Message getMessage(Long id) throws Exception;

    /**
     * @return A list of all {@link Device}s associated with the user.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    List<Device> listDevices() throws Exception;

    /**
     * @return A list of the most recent {@link Conversation}s associated with the user, up to the server's predefined limit.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    List<Conversation> listConversations() throws Exception;

    /**
     * @param limit The maximum limit of how many conversations to return.
     * @return A list of the most recent {@link Conversation}s associated with the user, up to the specified limit.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    List<Conversation> listConversations(int limit) throws Exception;

    /**
     * @param start Where to start list conversations.  (Zero indexed)
     * @param limit The maximum limit of how many conversations to return.
     * @return A list of the most recent {@link Conversation}s associated with the user, up to the specified limit.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    List<Conversation> listConversations(int start, int limit) throws Exception;

    /**
     * @return A list of all {@link Contact}s associated with the supplied user.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response, or the server returns a failure message.
     */
    List<Contact> listContacts() throws Exception;

    /**
     * @param start Where to start list contacts.  (Zero indexed)
     * @param limit The maximum limit of how many contacts to return.
     * @return A list of all {@link Contact}s associated with the supplied user.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response, or the server returns a failure message.
     */
    List<Contact> listContacts(int start, int limit) throws Exception;

    /**
     * @param fingerprint The fingerprint of the conversation that you wish to mark as read.
     * @return A boolean which represents whether or not the operation completed successfully.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    boolean readConversation(String fingerprint) throws Exception;

    /**
     * @param fingerprint The fingerprint of the conversation that you wish to mark as read.
     * @return A boolean which represents whether or not the operation completed successfully.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    boolean deleteConversation(String fingerprint) throws Exception;

    /**
     * Returns the contact for the provided contact id.
     *
     * @param contactId The id of the contact to be deleted.
     * @return A boolean which represents whether or not the operation completed successfully.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    boolean deleteContact(long contactId) throws Exception;

    /**
     * Returns the most recent messages for the user, up to a limit maintained by the zipwhip server.
     *
     * @param fingerprint The fingerprint for which you wish to load messages.
     * @return A list consisting of the most recent messages associated with the supplied fingerprint.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    List<Message> listMessagesByFingerprint(String fingerprint) throws Exception;

    /**
     * Returns the most recent messages for the supplied conversation, up to a the supplied limit.
     *
     * @param fingerprint The fingerprint for which you wish to load messages.
     * @param limit       The maximum number of messages that this call will return.
     * @return A list consisting of the most recent messages associated with the supplied fingerprint.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    List<Message> listMessagesByFingerprint(String fingerprint, int limit) throws Exception;

    /**
     * Returns the most recent messages for the supplied conversation, up to a the supplied limit.
     *
     * @param fingerprint The fingerprint for which you wish to load messages.
     * @param start       Where in the list to start. (Zero indexed)
     * @param limit       The maximum number of messages that this call will return.
     * @return A list consisting of the most recent messages associated with the supplied fingerprint.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    List<Message> listMessagesByFingerprint(String fingerprint, int start, int limit) throws Exception;

    /**
     * @return A list consisting of the most recent messages associated with the user.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    List<Message> listMessages() throws Exception;

    /**
     * Returns the most recent messages for the user, up to the supplied limit.
     *
     * @param limit The maximum number of messages that this call will return.
     * @return A list consisting of the most recent messages associated with this user.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    List<Message> listMessages(int limit) throws Exception;

    /**
     * Returns the most recent messages for the user, up to the supplied limit.
     *
     * @param start Where in the list to start. (Zero indexed)
     * @param limit The maximum number of messages that this call will return.
     * @return A list consisting of the most recent messages associated with this user.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    List<Message> listMessages(int start, int limit) throws Exception;

    /**
     * Delete messages by their corresponding UUIDs.
     *
     * @param uuids A list of message uuids to delete.
     * @return True for a successful delete otherwise false.
     * @throws Exception If an error occurred while sending or parsing the response.
     * @deprecated use {@code readMessage(List id)}
     */
    boolean messageRead(List<String> uuids) throws Exception;

    /**
     * Read messages by their corresponding IDs.
     *
     * @param ids A list of message ids to read.
     * @return True for a successful read otherwise false.
     * @throws Exception If an error occurred while sending or parsing the response.
     */
    boolean readMessage(List<Long> ids) throws Exception;

    /**
     * Read messages by their corresponding UUIDs.
     *
     * @param uuids A list of message uuids to mark as read.
     * @return True for a successful read otherwise false.
     * @throws Exception If an error occurred while sending or parsing the response.
     * @deprecated use {@code deleteMessage(List ids)}
     */
    boolean messageDelete(List<String> uuids) throws Exception;

    /**
     * Delete messages by their corresponding IDs.
     *
     * @param ids A list of message ids to delete.
     * @return True for a successful delete otherwise false.
     * @throws Exception If an error occurred while sending or parsing the response.
     */
    boolean deleteMessage(List<Long> ids) throws Exception;

    /**
     * Returns the contact for the provided contact id.
     *
     * @param id The id of the contact.
     * @return A Connect DTO matching the id.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    Contact getContact(long id) throws Exception;

    /**
     * Returns the contact for the provided mobile number.
     *
     * @param mobileNumber The mobile number of the contact to get.
     * @return contact The contact corresponding to the mobile number.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    Contact getContact(String mobileNumber) throws Exception;

    /**
     * Query for the list of presences for your session
     *
     * @param category The category of presence to query for
     * @return List of presences for category and session
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    List<Presence> getPresence(UserAgentCategory category) throws Exception;

    /**
     * Send a signal via Zipwhip SignalServer.
     * Generally this is for debug since the SignalServer protocol is proprietary.
     *
     * @param scope   The scope of the signal, ie device.
     * @param channel The channel the signal is on.
     * @param event   The event of the signal.
     * @param payload The content of the signal.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    void sendSignal(String scope, String channel, String event, String payload) throws Exception;

    /**
     * A debug call to generate a SIGNAL_VERIFICATION command back to the client
     * associated with the {@param clientId}.
     *
     * @param clientId The client ID to send the SIGNAL_VERIFICATION to.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    void sendSignalsVerification(String clientId) throws Exception;

    /**
     * Save a new contact for the user or update an existing contact.
     *
     * @param address   The address of the contact, generally mobile number.
     * @param firstName Contact's first name.
     * @param lastName  Contact's last name.
     * @param phoneKey  Contact's phone type.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    void saveContact(String address, String firstName, String lastName, String phoneKey) throws Exception;

    /**
     * Save a new contact for the user or update an existing contact.
     *
     * @param address   The address of the contact, generally mobile number.
     * @param firstName Contact's first name.
     * @param lastName  Contact's last name.
     * @param phoneKey  Contact's phone type.
     * @param notes     Free text.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    void saveContact(String address, String firstName, String lastName, String phoneKey, String notes) throws Exception;

    /**
     * Save a new contact for the user or update an existing contact.
     *
     * @param address   The address of the contact, generally mobile number.
     * @param firstName Contact's first name.
     * @param lastName  Contact's last name.
     * @param phoneKey  Contact's phone type.
     * @param notes     Free text.
     * @param location  Contact's location
     * @param email     Contact's e-mail address.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    void saveContact(String address, String firstName, String lastName, String phoneKey, String notes, String location, String email) throws Exception;

    /**
     * Add a member to an existing group.
     *
     * @param groupAddress   The address of the group to add a new member to.
     * @param contactAddress The address, mobile number, of the new contact.
     * @return A {@link Contact} representing the new group member.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Contact addMember(String groupAddress, String contactAddress) throws Exception;

    /**
     * Add a member to an existing group.
     *
     * @param groupAddress   The address of the group to add a new member to.
     * @param contactAddress The address, mobile number, of the new contact.
     * @param firstName      Contact's first name.
     * @param lastName       Contact's last name.
     * @param phoneKey       Contact's phone type.
     * @param notes          Free text.
     * @return A {@link Contact} representing the new group member.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Contact addMember(String groupAddress, String contactAddress, String firstName, String lastName, String phoneKey, String notes) throws Exception;

    /**
     * Toggles the on/off value of Device Carbon in the cloud. The cloud holds the master value that Device Carbon uses
     * to override any other value it has.
     *
     * @param enabled:     turn Device Carbon on/off in the cloud
     * @param versionCode: What version of Device Carbon is being used?
     * @throws Exception if an error occurs communicating with Zipwhip.
     */
    void carbonEnable(boolean enabled, Integer versionCode) throws Exception;

    /**
     * Returns the on/off state Device Carbon should be in according to the cloud.
     *
     * @param enabled: The on/off state Device Carbon is currently in
     * @return What state the cloud thinks device carbon is in
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Boolean carbonEnabled(boolean enabled, Integer versionCode) throws Exception;

    /**
     * Register Device Carbon for Push Notifications from Google
     *
     * @param registrationId - Google provided registrationId to send push notifications too
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    void carbonRegister(String registrationId) throws Exception;

    /**
     * Informs Zipwhip of Device Carbon usage statistics
     *
     * @param totalPhoneMessages - total messages sent/receive on the device
     * @throws Exception
     */
    void carbonStats(int totalPhoneMessages) throws Exception;

    boolean acceptedTCs() throws Exception;

    /**
     * Initiates the sign up process to:
     * 1) Enroll a new account if one doesn't exist
     * 2) Create necessary subscriptions
     * 3) Eventually return a valid session key for this device
     *
     * @param mobileNumber: mobile number of the account
     * @param portal:       product that is retrieving a session
     * @return clientId that is used to finish the challenge process
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    String sessionChallenge(String mobileNumber, String portal) throws Exception;

    /**
     * Finishes the challenge process and returns a session key
     *
     * @param clientId:      clientId returned by the original sessionChallenge call
     * @param securityToken: The random string that is sent in an ".signup verify" sms to the phone
     * @param portal:        product line to customize the user account for
     * @param arguments:     any extra arguments for the cloud to react to (previously this was ".signup devicecarbonall")
     * @param userAgent:     Device's user agent
     * @return A session key
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    String sessionChallengeConfirm(String clientId, String securityToken, String portal, String arguments, String userAgent) throws Exception;

    /**
     * @param packageName The name of the subscription package to unenroll.
     * @return True if the unenrollment was successful otherwise false.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    boolean userUnenroll(String packageName) throws Exception;

    /**
     * Query Zipwhip Face Ecosystem for a user's preferred profile name.
     *
     * @param mobileNumber The mobile number of the user you wish to query.
     * @return The user's full name if it exists or empty string.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    String getFaceName(String mobileNumber) throws Exception;


    /**
     * Bulk Query Zipwhip Face Ecosystem for a users' preferred profile names.
     *
     * @param phoneNumbers Collection of phone numbers you wish to query.
     * @return Map of phone number/full name
     * @throws Exception                if an error occurs communicating with Zipwhip or parsing the response.
     * @throws IllegalArgumentException if the phoneNumber size is larger than 100.
     */
    public Map<String, String> getFaceName(Collection<String> phoneNumbers) throws Exception;

    /**
     * Query Zipwhip Face Ecosystem for a user's preferred profile image.
     *
     * @param mobileNumber The mobile number of the user you wish to query.
     * @param thumbnail    true if you want a thumbnail, false for the full image
     * @return A byte[] of the user's image.
     * @throws Exception if an error occurs communicating with Zipwhip or the image is not found.
     */
    byte[] getFaceImage(String mobileNumber, boolean thumbnail) throws Exception;

    /**
     * Query Zipwhip Face Ecosystem for a user's preferred profile image.
     *
     * @param mobileNumber The mobile number of the user you wish to query.
     * @param size         the size of thumbnail in pixels
     * @return A byte[] of the user's image.
     * @throws Exception if an error occurs communicating with Zipwhip or the image is not found.
     */
    byte[] getFaceImage(String mobileNumber, int size) throws Exception;

    /**
     * Check if the phone number has a face eco image
     *
     * @param phoneNumbers Collection of phone numbers you wish to query.
     * @return Map of phone number/boolean (exists or not)
     * @throws Exception                if an error occurs communicating with Zipwhip or parsing the response.
     * @throws IllegalArgumentException if the phoneNumber size is larger than 100.
     */
    public Map<String, Boolean> hasFaceImage(Collection<String> phoneNumbers) throws Exception;

    /**
     * Query for a message's MMS attachment descriptors.
     *
     * @param messageId The id of the message to query for attachments.
     * @return A list of attachment descriptors or an empty list if no attachments are found,
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    List<MessageAttachment> listAttachments(Long messageId) throws Exception;

    /**
     * Get a single piece of hosted using its storage key. The key is retrieved by calling {@code listAttachments}.
     *
     * @param storageKey The storage key of the content to query. Retrieved by calling {@code listAttachments}
     * @return A byte[] containing the requested content.
     * @throws Exception if an error occurs communicating with Zipwhip or the content is not found.
     */
    byte[] getHostedContent(String storageKey) throws Exception;

    /**
     * Upload one or more files into Zipwhip hosted content servers.
     *
     * @param files A list of files to upload to Zipwhip HostedContent.
     * @return A map from file name to the HostedContent storage key.
     * @throws Exception if an error occurs communicating with Zipwhip or the content is not found.
     */
    Map<String, String> saveHostedContent(List<File> files) throws Exception;

    /**
     * Reserve a tiny url in the Zipwhip TinyUrl system. The resulting TinyUrl
     * can be used to upload content to via {@code saveTinyUrl}.
     *
     * @return The tinyUrl.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the result.
     */
    TinyUrl reserveTinyUrl() throws Exception;

    /**
     * Save content into a tinyUrl with the Zipwhip TinyUrl system.
     *
     * @param key      The storage key returned by the call to {@code reserveTinyUrl}.
     * @param mimeType An optional mime-type for the file. If not provided it will default to multipart/mixed.
     * @param file     A file to be uploaded into the tinyUrl.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the result.
     */
    boolean saveTinyUrl(String key, String mimeType, File file) throws Exception;

    /**
     * A connection to Zipwhip over a medium.
     *
     * @return the current connection
     */
    ApiConnection getConnection();

    /**
     * @param connection the connection to use
     */
    void setConnection(ApiConnection connection);

    /**
     * Getter for the SignalProvider. SignalProvider manages the connection to the SignalServer
     * and provides events when messages are received or connection state changes.
     *
     * @return An implementation of SignalProvider or null if none has been set.
     */
    SignalProvider getSignalProvider();

    /**
     * Setter for the SignalProvider. SignalProvider manages the connection to the SignalServer
     * and provides events when messages are received or connection state changes.
     *
     * @param provider An implementation of SignalProvider.
     */
    void setSignalProvider(SignalProvider provider);

    /**
     * Get the setting settingsStore
     *
     * @return the setting settingsStore
     */
    SettingsStore getSettingsStore();

    /**
     * Set the setting settingsStore
     *
     * @param store the setting settingsStore
     */
    void setSettingsStore(SettingsStore store);

    /**
     * Query Group image by address.
     *
     * @param address group address.
     * @param size    thumbnail size.
     * @return A byte[] of the group's image.
     * @throws Exception if an error occurs communicating with Zipwhip or the image is not found.
     */
    byte[] getGroupImage(String address, int size) throws Exception;
}
