package com.zipwhip.util;

import com.zipwhip.timers.Timeout;
import com.zipwhip.timers.Timer;
import com.zipwhip.timers.TimerTask;

import java.util.concurrent.TimeUnit;

/**
 * Date: 5/22/13
 * Time: 3:33 PM
 *
 * @author Michael
 * @version 1
 */
public class BufferedRunnable implements Runnable, TimerTask {

    private final Timer timer;
    private final Runnable runnable;
    private final long time;
    private final TimeUnit timeUnit;

    private volatile Timeout timeout;

    public BufferedRunnable(Timer timer, Runnable runnable, long time, TimeUnit timeUnit) {
        this.timer = timer;
        this.runnable = runnable;
        this.time = time;
        this.timeUnit = timeUnit;
    }

    public BufferedRunnable(Timer timer, Runnable runnable) {
        this(timer, runnable, 5, TimeUnit.SECONDS);
    }

    @Override
    public synchronized void run() {
        if (timeout != null) {
            timeout.cancel();
        }

        timeout = timer.newTimeout(this, time, timeUnit);
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        runnable.run();
    }

    public Timer getTimer() {
        return timer;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public long getTime() {
        return time;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public Timeout getTimeout() {
        return timeout;
    }
}
