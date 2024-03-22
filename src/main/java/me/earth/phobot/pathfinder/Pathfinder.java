package me.earth.phobot.pathfinder;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.modules.client.Pathfinding;
import me.earth.phobot.pathfinder.algorithm.AStar;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.algorithm.pooling.PooledAStar;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManager;
import me.earth.phobot.pathfinder.movement.MovementPathfinder;
import me.earth.phobot.pathfinder.render.AlgorithmRenderer;
import me.earth.phobot.pathfinder.render.AlgorithmRendererManager;
import me.earth.phobot.pathfinder.util.*;
import me.earth.phobot.services.TaskService;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.api.EventBus;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages Pathfinding in {@link Phobot}.
 * For more information on the pathfinding process read the documentation of the {@link MovementPathfinder}.
 *
 * @see MovementPathfinder
 */
@Slf4j
@Getter
public class Pathfinder extends MovementPathfinder {
    public static final long DEFAULT_TIME_OUT = TimeUnit.MILLISECONDS.toMillis(Long.parseLong(System.getProperty("phobot.pathfinder.timeout", "250")));

    private final LevelBoundTaskManager levelBoundTaskManager = new LevelBoundTaskManager();
    private final AlgorithmRendererManager algorithmRendererManager;
    private final NavigationMeshManager navigationMeshManager;
    private final ExecutorService executorService;
    private final Pathfinding configuration;
    private final TaskService taskService;
    private final PingBypass pingBypass;
    private final EventBus eventBus;

    public Pathfinder(PingBypass pingBypass, EventBus eventBus, NavigationMeshManager navigationMeshManager, ExecutorService executorService, TaskService taskService, Pathfinding configuration) {
        super(pingBypass);
        this.eventBus = eventBus;
        this.executorService = executorService;
        this.configuration = configuration;
        this.algorithmRendererManager = new AlgorithmRendererManager(configuration.getRenderAlgorithm());
        this.algorithmRendererManager.getListeners().forEach(this::listen);
        this.levelBoundTaskManager.getListeners().forEach(this::listen);
        this.navigationMeshManager = navigationMeshManager;
        this.taskService = taskService;
        this.pingBypass = pingBypass;
    }

    @Override
    public int getLagTime() {
        return configuration.getLagTime().getValue();
    }

    /**
     * Finds a path of {@link MeshNode}s from the current node the player is on to the given goal using {@link AStar}.
     *
     * @param player the player to get the starting position from.
     * @param goal the goal to reach.
     * @param render if you want to render the algorithm.
     * @return a {@link Process} representing the path finding process.
     */
    public CancellableFuture<Algorithm.@NotNull Result<MeshNode>> findPath(Player player, MeshNode goal, boolean render) {
        CancellableFuture<Algorithm.Result<MeshNode>> future;

        Optional<MeshNode> start = navigationMeshManager.getStartNode(player);
        if (start.isEmpty()) {
            future = new CancellableFuture<>(Cancellation.UNCANCELLABLE);
            future.completeExceptionally(new IllegalStateException("Could not find start mesh node"));
        } else {
            future = findPath(start.get(), goal, render);
        }

        return future;
    }

    /**
     * Finds a path of {@link MeshNode}s from the given start node to the give goal.
     *
     * @param start the node to start from.
     * @param goal the goal to reach.
     * @param render if you want to render the algorithm.
     * @return a {@link Process} representing the path finding process.
     */
    public CancellableFuture<Algorithm.@NotNull Result<MeshNode>> findPath(MeshNode start, MeshNode goal, boolean render) {
        CancellableFuture<Algorithm.Result<MeshNode>> future;
        var algorithm = new PooledAStar<>(start, goal, navigationMeshManager.getPooling());
        future = CancellationTaskUtil.runWithTimeOut(algorithm, taskService, DEFAULT_TIME_OUT, executorService);
        future = FutureUtil.notNull(future);
        levelBoundTaskManager.addFuture(future);
        if (render) {
            AlgorithmRenderer<MeshNode> renderer = new AlgorithmRenderer<>(algorithm);
            algorithmRendererManager.add(renderer);
            future.whenComplete((r,t) -> algorithmRendererManager.remove(renderer));
            // this additionally is very important
            // waiting for the future to complete is terrible when lots of Algorithms are running in parallel
            // because it could take quite a while for the cancellation to arrive at the algorithm
            taskService.addTaskToBeExecutedIn(DEFAULT_TIME_OUT, () -> algorithmRendererManager.remove(renderer));
        }

        return future;
    }

}
