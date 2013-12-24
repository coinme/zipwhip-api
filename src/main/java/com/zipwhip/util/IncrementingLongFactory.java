package com.zipwhip.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/8/12
 * Time: 3:30 PM
 *
 * For creating id's incrementing.
 */
public class IncrementingLongFactory implements Factory<Long> {

    private static final Factory<Long> INSTANCE = new IncrementingLongFactory();

    private final AtomicLong ID = new AtomicLong();

    @Override
    public Long create() {
        return ID.incrementAndGet();
    }

    public static Factory<Long> getInstance() {
        return INSTANCE;
    }
}
