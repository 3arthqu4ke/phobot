package me.earth.phobot.pathfinder.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@UtilityClass
public class FutureUtil {
    public static <T> CompletableFuture<T> notNull(CompletableFuture<@Nullable T> future) {
        return future.thenApply(value -> {
            if (value == null) {
                throw new IllegalStateException("Result of computation was null!");
            }

            return value;
        });
    }

}
