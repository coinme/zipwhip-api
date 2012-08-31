package com.zipwhip.util;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 1:11 PM
 */
public class FutureDateUtil {

    public static Date in1Second(){
        return inFuture(1, TimeUnit.SECONDS);
    }

    public static Date in30Seconds(){
        return inFuture(30, TimeUnit.SECONDS);
    }

    public static Date inFuture(long amount, TimeUnit unit) {
        if (amount == 0){
            return new Date();
        }

        long millis = unit.toMillis(amount);
        return new Date(System.currentTimeMillis() + millis);
    }

}
