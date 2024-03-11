package me.earth.phobot.pathfinder.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class FutureUtil {
    public static <T> CancellableFuture<T> notNull(CancellableFuture<@Nullable T> future) {
        return (CancellableFuture<T>) future.thenApply(value -> {
            if (value == null) {
                throw new IllegalStateException("Result of computation was null!");
            }

            return value;
        });
    }

}
