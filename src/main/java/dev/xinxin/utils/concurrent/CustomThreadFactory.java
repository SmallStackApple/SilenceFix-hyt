package dev.xinxin.utils.concurrent;

import dev.xinxin.SilenceFix;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class CustomThreadFactory implements ThreadFactory {
    private final AtomicInteger threadId = new AtomicInteger(0);

    @Override
    public Thread newThread(@NotNull Runnable r) {
        var threadName = SilenceFix.NAME + "-Thread-" + threadId.getAndIncrement();

        var thread = new Thread(r, threadName);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) -> {
        });

        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }
}