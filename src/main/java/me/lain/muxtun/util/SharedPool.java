package me.lain.muxtun.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public enum SharedPool implements Executor {

    INSTANCE;

    private final Executor pool = Executors.newWorkStealingPool(Math.max(4, Math.min(Runtime.getRuntime().availableProcessors(), Short.MAX_VALUE)));

    @Override
    public void execute(Runnable command) {
        pool.execute(command);
    }

}
