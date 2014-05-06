package com.zipwhip.important.schedulers;

import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.NamedThreadFactory;
import com.zipwhip.important.Scheduler;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.timers.HashedWheelTimer;
import com.zipwhip.timers.Timeout;
import com.zipwhip.timers.Timer;
import com.zipwhip.timers.TimerTask;
import com.zipwhip.util.HashCodeComparator;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 12:37 PM
 */
public class TimerScheduler extends CascadingDestroyableBase implements Scheduler {

    private final Timer timer;
    private final ObservableHelper<String> observableHelper = new ObservableHelper<String>("Scheduler");
    private Map<String, Timeout> map = Collections.synchronizedMap(new TreeMap<String, Timeout>(HashCodeComparator.getInstance()));

    public TimerScheduler() {
        this(null, "TimerScheduler");
    }

    public TimerScheduler(String name) {
        this(null, name);
    }

    public TimerScheduler(Timer timer) {
        this.timer = timer;
    }

    protected TimerScheduler(final Timer timer, String name) {
        this(timer == null ? new HashedWheelTimer(new NamedThreadFactory("HashedWheelScheduler-" + name), 1, TimeUnit.SECONDS) : timer);

        // since we created it, destroy it when we get destroyed.
        if (timer == null){
            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    TimerScheduler.this.timer.stop();
                }
            });
        }
    }

    @Override
    public synchronized void schedule(final String requestId, Date exitTime) {
        Timeout timeout = timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                synchronized (TimerScheduler.this) {
                    observableHelper.notifyObservers(TimerScheduler.this, requestId);
                    if (isSameRequest(requestId, timeout)) {
                        map.remove(requestId);
                    }
                }
            }
        }, exitTime.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);

        map.put(requestId, timeout);
    }

    @Override
    public void scheduleRecurring(final String requestId, final long interval, final TimeUnit units) {
        TimerTask task = new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {

                synchronized (TimerScheduler.this) {
                    observableHelper.notifyObservers(TimerScheduler.this, requestId);
                    if (isSameRequest(requestId, timeout)) {
                        map.remove(requestId);
                    }
                }

                Timeout t = timer.newTimeout(this, interval, units);
                map.put(requestId, t);
            }
        };

        Timeout timeout = timer.newTimeout(task, interval, units);

        map.put(requestId, timeout);
    }

    @Override
    public synchronized void cancel(String requestId) {
        Timeout timeout = map.get(requestId);

        if (timeout == null) {
            return;
        }

        if (isSameRequest(requestId, timeout)) {
            timeout.cancel();
            map.remove(requestId);
        }
    }

    @Override
    public void onScheduleComplete(Observer<String> observer) {
        observableHelper.addObserver(observer);
    }

    @Override
    public void removeOnScheduleComplete(Observer<String> observer) {
        observableHelper.removeObserver(observer);
    }

    @Override
    protected void onDestroy() {

    }

    private boolean isSameRequest(String requestId, Timeout timeout) {
        if (timeout == null){
            return false;
        }

        return  (map.get(requestId) == timeout);
    }
}
