package me.earth.phobot.pathfinder.parallelization;

import me.earth.phobot.BlockableEventLoopImpl;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.util.CancellableFuture;
import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.pathfinder.util.LevelBoundTaskManager;
import me.earth.phobot.services.TaskService;
import me.earth.pingbypass.api.event.api.EventListener;
import me.earth.pingbypass.api.event.loop.GameloopEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertSame;

public class PrioritizingParallelPathSearchTest {
    @Test
    public void testPrioritizingParallelPathSearch() {
        PrioritizingParallelPathSearch<String> parallelPathSearch = new PrioritizingParallelPathSearch<>();
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

        assertFalse(parallelPathSearch.future.isDone());
        var future1Result = new Algorithm.Result<MeshNode>(new ArrayList<>(), Algorithm.Result.Order.START_TO_GOAL);
        future1.complete(future1Result);

        assertTrue(parallelPathSearch.future.isDone());
        CancellableSearch.Result<String> result = parallelPathSearch.future.getNow(null);
        assertEquals("1", result.key());
        assertSame(future1Result, result.algorithmResult());
    }

    @Test
    public void testCancelPrimaryFuture() {
        PrioritizingParallelPathSearch<String> parallelPathSearch = new PrioritizingParallelPathSearch<>();
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

        assertFalse(parallelPathSearch.future.isDone());
        future1.cancel(true);

        assertTrue(parallelPathSearch.future.isDone());
        CancellableSearch.Result<String> result = parallelPathSearch.future.getNow(null);
        assertEquals("2", result.key());
        assertSame(future2Result, result.algorithmResult());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWithTimeoutManager() {
        PrioritizingParallelPathSearch<String> parallelPathSearch = new PrioritizingParallelPathSearch<>();
        var future1 = new CancellableFuture<Algorithm.@NotNull Result<MeshNode>>(new Cancellation());
        var future2 = new CancellableFuture<Algorithm.@NotNull Result<MeshNode>>(new Cancellation());
        var future3 = new CancellableFuture<Algorithm.@NotNull Result<MeshNode>>(new Cancellation());
        parallelPathSearch.addFuture("1", future1);
        parallelPathSearch.addFuture("2", future2);
        parallelPathSearch.addFuture("3", future3);
        parallelPathSearch.allFuturesAdded();

        assertFalse(parallelPathSearch.future.isDone());
        var future3Result = new Algorithm.Result<MeshNode>(new ArrayList<>(), Algorithm.Result.Order.START_TO_GOAL);
        future3.complete(future3Result);

        var future2Result = new Algorithm.Result<MeshNode>(new ArrayList<>(), Algorithm.Result.Order.START_TO_GOAL);
        future2.complete(future2Result);

        assertFalse(parallelPathSearch.future.isDone());
        TaskService taskService = new TaskService(new BlockableEventLoopImpl());
        parallelPathSearch.registerTimeoutManager(taskService, -1);
        ((EventListener<GameloopEvent>) taskService.getListeners().get(0)).onEvent(GameloopEvent.INSTANCE);

        assertTrue(parallelPathSearch.future.isDone());
        CancellableSearch.Result<String> result = parallelPathSearch.future.getNow(null);
        assertEquals("2", result.key());
        assertSame(future2Result, result.algorithmResult());
    }

}
