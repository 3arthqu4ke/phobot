package me.earth.phobot.pathfinder.util;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RequiredArgsConstructor
public class CancellableFuture<T> extends CompletableFuture<T> {
    protected final Cancellation cancellation;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!isDone()) {
            cancellation.setCancelled(true);
        }

        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return new CancellableFuture<>(cancellation);
    }

    public static class WithExecutor<T> extends CancellableFuture<T> {
        protected final @Nullable Executor executor;

        public WithExecutor(Cancellation cancellation, @Nullable Executor executor) {
            super(cancellation);
            this.executor = executor;
        }

        @Override
        public <U> CompletableFuture<U> newIncompleteFuture() {
            return new WithExecutor<>(cancellation, executor);
        }

        @Override
        public Executor defaultExecutor() {
            return executor == null ? super.defaultExecutor() : executor;
        }
    }

}
