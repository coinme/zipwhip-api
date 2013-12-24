package com.zipwhip.api.response;

import com.zipwhip.api.dto.*;
import com.zipwhip.signals2.presence.Presence;
import com.zipwhip.util.Parser;

import java.util.List;
import java.util.Map;

/**
 * Date: Jul 18, 2009
 * Time: 10:22:28 AM
 * <p/>
 * Will parse out objects from a specific data format. Currently the only supported format is JSON.
 */
public interface ResponseParser extends Parser<String, ServerResponse> {

    Message parseMessage(ServerResponse serverResponse) throws Exception;

    MessageListResult parseMessagesListResult(ServerResponse serverResponse) throws Exception;

    List<Message> parseMessages(ServerResponse serverResponse) throws Exception;

    List<Message> parseMessagesFromConversation(ServerResponse serverResponse) throws Exception;

    Device parseDevice(ServerResponse serverResponse) throws Exception;

    List<Device> parseDevices(ServerResponse serverResponse) throws Exception;

    String parseString(ServerResponse serverResponse) throws Exception;

    Contact parseContact(ServerResponse serverResponse) throws Exception;

    Group parseGroup(ServerResponse serverResponse) throws Exception;

    Contact parseUserAsContact(ServerResponse serverResponse) throws Exception;

    User parseUser(ServerResponse serverResponse) throws Exception;

    List<Contact> parseContacts(ServerResponse serverResponse) throws Exception;

    Conversation parseConversation(ServerResponse serverResponse) throws Exception;

    List<Conversation> parseConversations(ServerResponse serverResponse) throws Exception;

    DeviceToken parseDeviceToken(ServerResponse serverResponse) throws Exception;

    List<MessageToken> parseMessageTokens(ServerResponse serverResponse) throws Exception;

    List<Presence> parsePresence(ServerResponse serverResponse) throws Exception;

    List<MessageAttachment> parseAttachments(ServerResponse serverResponse) throws Exception;

    EnrollmentResult parseEnrollmentResult(ServerResponse serverResponse) throws Exception;

    String parseFaceName(ServerResponse serverResponse) throws Exception;

    Map<String, String> parseFaceNames(ServerResponse serverResponse) throws Exception;

    Map<String, String> parseHostedContentSave(ServerResponse serverResponse) throws Exception;

    TinyUrl parseTinyUrl(ServerResponse serverResponse) throws Exception;

    Map<String, Boolean> parseFaceImages(ServerResponse serverResponse) throws Exception;
}
