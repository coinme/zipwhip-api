package com.zipwhip.api.signals;

import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.events.Observer;
import com.zipwhip.signals2.SignalConversation;
import com.zipwhip.signals2.SignalMessage;
import com.zipwhip.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Date: 9/25/13
 * Time: 9:55 AM
 *
 * @author Michael
 * @version 1
 */
public class SignalObserver implements Observer<DeliveredMessage>  {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignalObserver.class);

    @Override
    public void notify(Object sender, DeliveredMessage item) {
        String type = item.getType();
        String event = item.getEvent();
        long timestamp = item.getTimestamp();
        Set<String> subscriptionIds = item.getSubscriptionIds();

        if (StringUtil.equalsIgnoreCase(type, "message")) {
            long messageId = Long.valueOf(item.getId());

            if (StringUtil.equalsIgnoreCase(event, "delete")) {
                onMessageDeleted(subscriptionIds, timestamp, messageId);
                return;
            } else if (StringUtil.equalsIgnoreCase(event, "receive")) {
                onMessageReceived(subscriptionIds, timestamp, (SignalMessage) item.getContent());
                return;
            } else if (StringUtil.equalsIgnoreCase(event, "read")) {
                onMessageRead(subscriptionIds, timestamp, messageId);
                return;
            } else if (StringUtil.equalsIgnoreCase(event, "send")) {
                onMessageRead(subscriptionIds, timestamp, messageId);
                return;
            }
        } else if (StringUtil.equalsIgnoreCase(type, "conversation")) {
            if (StringUtil.equalsIgnoreCase(event, "change")) {
                onConversationChanged(subscriptionIds, timestamp, (SignalConversation)item.getContent());
                return;
            }
        }

        onSignalReceived(item);
    }

    private void onConversationChanged(Set<String> subscriptionIds, long timestamp, SignalConversation content) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("onConversationChanged %s, %s, %s", subscriptionIds, timestamp, content));
        }
    }

    private void onMessageRead(Set<String> subscriptionIds, long timestamp, long messageId) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("onMessageRead %s, %s, %s", subscriptionIds, timestamp, messageId));
        }
    }

    private void onMessageReceived(Set<String> subscriptionIds, long timestamp, SignalMessage content) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("onMessageReceived %s, %s, %s", subscriptionIds, timestamp, content));
        }
    }

    public void onSignalReceived(DeliveredMessage message) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("onSignalReceived %s", message));
        }
    }

    public void onMessageDeleted(Set<String> subscriptionIds, long timestamp, long messageId) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("onMessageDeleted %s, %s, %s", subscriptionIds, timestamp, messageId));
        }
    }

}
