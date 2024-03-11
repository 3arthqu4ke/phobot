package me.earth.phobot.pathfinder.util;

import lombok.Getter;
import me.earth.phobot.pathfinder.Pathfinder;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class MultiPathSearch<T> extends Cancellation {
    private final Map<T, CancellableFuture<Algorithm.Result<MeshNode>>> futures = new ConcurrentHashMap<>();
    private final CancellableFuture<MultiPathSearch.Result<T>> future = new CancellableFuture<>(this);

    @Override
    public void setCancelled(boolean cancelled) {
        super.setCancelled(cancelled);
        if (cancelled) {
            futures.values().forEach(future -> future.cancel(true));
        }
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

        pathFuture.thenAccept(result -> {
            synchronized (future) {
                if (!future.isDone() && !future.isCancelled()) {
                    future.complete(new Result<>(result, key));
                    futures.values().forEach(f -> f.cancel(true));
                }
            }
        });
    }

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

    public record Result<V>(Algorithm.Result<MeshNode> algorithmResult, V key) { }

}
