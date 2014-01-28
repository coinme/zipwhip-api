package com.zipwhip.important;

import com.zipwhip.events.ObservableHelper;
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
    static Scheduler outerScheduler;

    @BeforeClass
    public static void setUp() {
        innerScheduler = new TimerScheduler("inner");
        outerScheduler = new ScopedScheduler(innerScheduler, "outer-");
    }

    @Test
    public void testNormalSchedule() throws InterruptedException {
        final CountDownLatch innerLatch = new CountDownLatch(1);
        final CountDownLatch outerLatch = new CountDownLatch(1);

        outerScheduler.onScheduleComplete(new Observer<String>() {
            @Override
            public void notify(Object sender, String item) {
                outerLatch.countDown();
            }
        });

        innerScheduler.onScheduleComplete(new Observer<String>() {
            @Override
            public void notify(Object sender, String item) {
                innerLatch.countDown();
            }
        });

        outerScheduler.schedule(UUID.randomUUID().toString(), FutureDateUtil.inFuture(50, TimeUnit.MILLISECONDS));

        innerLatch.await(3, TimeUnit.SECONDS);

        if (innerLatch.getCount() > 0) {
            fail("Inner scheduler didn't fire!");
        }

        if (outerLatch.getCount() > 0) {
            fail("Outer scheduler didn't fire!");
        }
    }
}
