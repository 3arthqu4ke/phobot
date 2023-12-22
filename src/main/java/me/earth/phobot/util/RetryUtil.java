package me.earth.phobot.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import me.earth.pingbypass.api.util.exceptions.ThrowingSupplier;

import java.util.function.Supplier;

@UtilityClass
public class RetryUtil {
    @SneakyThrows
    public static <T> T retryOrThrow(int retries, Supplier<Throwable> throwable, ThrowingSupplier<T, ?> supplier) {
        for (int i = 0; i < retries; i++) {
            try {
                return supplier.get();
            } catch (Exception ignored) {

            }
        }

        throw throwable.get();
    }

}
