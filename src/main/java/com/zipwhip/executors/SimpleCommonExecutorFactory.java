package com.zipwhip.executors;

import java.util.concurrent.ExecutorService;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 6/3/2014
 * Time: 12:55 PM
 */
public class SimpleCommonExecutorFactory implements CommonExecutorFactory {

    private static final SimpleCommonExecutorFactory INSTANCE = new SimpleCommonExecutorFactory();

    @Override
    public ExecutorService create() throws Exception {
        return create(null, null);
    }

    @Override
    public ExecutorService create(CommonExecutorTypes type, String name) {
        return SimpleExecutor.getInstance();
    }

    public static SimpleCommonExecutorFactory getInstance() {
        return INSTANCE;
    }
}
