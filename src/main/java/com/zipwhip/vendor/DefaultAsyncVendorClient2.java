package com.zipwhip.vendor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ning.http.client.AsyncHttpClient;
import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.Connection;
import com.zipwhip.api.dto.MessageToken;
import com.zipwhip.api.request.QueryStringBuilder;
import com.zipwhip.api.response.MessageSendResult;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.MutableObservableFuture;
import com.zipwhip.concurrent.NestedObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.util.InputCallable;
import com.zipwhip.util.StreamUtil;
import com.zipwhip.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * @author Michael
 * @date 5/20/2014
 */
public class DefaultAsyncVendorClient2 extends CascadingDestroyableBase implements AsyncVendorClient2 {

    private Connection connection;
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(MessageSendResult.class, new MessageSendResultTypeAdapter())
            .create();
    private Executor eventExecutor;
    private String vendorKey;

    @Override
    public ObservableFuture<MessageSendResult> send(final String subscriberPhoneNumber, final String friendPhoneNumber, final String body, final String advertisement) throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();

        // In a few weeks, change this to apiKey
        params.put("vendorKey", vendorKey);
        params.put("sourceAddress", subscriberPhoneNumber);
        params.put("destinationAddress", friendPhoneNumber);
        params.put("body", body);

        if (StringUtil.exists(advertisement)){
            params.put("advertisement", advertisement);
        }

        return executeAsync("message/send", params, new InputCallable<InputStream, MessageSendResult>() {
            @Override
            public MessageSendResult call(InputStream input) throws Exception {
                MessageSendResult result = gson.fromJson(StreamUtil.getString(input), MessageSendResult.class);

                if (result == null) {
                    return null;
                }

                result.setSubscriberPhoneNumber(subscriberPhoneNumber);
                result.setFriendPhoneNumber(friendPhoneNumber);
                result.setBody(body);
                result.setAdvertisement(advertisement);

                return result;
            }
        });
    }

    private <T> ObservableFuture<T> executeAsync(String path, Map<String, Object> params, final InputCallable<InputStream, T> callable) throws Exception {
        final ObservableFuture<InputStream> future = connection.send("GET", path, params);
        final MutableObservableFuture<T> result = new DefaultObservableFuture<T>(this, eventExecutor, "AsyncVendorClient2-events");

        future.addObserver(new Observer<ObservableFuture<InputStream>>() {
            @Override
            public void notify(Object sender, ObservableFuture<InputStream> item) {
                if (!item.isSuccess()) {
                    NestedObservableFuture.syncFailure(item, result);
                    return;
                }

                try {
                    if (callable == null) {
                        result.setSuccess(null);
                    } else {
                        result.setSuccess(callable.call(item.getResult()));
                    }
                } catch (Exception e) {
                    // If the future is cancelled or failed, it will throw an exception on getResult();
                    result.setFailure(e);
                }
            }
        });

        return result;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Gson getGson() {
        return gson;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    public Executor getEventExecutor() {
        return eventExecutor;
    }

    public void setEventExecutor(Executor eventExecutor) {
        this.eventExecutor = eventExecutor;
    }

    public String getVendorKey() {
        return vendorKey;
    }

    public void setVendorKey(String vendorKey) {
        this.vendorKey = vendorKey;
    }

    @Override
    protected void onDestroy() {

    }
}
