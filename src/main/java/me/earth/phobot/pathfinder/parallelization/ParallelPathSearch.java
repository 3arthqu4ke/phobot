package me.earth.phobot.pathfinder.parallelization;

import lombok.Getter;
import me.earth.phobot.pathfinder.Pathfinder;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.util.CancellableFuture;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allows you to run multiple pathfinding searches with {@link Algorithm}s in parallel and retrieves the first result that gets found.
 *
 * @param <T> the type of key to use to index the running algorithms.
 */
@Getter
public class ParallelPathSearch<T> extends CancellableSearch<T> {
    protected final Map<T, CancellableFuture<Algorithm.Result<MeshNode>>> futures = new ConcurrentHashMap<>();

    @Override
    public void setCancelled(boolean cancelled) {
        super.setCancelled(cancelled);
        if (cancelled) {
            futures.values().forEach(future -> future.cancel(true));
        }
    }

    @Override
    public void allFuturesAdded() {
        CompletableFuture<?> allFutures = CompletableFuture.allOf(futures.values().stream().distinct().toArray(CompletableFuture[]::new));
        allFutures.whenComplete((r,t) -> {
            synchronized (future) {
                if (!future.isDone() && !future.isCancelled()) {
                    future.completeExceptionally(new NoSuchElementException("None of the given futures returned a value."));
                }
            }
        });
    }

    public void findPath(T key, Pathfinder pathfinder, Player player, MeshNode goal, boolean render) {
        if (futures.containsKey(key)) {
            return;
        }

        var pathFuture = pathfinder.findPath(player, goal, render);
        addFuture(key, pathFuture);
    }

    public void addFuture(T key, CancellableFuture<Algorithm.Result<MeshNode>> pathFuture) {
        var before = futures.put(key, pathFuture);
        if (before != null) {
            before.cancel(true);
        }

        registerPathFutureFinishedListener(key, pathFuture);
    }

    protected void registerPathFutureFinishedListener(T key, CancellableFuture<Algorithm.Result<MeshNode>> pathFuture) {
        pathFuture.thenAccept(result -> {
            synchronized (future) {
                if (!future.isDone() && !future.isCancelled()) {
                    future.complete(new Result<>(result, key));
                    futures.values().forEach(f -> f.cancel(true));
                }
            }
        });
    }

}
