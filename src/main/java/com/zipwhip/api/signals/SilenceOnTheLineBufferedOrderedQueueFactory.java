package com.zipwhip.api.signals;

import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.timers.Timer;
import com.zipwhip.util.Factory;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 10/29/13
 * Time: 4:57 PM
 */
public class SilenceOnTheLineBufferedOrderedQueueFactory implements Factory<BufferedOrderedQueue<DeliveredMessage>> {

    private Timer timer;

    public SilenceOnTheLineBufferedOrderedQueueFactory(Timer timer) {
        this.timer = timer;
    }

    @Override
    public BufferedOrderedQueue<DeliveredMessage> create() {
        return new SilenceOnTheLineBufferedOrderedQueue<DeliveredMessage>(timer);
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }
}
