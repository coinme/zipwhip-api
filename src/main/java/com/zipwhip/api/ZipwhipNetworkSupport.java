package com.zipwhip.api;

import com.zipwhip.api.response.JsonResponseParser;
import com.zipwhip.api.response.ResponseParser;
import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.api.response.StringServerResponse;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ExecutorFactory;
import com.zipwhip.concurrent.MutableObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.InputRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A base class for future implementation to extend.
 * <p/>
 * It takes all the non-API specific stuff out of ZipwhipClient implementations.
 * <p/>
 * If some class wants to communicate with Zipwhip, then it needs to extend this
 * class. This class gives functionality that can be used to parse Zipwhip API.
 * This naming convention was copied from Spring (JmsSupport) base class.
 */
public abstract class ZipwhipNetworkSupport extends CascadingDestroyableBase {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ZipwhipNetworkSupport.class);

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * The default timeout when connecting to Zipwhip
     */
    public static final long DEFAULT_TIMEOUT_SECONDS = 45;

    public static final String SESSION_GET = "session/get";
    public static final String PRESENCE_GET = "presence/get";

    public static final String CONTACT_LIST = "contact/list";
    public static final String CONTACT_DELETE = "contact/delete";
    public static final String CONTACT_SAVE = "contact/save";
    public static final String CONTACT_GET = "contact/get";

    public static final String GROUP_GET = "group/get";
    public static final String GROUP_IMAGE = "group/image/view";

    public static final String MESSAGE_SEND = "message/send";
    public static final String MESSAGE_LIST = "message/list";
    public static final String MESSAGE_READ = "message/read";
    public static final String MESSAGE_DELETE = "message/delete";
    public static final String MESSAGE_GET = "message/get";

    public static final String CONVERSATION_LIST = "conversation/list";
    public static final String CONVERSATION_READ = "conversation/read";
    public static final String CONVERSATION_DELETE = "conversation/delete";
    public static final String CONVERSATION_GET = "conversation/get";

    public static final String DEVICE_SAVE = "device/save";
    public static final String DEVICE_GET = "device/get";
    public static final String DEVICE_LIST = "device/list";
    public static final String DEVICE_DELETE = "device/delete";

    public static final String GROUP_SAVE = "group/save";
    public static final String GROUP_ADD_MEMBER = "group/addMember";

    public static final String SIGNALS_DISCONNECT = "signals/disconnect";
    public static final String SIGNALS_CONNECT = "signals/connect";
    public static final String SIGNALS_VERIFY = "signal/verify";
    public static final String SIGNAL_SEND = "signal/send";

    public static final String USER_ENROLL = "user/enroll";
    public static final String USER_DEACT = "user/deact";
    public static final String USER_SAVE = "user/save";
    public static final String USER_GET = "user/get";
    public static final String USER_EXISTS = "user/exists";
    public static final String USER_UNENROLL = "user/unenroll";

    public static final String CARBON_ENABLE = "v1/carbon/enable";
    public static final String CARBON_ENABLED = "v1/carbon/enabled";
    public static final String CARBON_ENABLED_VENDOR = "carbon/enabled";
    public static final String CARBON_INSTALLED = "carbon/installed";
    public static final String CARBON_SUGGEST = "carbon/suggest";
    public static final String CARBON_REGISTER = "carbon/register";
    public static final String CARBON_V2_REGISTER = "carbon/v2/register";
    public static final String CARBON_STATS = "carbon/stats";
    public static final String CARBON_ACCEPTED_TCS = "carbon/acceptedTCs";

    public static final String CHALLENGE_REQUEST = "session/v2/challenge";
    public static final String CHALLENGE_CONFIRM = "session/challenge/confirm";

    public static final String TEXTLINE_PROVISION = "textline/provision";
    public static final String TEXTLINE_ENROLL = "textline/enroll";
    public static final String TEXTLINE_UNENROLL = "textline/unenroll";

    public static final String FACE_IMAGE = "face/image";
    public static final String FACE_IMAGES = "face/v2/image";
    public static final String FACE_NAME = "face/name";
    public static final String FACE_NAMES = "face/v2/name";

    public static final String ATTACHMENT_LIST = "messageAttachment/list";
    public static final String HOSTED_CONTENT_GET = "hostedContent/get";
    public static final String HOSTED_CONTENT_SAVE = "hostedContent/save";

    public static final String TINY_URL_RESERVE = "tinyUrl/reserve";
    public static final String TINY_URL_SAVE = "tinyUrl/save";

    /**
     * A runnable for for for executing asynchronous server responses.
     */
    private static final InputRunnable<ParsableServerResponse<ServerResponse>> FORWARD_RUNNABLE = new InputRunnable<ParsableServerResponse<ServerResponse>>() {
        @Override
        public void run(ParsableServerResponse<ServerResponse> object) {
            object.getFuture().setSuccess(object.getServerResponse());
        }
    };

    /**
     * This importantTaskExecutor really matters. This is the importantTaskExecutor that runs client code. I mean, the guys that call us.
     * They are observing our web calls via this importantTaskExecutor. If it's too small, and they are too slow, it'll backlog.
     */
    protected final Executor callbackExecutor;

    protected ApiConnection connection;
    protected ResponseParser responseParser;

    /**
     * Create a new default {@code ZipwhipNetworkSupport}
     */
    public ZipwhipNetworkSupport() {
        this(null);
    }

    public ZipwhipNetworkSupport(ApiConnection connection) {
        this(null, connection);
    }

    public ZipwhipNetworkSupport(Executor callbackExecutor, ApiConnection connection) {

        if (connection == null) {
            connection = new HttpConnection();
        }

        if (callbackExecutor == null) {
            callbackExecutor = ExecutorFactory.newInstance("ZipwhipNetworkSupport-callbacks");
            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    ((ExecutorService) ZipwhipNetworkSupport.this.callbackExecutor).shutdownNow();
                }
            });
        }
        this.callbackExecutor = callbackExecutor;

        setConnection(connection);
        link(connection);

        setResponseParser(new JsonResponseParser());
    }

    public ApiConnection getConnection() {
        return connection;
    }

    public void setConnection(ApiConnection connection) {
        if (this.connection != null) {
            unlink(this.connection);
        }
        this.connection = connection;
        link(this.connection);
    }

    public ResponseParser getResponseParser() {
        return responseParser;
    }

    public void setResponseParser(ResponseParser responseParser) {
        this.responseParser = responseParser;
    }

    protected ServerResponse executeSync(final String method, final Map<String, Object> params) throws Exception {
        return get(executeAsync(method, params, true, FORWARD_RUNNABLE));
    }

    protected ServerResponse executeSync(final String method, final Map<String, Object> params, boolean requiresAuthentication) throws Exception {
        return get(executeAsync(method, params, requiresAuthentication, FORWARD_RUNNABLE));
    }

    protected ServerResponse executeSync(final String method, final Map<String, Object> params, List<File> files) throws Exception {
        return get(executeAsync(method, params, files, true, FORWARD_RUNNABLE));
    }

    protected ServerResponse executeSync(final String method, final Map<String, Object> params, List<File> files, boolean requiresAuthentication) throws Exception {
        return get(executeAsync(method, params, files, requiresAuthentication, FORWARD_RUNNABLE));
    }

    protected <T> ObservableFuture<T> executeAsync(String method, Map<String, Object> params) throws Exception {
        return executeAsync(method, params, true, null);
    }

    protected <T> ObservableFuture<T> executeAsync(String method, Map<String, Object> params, boolean requiresAuthentication, final InputRunnable<ParsableServerResponse<T>> businessLogic) throws Exception {
        return executeAsync(method, params, null, requiresAuthentication, businessLogic);
    }

    protected <T> ObservableFuture<T> executeAsync(String method, Map<String, Object> params, List<File> files, boolean requiresAuthentication, final InputRunnable<ParsableServerResponse<T>> businessLogic) throws Exception {

        if (requiresAuthentication && !connection.isAuthenticated()) {
            throw new Exception("The connection is not authenticated, can't continue.");
        }

        final MutableObservableFuture<T> result = new DefaultObservableFuture<T>(this, callbackExecutor);

        final ObservableFuture<String> responseFuture;

        if (CollectionUtil.exists(files)) {
            responseFuture = getConnection().send(method, params, files);
        } else {
            responseFuture = getConnection().send(method, params);
        }

        responseFuture.addObserver(new Observer<ObservableFuture<String>>() {

            /**
             * This code will execute in the "workerExecutor" of the connection.
             * If you pass in a bogus/small importantTaskExecutor to him, our code will lag.
             *
             * @param sender The sender might not be the same object every time.
             * @param item Rich object representing the notification.
             */
            @Override
            public void notify(Object sender, ObservableFuture<String> item) {

                // The network is done! let's check for our cake!
                if (!item.isDone()) {
                    return;
                }

                if (item.isCancelled()) {
                    // this will execute in the "callbackExecutor"
                    result.cancel();
                    return;
                }
                if (!item.isSuccess()) {
                    // this will execute in the "callbackExecutor"
                    result.setFailure(item.getCause());
                    return;
                }

                String responseString = item.getResult();

                ServerResponse serverResponse;
                try {
                    serverResponse = responseParser.parse(responseString);
                } catch (Exception e) {
                    LOGGER.error("Problem parsing json response", e);
                    // this will execute in the "callbackExecutor"
                    result.setFailure(e);
                    return;
                }

                try {
                    checkAndThrowError(serverResponse);
                } catch (Exception e) {
                    // this will execute in the "callbackExecutor"
                    result.setFailure(e);
                    return;
                }

                try {
                    if (businessLogic != null) {
                        businessLogic.run(new ParsableServerResponse<T>(result, serverResponse));
                    } else {
                        result.setSuccess(null);
                    }
                } catch (Exception e) {
                    LOGGER.error("Problem with running the business logic conversion", e);
                    // this will execute in the "callbackExecutor"
                    result.setFailure(e);
                }
            }
        });

        return result;
    }

    protected ObservableFuture<byte[]> executeAsyncBinaryResponse(String method, Map<String, Object> params, boolean requiresAuthentication) throws Exception {

        if (requiresAuthentication && !connection.isAuthenticated()) {
            throw new Exception("The connection is not authenticated, can't continue.");
        }

        final MutableObservableFuture<byte[]> result = new DefaultObservableFuture<byte[]>(this, callbackExecutor);

        final ObservableFuture<InputStream> responseFuture = getConnection().sendBinaryResponse(method, params);

        responseFuture.addObserver(new Observer<ObservableFuture<InputStream>>() {

            @Override
            public void notify(Object sender, ObservableFuture<InputStream> item) {

                // The network is done! let's check for our cake!
                if (!item.isDone()) {
                    return;
                }

                if (item.isCancelled()) {
                    // this will execute in the "callbackExecutor"
                    result.cancel();
                    return;
                }
                if (!item.isSuccess()) {
                    // this will execute in the "callbackExecutor"
                    result.setFailure(item.getCause());
                    return;
                }

                byte[] bytes;

                try {
                    bytes = toByteArray(item.getResult());
                } catch (IOException e) {
                    result.setFailure(e);
                    return;
                }

                result.setSuccess(bytes);
            }
        });

        return result;
    }

    protected void checkAndThrowError(ServerResponse serverResponse) throws Exception {

        if (serverResponse == null) {
            // A null response from the server is OK
            return;
        }

        if (!serverResponse.isSuccess()) {
            throwError(serverResponse);
        }
    }

    protected void throwError(ServerResponse serverResponse) throws Exception {

        if (serverResponse instanceof StringServerResponse) {

            StringServerResponse string = (StringServerResponse) serverResponse;
            throw new Exception(string.response);

        } else {
            throw new Exception(serverResponse.getRaw());
        }
    }

    protected static byte[] toByteArray(InputStream input) throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        int numBytes;

        while (-1 != (numBytes = input.read(buffer))) {
            output.write(buffer, 0, numBytes);
        }

        return output.toByteArray();
    }

    protected <T> T get(ObservableFuture<T> task) throws Exception {

        task.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!task.isSuccess()) {
            throw new Exception(task.getCause());
        }

        return task.getResult();
    }

    protected boolean success(ServerResponse serverResponse) {
        return (serverResponse != null) && serverResponse.isSuccess();
    }

    protected static class ParsableServerResponse<T> {

        private MutableObservableFuture<T> future;
        private ServerResponse serverResponse;

        private ParsableServerResponse(MutableObservableFuture<T> future, ServerResponse serverResponse) {
            this.future = future;
            this.serverResponse = serverResponse;
        }

        public MutableObservableFuture<T> getFuture() {
            return future;
        }

        public ServerResponse getServerResponse() {
            return serverResponse;
        }
    }

}
