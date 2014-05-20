package com.zipwhip.api;

import com.zipwhip.api.response.JsonResponseParser;
import com.zipwhip.api.response.ResponseParser;
import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.api.response.StringServerResponse;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.util.Factory;
import com.zipwhip.util.SignTool;
import com.zipwhip.util.StreamUtil;
import com.zipwhip.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a Connection with the specified parameters.
 */
public abstract class ApiConnectionFactory implements Factory<ApiConnection> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiConnectionFactory.class);

    private ResponseParser responseParser = new JsonResponseParser();

    private String host = ApiConnectionConfiguration.API_HOST;
    private String apiVersion = ApiConnection.DEFAULT_API_VERSION;
    private String username;
    private String password;
    private String apiKey;
    private String secret;
    private String sessionKey;

    public ApiConnectionFactory() {
        super();
    }

    /**
     * Creates a generic unauthenticated ApiConnection.
     *
     * @return Connection an authenticated Connection
     */
    @Override
    public ApiConnection create() throws Exception {
        ApiConnection connection = createInstance();

        connection.setSessionKey(sessionKey);
        connection.setApiVersion(apiVersion);
        connection.setHost(host);

        if (StringUtil.exists(apiKey) && StringUtil.exists(secret)) {
            connection.setAuthenticator(new SignTool(apiKey, secret));
        }

        // We have a username/password
        if (StringUtil.exists(username) && StringUtil.exists(password)) {
            Map<String, Object> params = new HashMap<String, Object>();

            params.put("mobileNumber", username);
            params.put("password", password);

            ObservableFuture<InputStream> future = connection.send("get", "user/login", params);

            future.awaitUninterruptibly();

            if (!future.isSuccess()) {
                throw new Exception("Cannot create connection, login rejected");
            }

            ServerResponse serverResponse = responseParser.parse(StreamUtil.getString(future.getResult()));

            if (!serverResponse.isSuccess()) {
                throw new Exception("Error authenticating client");
            }

            if (serverResponse instanceof StringServerResponse) {
                connection.setSessionKey(((StringServerResponse) serverResponse).response);
            }
        }

        return connection;
    }

    protected abstract ApiConnection createInstance();

    public ResponseParser getResponseParser() {
        return responseParser;
    }

    public void setResponseParser(ResponseParser responseParser) {
        this.responseParser = responseParser;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

}
