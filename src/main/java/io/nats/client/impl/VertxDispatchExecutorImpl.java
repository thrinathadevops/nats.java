package io.nats.client.impl;


import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class VertxDispatchExecutorImpl implements DispatchExecutor {
    private final Vertx vertx;

    public VertxDispatchExecutorImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public Future<Boolean> submit(final Runnable task) {
        final CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        io.vertx.core.Future<Boolean> booleanFuture = vertx.executeBlocking(event -> {
            task.run();
            event.complete(true);
        });
        booleanFuture.onFailure(completableFuture::completeExceptionally).onSuccess(completableFuture::complete);
        return completableFuture;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {

        final CompletableFuture<T> completableFuture = new CompletableFuture<>();
        io.vertx.core.Future<T> vertxFuture = vertx.executeBlocking(event -> {
            try {
                event.complete(task.call());
            } catch (Exception e) {
                event.fail(e);
            }
        });
        vertxFuture.onFailure(completableFuture::completeExceptionally).onSuccess(completableFuture::complete);
        return completableFuture;

    }

    @Override
    public void execute(final Runnable command) {
        vertx.executeBlocking((Handler<Promise<Void>>) event -> {
            try {
                command.run();
                event.complete(null);
            } catch (Exception ex) {
                event.fail(ex);
            }
        });
    }

    @Override
    public boolean isShutdown() {
        return false;
    }
}
