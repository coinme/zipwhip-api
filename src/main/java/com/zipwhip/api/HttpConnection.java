package com.zipwhip.api;

import com.zipwhip.api.request.RequestBuilder;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ExecutorFactory;
import com.zipwhip.concurrent.MutableObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.executors.NamedThreadFactory;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.DownloadURL;
import com.zipwhip.util.SignTool;
import com.zipwhip.util.StringUtil;
import com.zipwhip.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides a persistent connection to a User on Zipwhip.
 * <p/>
 * You initialize this class with a sessionKey and then can execute raw requests
 * on behalf of the user. If you want a more Object oriented way to interact
 * with Zipwhip, use Consumer instead of Connection.
 * <p/>
 * This class is thread safe.
 */
public class HttpConnection extends CascadingDestroyableBase implements ApiConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpConnection.class);

    private String apiVersion = "/";
    private String host = ApiConnectionConfiguration.API_HOST;

    private String sessionKey;
    private SignTool authenticator;
    private final Executor bossExecutor;
    private final Executor workerExecutor;

    public HttpConnection() {
        this((ExecutorService)null, null);
    }

    public HttpConnection(String apiKey, String secret) throws Exception {
        this(null, null, new SignTool(apiKey, secret));
    }

    public HttpConnection(Executor bossExecutor, Executor workerExecutor, String apiKey, String secret) throws Exception {
        this(bossExecutor, workerExecutor, new SignTool(apiKey, secret));
    }

    public HttpConnection(Executor bossExecutor, Executor workerExecutor, SignTool authenticator) {
        this(bossExecutor, workerExecutor);
        this.setAuthenticator(authenticator);
    }

    public HttpConnection(Executor bossExecutor, Executor workerExecutor) {
        super();

        if (bossExecutor == null){
            bossExecutor = ExecutorFactory.newInstance("HttpConnection-boss");
            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    ((ExecutorService)HttpConnection.this.bossExecutor).shutdownNow();
                }
            });
        }
        this.bossExecutor = bossExecutor;

        if (workerExecutor == null){
            workerExecutor = Executors.newFixedThreadPool(10, new NamedThreadFactory("HttpConnection-worker-"));
            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    ((ExecutorService)HttpConnection.this.workerExecutor).shutdownNow();
                }
            });
        }
        this.workerExecutor = workerExecutor;
    }

    @Override
    public void setAuthenticator(SignTool authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public SignTool getAuthenticator() {
        return this.authenticator;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public void setSessionKey(String sessionKey) {
        LOGGER.debug("Setting sessionKey to " + sessionKey);
        this.sessionKey = sessionKey;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public boolean isAuthenticated() {
        return StringUtil.exists(sessionKey) || (authenticator != null && authenticator.prepared());
    }

    @Override
    public boolean isConnected() {
        return isAuthenticated();
    }

    @Override
    public ObservableFuture<String> send(String method, Map<String, Object> params) {

        RequestBuilder rb = new RequestBuilder();

        // convert the map into a key/value HTTP params string
        rb.params(params, true);

        return send(method, rb.build());
    }

    private ObservableFuture<String> send(final String method, final String params) {

        // NOTE: if this is a SimpleExecutor (single threaded) then this will be a deadlock.
        final MutableObservableFuture<String> future = new DefaultObservableFuture<String>(this, workerExecutor);

        bossExecutor.execute(new Runnable() {
            @Override
            public void run() {

                String result;

                try {
                    result = DownloadURL.get(UrlUtil.getSignedUrl(host, apiVersion, method, params, sessionKey, authenticator));
                } catch (Exception e) {

                    LOGGER.error("problem with DownloadUrl", e);

                    // NOTE: if this is a SimpleExecutor (single threaded) then this will be a deadlock. (workerExecutor)
                    future.setFailure(e);
                    return;
                }

                // NOTE: if this is a SimpleExecutor (single threaded) then this will be a deadlock. (workerExecutor)
                future.setSuccess(result);
            }
        });

        return future;
    }

    @Override
    public ObservableFuture<String> send(String method, Map<String, Object> params, List<File> files) throws Exception {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public ObservableFuture<InputStream> sendBinaryResponse(String method, Map<String, Object> params) throws Exception {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected void onDestroy() {
        LOGGER.debug("Destroying HttpConnection");
    }

}
