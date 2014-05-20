package com.zipwhip.vendor;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.ApiConnectionConfiguration;
import com.zipwhip.api.ApiConnectionFactory;
import com.zipwhip.api.NingApiConnectionFactory;
import com.zipwhip.util.Factory;

/**
 * This factory produces {@code AsyncVendorClient}s that are authenticated.
 */
@Deprecated
public class AsyncVendorClientFactory implements Factory<AsyncVendorClient> {

    private static final String API_VERSION = "/vendor/v1/";

    private Factory<ApiConnection> connectionFactory;

    private AsyncVendorClientFactory(ApiConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Create a new AsyncVendorClient which has been authenticated via apiKey.
     * <p/>
     * By default this method will be over HTTPS.
     *
     * @param apiKey The Zipwhip assigned, vendor specific key.
     * @param secret The Zipwhip assigned, vendor specific secret.
     * @return An authenticated {@link AsyncVendorClient}
     * @throws Exception if an error occurs creating or authenticating the client.
     */
    public static AsyncVendorClient createViaApiKey(String apiKey, String secret) throws Exception {
        ApiConnectionFactory connectionFactory = new NingApiConnectionFactory();

        connectionFactory.setHost(ApiConnectionConfiguration.SIGNALS_HOST);
        connectionFactory.setApiVersion(API_VERSION);
        connectionFactory.setApiKey(apiKey);
        connectionFactory.setSecret(secret);

        AsyncVendorClientFactory asyncVendorClientFactory = new AsyncVendorClientFactory(connectionFactory);

        return asyncVendorClientFactory.create();
    }

    /**
     * Create a new AsyncVendorClient which has been authenticated via apiKey.
     * <p/>
     * By default this method will be over HTTPS.
     *
     * @param apiKey The Zipwhip assigned, vendor specific key.
     * @param secret The Zipwhip assigned, vendor specific secret.
     * @param host   The host to connect to.
     * @return An authenticated {@link AsyncVendorClient}
     * @throws Exception if an error occurs creating or authenticating the client.
     */
    public static AsyncVendorClient createViaApiKey(String apiKey, String secret, String host) throws Exception {
        ApiConnectionFactory connectionFactory = new NingApiConnectionFactory();

        connectionFactory.setHost(host);
        connectionFactory.setApiKey(apiKey);
        connectionFactory.setSecret(secret);
        connectionFactory.setApiVersion(API_VERSION);

        AsyncVendorClientFactory asyncVendorClientFactory = new AsyncVendorClientFactory(connectionFactory);

        return asyncVendorClientFactory.create();
    }

    /**
     * Create an authenticated AsyncVendorClient.
     *
     * @return An authenticated AsyncVendorClient.
     */
    @Override
    public AsyncVendorClient create() throws Exception {
        return new DefaultAsyncVendorClient(connectionFactory.create());
    }

}
