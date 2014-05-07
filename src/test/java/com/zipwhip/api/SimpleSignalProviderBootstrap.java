package com.zipwhip.api;

import com.zipwhip.api.settings.PreferencesSettingsStore;
import com.zipwhip.api.signals.Event;
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
public class SimpleSignalProviderBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSignalProviderBootstrap.class);

    public static void main(String[] args) throws Exception {
        SimpleSignalProvider client = new SimpleSignalProvider();
        String sessionKey = "a18eb775-9bf8-4166-8187-0f5dfc526d67:375";

        // The default SettingsStore is the MemorySettingsStore. But that would lose your clientId if you restarted
        // the app. My recommendation is to use either a PreferencesSettingsStore, or roll your own for a MySql database.
        // Maybe the API could be "new DataSourceSettingsStore(dataSource, mobileNumber)"
        // This example (PreferencesSettingsStore) cannot be shared amongst many accounts. They would overwrite
        // each others data.
        client.setSettingsStore(new PreferencesSettingsStore());
        client.setUserAgent(new UserAgent(UserAgentCategory.Desktop, "TesterApp"));

        client.onSignalsConnectionChanged(new Observer<SimpleSignalProvider.ConnectionState>() {
            @Override
            public void notify(Object sender, SimpleSignalProvider.ConnectionState item) {
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

        // This is an asynchronous call. The future will complete when the connection goes through all 3 phases:
        // 1. Connect
        // 2. Bind / BindResult
        // 3. Subscribe / SubscribeResult
        ObservableFuture<Void> future = client.connect(sessionKey);

        // This will complete when the connection is successful. If your internet is down, it won't fail. It will block
        // until the internet comes back up (even if that is forever). (This is what 'Simple' means).
        future.await();

        if (!future.isSuccess()) {
            throw new RuntimeException();
        }
    }
}
