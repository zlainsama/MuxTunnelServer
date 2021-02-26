package me.lain.muxtun;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ShutdownTasks {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<Runnable> TASKS = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private ShutdownTasks() {
    }

    public static boolean register(Runnable task) {
        if (!REGISTERED.get() && REGISTERED.compareAndSet(false, true))
            registerHook();
        return TASKS.add(Objects.requireNonNull(task, "task"));
    }

    private static void registerHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Runnable task : TASKS) {
                try {
                    task.run();
                } catch (Throwable t) {
                    LOGGER.error("error executing shutdown task", t);
                }
            }
        }, "ShutdownTasks"));
    }

}
