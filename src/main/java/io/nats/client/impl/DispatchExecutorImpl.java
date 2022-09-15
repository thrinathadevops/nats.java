package io.nats.client.impl;

import io.nats.client.Options;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DispatchExecutorImpl implements DispatchExecutor {

    private final ExecutorService executorService;
    private final Duration waitForShutdown;


    public DispatchExecutorImpl(ExecutorService executorService, Duration waitForShutdown) {
        this.executorService = executorService;
        this.waitForShutdown = waitForShutdown;
    }

    @Override
    public Future<Boolean> submit(Runnable task) {
        return executorService.submit(task, Boolean.TRUE);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }

    @Override
    public void execute(Runnable command) {
         executorService.execute(command);
    }

    @Override
    public void close() {

        if(waitForShutdown!=null) {
            // Stop the error handling and connect executors
            executorService.shutdown();
            try {
                try {
                    executorService.awaitTermination(waitForShutdown.toNanos(), TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                }
            } finally {
                executorService.shutdownNow();
            }
        } else {
            executorService.shutdownNow();
        }
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }
}
