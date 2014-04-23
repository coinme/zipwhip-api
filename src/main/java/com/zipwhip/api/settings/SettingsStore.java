package com.zipwhip.api.settings;

import com.zipwhip.util.KeyValueStore;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/1/11
 * Time: 9:58 AM
 *
 * This is an interface that should be extended to implement your platform's preferred storage solution for important
 * settings.
 */
public interface SettingsStore extends KeyValueStore<SettingsStore.Keys, String> {

    public enum Keys {
        SESSION_KEY,
        CLIENT_ID,
        CLIENT_ID_TOKEN,
        USERNAME,
        VERSIONS,
        EXPECTS_SUBSCRIPTION_COMPLETE,
        LAST_SUBSCRIBED_CLIENT_ID
    }

}
