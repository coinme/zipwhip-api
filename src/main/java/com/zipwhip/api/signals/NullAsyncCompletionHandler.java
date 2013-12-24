package com.zipwhip.api.signals;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

/**
 * Date: 8/22/13
 * Time: 1:41 PM
 *
 * @author Michael
 * @version 1
 */
public class NullAsyncCompletionHandler<T> extends AsyncCompletionHandler<T> {

    private static final AsyncCompletionHandler INSTANCE = new NullAsyncCompletionHandler();

    @Override
    public T onCompleted(Response response) throws Exception {
        switch (response.getStatusCode()) {
            case 200:
                return null;
            default:
                throw new Exception(response.getStatusText());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> AsyncCompletionHandler<T> getInstance() {
        return INSTANCE;
    }

}
