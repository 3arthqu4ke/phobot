package me.earth.phobot.pathfinder.parallelization;

import lombok.Setter;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.util.CancellableFuture;
import me.earth.phobot.services.TaskService;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A {@link ParallelPathSearch} but the order in which futures are registered is important.
 * It waits for the first added future to complete.
 * If this has not happened after the timeout configured by {@link #registerTimeoutManager(TaskService, long)},
 * the result of the next future that has been registered and that has completed will be returned.
 *
 * @param <T> the type of key to use to index the running algorithms.
 */
@Setter
public class PrioritizingParallelPathSearch<T> extends ParallelPathSearch<T> {
    private final Queue<T> keyQueue = new LinkedList<>();

    public void registerTimeoutManager(TaskService taskService, long timeout) {
        taskService.addTaskToBeExecutedIn(timeout, this::allowOtherFuturesToComplete);
    }

    @Override
    protected void registerPathFutureFinishedListener(T key, CancellableFuture<Algorithm.Result<MeshNode>> pathFuture) {
        if (keyQueue.isEmpty()) { // primary future!
            super.registerPathFutureFinishedListener(key, pathFuture); // this future cancels all futures, it is the most important one
            pathFuture.whenComplete((r,t) -> {
                if (t != null) { // when completed exceptionally allow all other futures to complete this
                    allowOtherFuturesToComplete();
                }
            });
        }

        keyQueue.add(key);
    }

    protected void allowOtherFuturesToComplete() {
        synchronized (future) {
            keyQueue.forEach(key -> {
                var future = futures.get(key);
                if (future != null) {
                    super.registerPathFutureFinishedListener(key, future);
                }
            });
        }
    }

}
