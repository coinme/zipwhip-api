package com.zipwhip.api;

import com.zipwhip.api.signals.Event;
import com.zipwhip.api.signals.ImmediatelyFailingSignalsSubscribeActor;
import com.zipwhip.api.signals.MockSignalSubscribeActor;
import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.signals2.presence.Presence;
import com.zipwhip.signals2.presence.UserAgent;
import com.zipwhip.signals2.presence.UserAgentCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michael
 * @date 5/6/2014
 */
public class SimpleZipwhipClientBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleZipwhipClientBootstrap.class);

    public static void main(String[] args) throws Exception {
        SimpleZipwhipClient client = new SimpleZipwhipClient();
        String sessionKey = "enter your sessionKey here";

//        client.setSignalsSubscribeActor(new MockSignalSubscribeActor());
        client.setUserAgent(new UserAgent(UserAgentCategory.Desktop, "TesterApp"));

        client.onSignalsConnectionChanged(new Observer<SimpleZipwhipClient.ConnectionState>() {
            @Override
            public void notify(Object sender, SimpleZipwhipClient.ConnectionState item) {
                LOGGER.info("ConnectionChanged: " + item);
            }
        });

        client.onSignalsPresenceChanged(new Observer<Event<Presence>>() {
            @Override
            public void notify(Object sender, Event<Presence> item) {
                LOGGER.info("PresenceChanged: " + item);
            }
        });

        client.onSignalReceived(new Observer<DeliveredMessage>() {
            @Override
            public void notify(Object sender, DeliveredMessage item) {
                LOGGER.info("SignalReceived: " + item);
            }
        });

        // This isn't a synchronous call....
        ObservableFuture<Void> future = client.connect(sessionKey);

        future.await();

        if (!future.isSuccess()) {
            throw new RuntimeException();
        }

        // We need to block somehow?

    }
}
