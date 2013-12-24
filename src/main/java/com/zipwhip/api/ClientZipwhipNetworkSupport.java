package com.zipwhip.api;

import com.zipwhip.api.settings.PreferencesSettingsStore;
import com.zipwhip.api.settings.SettingsStore;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.important.ImportantTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

import static com.zipwhip.concurrent.ThreadUtil.ensureLock;

/**
 * A base class for future implementation to extend.
 * <p/>
 * It takes all the non-API specific stuff out of ZipwhipClient implementations.
 * <p/>
 * If some class wants to communicate with Zipwhip, then it needs to extend this
 * class. This class gives functionality that can be used to parse Zipwhip API.
 * This naming convention was copied from Spring (JmsSupport) base class.
 */
public abstract class ClientZipwhipNetworkSupport extends ZipwhipNetworkSupport {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ClientZipwhipNetworkSupport.class);

    protected final ImportantTaskExecutor importantTaskExecutor;
    protected long signalsConnectTimeoutInSeconds = 10;

    protected SignalProvider signalProvider;
    protected SettingsStore settingsStore;

    /**
     *
     *
     * @param executor The Executor that's used for processing callbacks, futures, and SignalProvider events.
     * @param importantTaskExecutor This class gives us the ability to expire and cancel futures (SubscriptionCompleteCommand never comes back).
     * @param connection For talking with Zipwhip (message/send)
     * @param signalProvider For signal i/o
     */
    public ClientZipwhipNetworkSupport(SettingsStore store, Executor executor, ImportantTaskExecutor importantTaskExecutor, ApiConnection connection, SignalProvider signalProvider) {
        super(executor, connection);

        if (signalProvider != null) {
            setSignalProvider(signalProvider);
            link(signalProvider);
        }

        if (importantTaskExecutor == null){
            importantTaskExecutor = new ImportantTaskExecutor();
            this.link(importantTaskExecutor);
        }
        this.importantTaskExecutor = importantTaskExecutor;

        if (store == null) {
            store = new PreferencesSettingsStore();
        }

        this.setSettingsStore(store);
    }

    public SettingsStore getSettingsStore() {
        return settingsStore;
    }

    public void setSettingsStore(SettingsStore store) {
        this.settingsStore = store;
    }

    public SignalProvider getSignalProvider() {
        return signalProvider;
    }

    public void setSignalProvider(SignalProvider signalProvider) {
        this.signalProvider = signalProvider;
    }

    public long getSignalsConnectTimeoutInSeconds() {
        return signalsConnectTimeoutInSeconds;
    }

    public void setSignalsConnectTimeoutInSeconds(long signalsConnectTimeoutInSeconds) {
        this.signalsConnectTimeoutInSeconds = signalsConnectTimeoutInSeconds;
    }

    private synchronized void onSubscriptionComplete(String clientId) {
        accessSettings();

        settingsStore.put(SettingsStore.Keys.CLIENT_ID, clientId);
        settingsStore.put(SettingsStore.Keys.EXPECTS_SUBSCRIPTION_COMPLETE, "false");
        settingsStore.put(SettingsStore.Keys.LAST_SUBSCRIBED_CLIENT_ID, clientId);
    }

    private void accessSettings() {
        ensureLock(ClientZipwhipNetworkSupport.this);
        ensureLock(signalProvider);
        ensureLock(settingsStore);
    }

}
