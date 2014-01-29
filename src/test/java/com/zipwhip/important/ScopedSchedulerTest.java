package com.zipwhip.important;

import com.zipwhip.events.Observer;
import com.zipwhip.important.schedulers.TimerScheduler;
import com.zipwhip.util.FutureDateUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.fail;

/**
 * Created with IntelliJ IDEA.
 * User: russ
 * Date: 1/28/14
 * Time: 10:41 AM
 */
public class ScopedSchedulerTest {

    static Scheduler innerScheduler;
    static Scheduler outerScheduler1;
    static Scheduler outerScheduler2;


    @BeforeClass
    public static void setUp() {
        innerScheduler = new TimerScheduler("inner1");

        outerScheduler1 = new ScopedScheduler(innerScheduler, "outer1-");
        outerScheduler2 = new ScopedScheduler(innerScheduler, "outer2-");
    }

    @Test
    public void testNormalSchedule() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(2);

        outerScheduler1.onScheduleComplete(new Observer<String>() {
            @Override
            public void notify(Object sender, String item) {
                latch.countDown();
            }
        });

        outerScheduler2.onScheduleComplete(new Observer<String>() {
            @Override
            public void notify(Object sender, String item) {
                latch.countDown();
            }
        });

        innerScheduler.onScheduleComplete(new Observer<String>() {
            @Override
            public void notify(Object sender, String item) {
                latch.countDown();
            }
        });

        outerScheduler1.schedule(UUID.randomUUID().toString(), FutureDateUtil.inFuture(50, TimeUnit.MILLISECONDS));

        latch.await(3, TimeUnit.SECONDS);

        if (latch.getCount() != 0) {
            fail("Didn't count down as it should have!");
        }
    }
}
