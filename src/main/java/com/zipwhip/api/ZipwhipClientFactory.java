package com.zipwhip.api;

import com.zipwhip.api.settings.SettingsStore;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.executors.CommonExecutorFactory;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.util.Factory;

/**
 * This factory produces {@code ZipwhipClient}s that are authenticated.
 */
public class ZipwhipClientFactory implements Factory<ZipwhipClient> {

    private Factory<ApiConnection> connectionFactory;
    private Factory<SignalProvider> signalProviderFactory;
    private ImportantTaskExecutor importantTaskExecutor;
    private CommonExecutorFactory executorFactory;
    private SettingsStore settingsStore;

    public ZipwhipClientFactory() {
        this(null, null);
    }

    public ZipwhipClientFactory(ApiConnectionFactory connectionFactory, Factory<SignalProvider> signalProviderFactory) {
        this(connectionFactory, signalProviderFactory, null, null, null);
    }

    public ZipwhipClientFactory(ApiConnectionFactory connectionFactory, Factory<SignalProvider> signalProviderFactory, ImportantTaskExecutor importantTaskExecutor, SettingsStore settingsStore, CommonExecutorFactory executorFactory) {
        this.connectionFactory = connectionFactory;
        this.signalProviderFactory = signalProviderFactory;
        this.importantTaskExecutor = importantTaskExecutor;
        this.settingsStore = settingsStore;
        this.executorFactory = executorFactory;

        if (this.importantTaskExecutor == null){
            this.importantTaskExecutor = new ImportantTaskExecutor();
        }
    }

    /**
     * Create a new ZipwhipClient which has been authenticated via a username and password.
     *
     * @param username The mobile number of the user.
     * @param password The user's Zipwhip password.
     * @return An authenticated {@link ZipwhipClient}
     * @throws Exception if an error occurs creating or authenticating the client.
     */
    public static ZipwhipClient createViaUsername(String username, String password) throws Exception {
        ApiConnectionFactory connectionFactory = new HttpApiConnectionFactory();
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);

        // TODO: signal provider factory

        ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory, null);

        return zipwhipClientFactory.create();
    }

    /**
     * Create a new ZipwhipClient which has been authenticated via a username and password.
     *
     * The Connection will use {@code NingHttpConnection}
     *
     * @param username The mobile number of the user.
     * @param password The user's Zipwhip password.
     * @return An authenticated {@link ZipwhipClient}
     * @throws Exception if an error occurs creating or authenticating the client.
     */
    public static ZipwhipClient createAsyncViaUsername(String username, String password) throws Exception {
        ApiConnectionFactory connectionFactory = new NingApiConnectionFactory();
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);

        // TODO: signal provider factory

        ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory, null);

        return zipwhipClientFactory.create();
    }


    /**
     * Create a new ZipwhipClient which has been pre-authenticated via the sessionKey.
     *
     * @param sessionKey A valid Zipwhip sessionKey.
     * @return An authenticated {@link ZipwhipClient}
     * @throws Exception if an error occurs creating or authenticating the client.
     */
    public static ZipwhipClient createViaSessionKey(String sessionKey) throws Exception {
        ApiConnectionFactory connectionFactory = new HttpApiConnectionFactory();
        connectionFactory.setSessionKey(sessionKey);

        // TODO: signal provider factory

        ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory, null);

        return zipwhipClientFactory.create();
    }

    /**
     * Create a new ZipwhipClient which has been pre-authenticated via the sessionKey.
     *
     * The Connection will use {@code NingHttpConnection}
     *
     * @param sessionKey A valid Zipwhip sessionKey.
     * @return An authenticated {@link ZipwhipClient}
     * @throws Exception if an error occurs creating or authenticating the client.
     */
    public static ZipwhipClient createAsyncViaSessionKey(String sessionKey) throws Exception {
        ApiConnectionFactory connectionFactory = new NingApiConnectionFactory();
        connectionFactory.setSessionKey(sessionKey);

        // TODO: signal provider factory

        ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory, null);

        return zipwhipClientFactory.create();
    }

    /**
     * Create an authenticated ZipwhipClient.
     * 
     * @return An authenticated ZipwhipClient.
     */
    @Override
    public ZipwhipClient create() {
        return new DefaultZipwhipClient(settingsStore, executorFactory == null ? null : executorFactory.create(), importantTaskExecutor, connectionFactory.create(), signalProviderFactory == null ? null : signalProviderFactory.create());
    }

}
