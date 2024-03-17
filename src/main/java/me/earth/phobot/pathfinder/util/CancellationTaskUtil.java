package me.earth.phobot.pathfinder.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.services.TaskService;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;

/**
 * Utility for working with {@link CancellableFuture}s.
 */
@Slf4j
@UtilityClass
public class CancellationTaskUtil {
    public static <T> CancellableFuture<T> run(CancellableTask<T> task, Executor executor) {
        return runWithTimeOut(task, null, 0L, executor);
    }

    public static <T> CancellableFuture<T> runWithTimeOut(CancellableTask<T> task, @Nullable TaskService taskService, long timeout, Executor executor) {
        var cancellation = new Cancellation();
        var future = new CancellableFuture.WithExecutor<T>(cancellation, executor);
        future.completeAsync(() -> {
            if (taskService != null) {
                taskService.addTaskToBeExecutedIn(timeout, () -> {
                    if (!future.isDone() && !future.isCancelled()) {
                        log.info("Task " + task + " has reached the timeout of " + timeout + "ms, cancelling...");
                        future.cancel(true);
                        System.gc();
                    }
                });
            }

            return task.run(cancellation);
        });

        return future;
    }

}
