package com.zipwhip.api.signals;

import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.reliable.retry.RetryStrategy;
import com.zipwhip.timers.HashedWheelTimer;
import com.zipwhip.timers.Timer;
import com.zipwhip.util.Factory;
import com.zipwhip.util.StringUtil;

import java.util.concurrent.Executor;

/**
 * @author Ali Serghini
 *         Date: 12/16/13
 *         Time: 4:06 PM
 */
public class SocketIoSignalConnectionFactory implements Factory<SignalConnection> {

    private Executor executor = null;
    private Timer timer;

    private final String signalsUrl;
    private final ImportantTaskExecutor importantTaskExecutor;
    private final RetryStrategy retryStrategy;

    public SocketIoSignalConnectionFactory(final String signalsUrl, final ImportantTaskExecutor importantTaskExecutor, final RetryStrategy retryStrategy) {
        this.signalsUrl = signalsUrl;
        this.importantTaskExecutor = importantTaskExecutor;
        this.retryStrategy = retryStrategy;
    }

    @Override
    public SignalConnection create() {

        checkParams();

        final SocketIoSignalConnection connection = new SocketIoSignalConnection();
        connection.setUrl(signalsUrl);
        connection.setImportantTaskExecutor(importantTaskExecutor);
        connection.setTimer(timer);
        connection.setExecutor(executor);
        connection.setRetryStrategy(retryStrategy);

        return connection;
    }

    private void checkParams() {
        if (StringUtil.isNullOrEmpty(signalsUrl)) {
            throw new IllegalStateException("signalsUrl has not been set");
        }

        if (importantTaskExecutor == null) {
            throw new IllegalStateException("importantTaskExecutor has not been set");
        }

        if (retryStrategy == null) {
            throw new IllegalStateException("retryStrategy has not been set");
        }

        if (executor == null) {
            executor = SimpleExecutor.getInstance();
        }

        if (timer == null) {
            timer = new HashedWheelTimer();
        }
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

}
