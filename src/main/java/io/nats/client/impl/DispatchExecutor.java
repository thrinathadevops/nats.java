package io.nats.client.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface DispatchExecutor {
    Future<Boolean> submit(Runnable task);

    <T> Future<T> submit(Callable<T> task);

    void execute(Runnable command);

    default void close() {}

    boolean isShutdown();
}
