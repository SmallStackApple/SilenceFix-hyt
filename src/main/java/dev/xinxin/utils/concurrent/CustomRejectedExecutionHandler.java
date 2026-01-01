package dev.xinxin.utils.concurrent;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;


public class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        var executorStats = String.format(
                "Pool Size: %d, Active Threads: %d, Completed Tasks: %d, Total Tasks: %d, Queue Size: %d",
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getCompletedTaskCount(),
                executor.getTaskCount(),
                executor.getQueue().size()
        );

    }
}