package com.zipwhip.api.signals;

import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.events.Observer;
import com.zipwhip.timers.HashedWheelTimer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

/**
 * Date: 9/4/13
 * Time: 5:11 PM
 *
 * @author Michael
 * @version 1
 */
public class SilenceOnTheLineBufferedOrderedQueueTest {

    SilenceOnTheLineBufferedOrderedQueue<DeliveredMessage> queue = new SilenceOnTheLineBufferedOrderedQueue<DeliveredMessage>(new HashedWheelTimer(), 100, TimeUnit.MILLISECONDS);

    @Test
    public void testCorrectOrdering() throws Exception {
        DeliveredMessage message1 = new DeliveredMessage();
        message1.setTimestamp(1);

        DeliveredMessage message2 = new DeliveredMessage();
        message2.setTimestamp(2);

        queue.append(message1);
        queue.append(message2);

        final List<DeliveredMessage> list = new ArrayList<DeliveredMessage>();
        final CountDownLatch latch = new CountDownLatch(2);
        queue.getItemEvent().addObserver(new Observer<DeliveredMessage>() {
            @Override
            public void notify(Object sender, DeliveredMessage item) {
                list.add(item);
                latch.countDown();
            }
        });

        latch.await();

        assertEquals(1, list.get(0).getTimestamp());
        assertEquals(2, list.get(1).getTimestamp());
    }
}
