package me.earth.phobot.pathfinder.parallelization;

import lombok.Getter;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.util.CancellableFuture;
import me.earth.phobot.pathfinder.util.Cancellation;

@Getter
public abstract class CancellableSearch<T> extends Cancellation {
    protected final CancellableFuture<Result<T>> future = new CancellableFuture<>(this);

    public abstract void allFuturesAdded();

    public record Result<V>(Algorithm.Result<MeshNode> algorithmResult, V key) { }

}
