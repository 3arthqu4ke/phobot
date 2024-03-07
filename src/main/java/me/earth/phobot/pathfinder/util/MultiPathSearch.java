package me.earth.phobot.pathfinder.util;

import lombok.Getter;
import me.earth.phobot.pathfinder.Pathfinder;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

// TODO: test
@Getter
public class MultiPathSearch extends Cancellation {
    private final Set<CancellableFuture<Algorithm.Result<MeshNode>>> futures = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final CancellableFuture<Algorithm.Result<MeshNode>> future = new CancellableFuture<>(this);

    @Override
    public void setCancelled(boolean cancelled) {
        super.setCancelled(cancelled);
        if (cancelled) {
            futures.forEach(future -> future.cancel(true));
        }
    }

    public void findPath(Pathfinder pathfinder, Player player, MeshNode goal, boolean render) {
        var future = pathfinder.findPath(player, goal, render);
        futures.add(future);
        future.thenAccept(result -> {
            synchronized (future) {
                if (!future.isDone() && !future.isCancelled()) {
                    future.complete(result);
                    futures.forEach(f -> f.cancel(true));
                }
            }
        });
    }

    public void allFuturesAdded() {
        CompletableFuture<?> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.whenComplete((r,t) -> {
            synchronized (future) {
                if (!future.isDone() && !future.isCancelled()) {
                    future.completeExceptionally(new NoSuchElementException("None of the given futures returned a value."));
                }
            }
        });
    }

}
