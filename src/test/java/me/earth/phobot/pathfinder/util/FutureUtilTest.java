package me.earth.phobot.pathfinder.util;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class FutureUtilTest {
    @Test
    public void testNonNullCompletedWithNull() {
        String[] array = new String[]{ "1" };

        CompletableFuture<@Nullable String> future = new CompletableFuture<>();
        CompletableFuture<String> nonNullFuture = FutureUtil.notNull(future);
        nonNullFuture.thenApply(s -> array[0] = s).exceptionally(t -> array[0] = t.getMessage());
        future.complete(null);

        assertEquals("java.lang.IllegalStateException: Result of computation was null!", array[0]);
    }

    @Test
    public void testNonNullCompletedWithNonNull() {
        String[] array = new String[]{ "1" };

        CompletableFuture<@Nullable String> future = new CompletableFuture<>();
        CompletableFuture<String> nonNullFuture = FutureUtil.notNull(future);
        nonNullFuture.thenApply(s -> array[0] = s).exceptionally(t -> array[0] = t.getMessage());
        future.complete("2");

        assertEquals("2", array[0]);
    }

    @Test
    public void testNonNullCompletedExceptionally() {
        String[] array = new String[]{ "1" };

        CompletableFuture<@Nullable String> future = new CompletableFuture<>();
        CompletableFuture<String> nonNullFuture = FutureUtil.notNull(future);
        nonNullFuture.thenApply(s -> array[0] = s).exceptionally(t -> array[0] = t.getMessage());
        future.completeExceptionally(new IllegalStateException("Completed Exceptionally"));

        assertEquals("java.lang.IllegalStateException: Completed Exceptionally", array[0]);
    }

    @Test
    public void testWithCancellableFuture() {
        CancellableFuture<@Nullable String> future = new CancellableFuture<>(Cancellation.UNCANCELLABLE);
        CompletableFuture<String> nonNullFuture = FutureUtil.notNull(future);
        assertInstanceOf(CancellableFuture.class, nonNullFuture);
    }

}
