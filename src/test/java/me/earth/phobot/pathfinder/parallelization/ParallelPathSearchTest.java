package me.earth.phobot.pathfinder.parallelization;

import lombok.SneakyThrows;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.util.CancellableFuture;
import me.earth.phobot.pathfinder.util.Cancellation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class ParallelPathSearchTest {
    @Test
    @SneakyThrows
    public void testAddFuture() {
        ParallelPathSearch<String> parallelPathSearch = new ParallelPathSearch<>();
        var future1 = new CancellableFuture<Algorithm.@NotNull Result<MeshNode>>(new Cancellation());
        var future2 = new CancellableFuture<Algorithm.@NotNull Result<MeshNode>>(new Cancellation());
        var future3 = new CancellableFuture<Algorithm.@NotNull Result<MeshNode>>(new Cancellation());
        parallelPathSearch.addFuture("1", future1);
        parallelPathSearch.addFuture("2", future2);
        parallelPathSearch.addFuture("3", future3);
        parallelPathSearch.allFuturesAdded();

        assertFalse(parallelPathSearch.future.isDone());
        var future2Result = new Algorithm.Result<MeshNode>(new ArrayList<>(), Algorithm.Result.Order.START_TO_GOAL);
        future2.complete(future2Result);

        assertTrue(parallelPathSearch.future.isDone());
        CancellableSearch.Result<String> result = parallelPathSearch.future.getNow(null);
        assertEquals("2", result.key());
        assertSame(future2Result, result.algorithmResult());
    }

    @Test
    @SneakyThrows
    public void testCancellation() {
        ParallelPathSearch<String> parallelPathSearch = new ParallelPathSearch<>();
        var future1 = new CancellableFuture<Algorithm.@NotNull Result<MeshNode>>(new Cancellation());
        var future2 = new CancellableFuture<Algorithm.@NotNull Result<MeshNode>>(new Cancellation());
        var future3 = new CancellableFuture<Algorithm.@NotNull Result<MeshNode>>(new Cancellation());
        parallelPathSearch.addFuture("1", future1);
        parallelPathSearch.addFuture("2", future2);
        parallelPathSearch.addFuture("3", future3);
        parallelPathSearch.allFuturesAdded();

        assertFalse(parallelPathSearch.future.isDone());
        parallelPathSearch.setCancelled(true);
        assertTrue(future1.isCancelled());
        assertTrue(future2.isCancelled());
        assertTrue(future3.isCancelled());
        assertTrue(parallelPathSearch.future.isDone());
        assertTrue(parallelPathSearch.future.isCompletedExceptionally());
    }

}
