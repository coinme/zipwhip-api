package com.zipwhip.executors;

import java.util.concurrent.Executor;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 9/2/12
 * Time: 5:11 PM
 */
public class NullExecutor implements Executor {

    @Override
    public void execute(Runnable runnable) {
        // fuck em, don't execute.
    }
}
