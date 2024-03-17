package me.earth.phobot.pathfinder.algorithm.pooling;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.BlockableEventLoopImpl;
import me.earth.phobot.Phobot;
import me.earth.phobot.TestPhobot;
import me.earth.phobot.TestUtil;
import me.earth.phobot.pathfinder.algorithm.AStar;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManagerTest;
import me.earth.phobot.pathfinder.parallelization.HasPriority;
import me.earth.phobot.pathfinder.parallelization.ParallelSearchManager;
import me.earth.phobot.pathfinder.util.CancellationTaskUtil;
import me.earth.phobot.services.TaskService;
import me.earth.phobot.util.time.StopWatch;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static me.earth.phobot.TestUtil.setupBlockStateLevelFromJson;

/**
 * This is the worst benchmark ever.
 * But, ngl I was surprised by how severe the results were.
 * The pooled AStar algorithm seem run like 10 times faster.
 */
@Slf4j
public class PoolingBenchmark {
    @SneakyThrows
    public static void main(String[] args) {
        Phobot phobot = TestPhobot.createNewTestPhobot();
        @Cleanup
        ClientLevel clientLevel = TestUtil.createClientLevel();
        BlockStateLevel.Delegating level = new BlockStateLevel.Delegating(clientLevel);
        log.info("Loading world...");
        setupBlockStateLevelFromJson("worlds/CCWinterSpawn.json", (pos, block) -> {
            for (int x = -40; x < 40; x++) {
                for (int z = -40; z < 40; z++) {
                    level.getMap().put(new BlockPos(pos.getX() + x * 5, pos.getY(), pos.getZ() + z * 5), block.defaultBlockState());
                }
            }
        });

        var blockableEventLoop = new BlockableEventLoopImpl();
        // the actual task service will fail because Minecraft is null
        Field eventLoopFieldInTaskService = TaskService.class.getDeclaredField("mc");
        eventLoopFieldInTaskService.setAccessible(true);
        eventLoopFieldInTaskService.set(phobot.getTaskService(), blockableEventLoop);

        // Setup World, map size: 1385823
        // Setup Mesh for World, size: 164418
        log.info("Setup World, map size: " + level.getMap().size());
        NavigationMeshManagerTest.setupMeshForBlockStateLevelMap(level, phobot.getNavigationMeshManager());
        log.info("Setup Mesh for World, size: " + phobot.getNavigationMeshManager().getMap().size());

        MeshNode start = phobot.getNavigationMeshManager().getMap().values().stream().min(Comparator.comparingDouble(node -> node.distanceSq(0, 10, 0))).orElseThrow();
        List<MeshNode> goals = phobot.getNavigationMeshManager().getMap().values().stream().sorted(Comparator.comparingDouble(node -> node.distanceSq(0, 131, 0))).limit(50).toList();
        log.info("Start: " + start + " : " + Arrays.toString(start.getAdjacent()) + ", goals: " + goals);

        ParallelSearchManager parallelSearchManager = new ParallelSearchManager();
        HasPriority provider = () -> 0;
        StopWatch.ForMultipleThreads stopWatch = new StopWatch.ForMultipleThreads();
        for (int i = 0; i < 5; i++) {
            var currentSearch = parallelSearchManager.<MeshNode>applyForPathSearch(provider, multiPathSearch -> {
                goals.forEach(goal -> multiPathSearch.addFuture(goal, CancellationTaskUtil.run(new AStar<>(start, goal), phobot.getExecutorService())));
                stopWatch.reset();
                multiPathSearch.getFuture().whenComplete((r,t) -> log.info("Multi path search with A* took " + stopWatch.getPassedTime() + "ms"));
            });

            assert currentSearch != null;
            try {
                currentSearch.get();
            } catch (Exception ignored) {}
            System.gc();
        }

        for (int i = 0; i < 5; i++) {
            var currentSearch = parallelSearchManager.<MeshNode>applyForPathSearch(provider, multiPathSearch -> {
                goals.forEach(goal -> multiPathSearch.addFuture(goal,
                        CancellationTaskUtil.run(new PooledAStar<>(start, goal, phobot.getNavigationMeshManager().getPooling()), phobot.getExecutorService())));
                stopWatch.reset();
                multiPathSearch.getFuture().whenComplete((r,t) -> log.info("Multi path search pool A* took " + stopWatch.getPassedTime() + "ms"));
            });

            assert currentSearch != null;
            try {
                currentSearch.get();
            } catch (Exception ignored) {}
            System.gc();
        }

        log.info("Real benchmark now:");

        for (int i = 0; i < 5; i++) {
            var currentSearch = parallelSearchManager.<MeshNode>applyForPathSearch(provider, multiPathSearch -> {
                goals.forEach(goal -> multiPathSearch.addFuture(goal,
                        CancellationTaskUtil.run(new PooledAStar<>(start, goal, phobot.getNavigationMeshManager().getPooling()), phobot.getExecutorService())));
                stopWatch.reset();
                multiPathSearch.getFuture().whenComplete((r,t) -> log.info("Multi path search pool A* took " + stopWatch.getPassedTime() + "ms"));
            });

            assert currentSearch != null;
            try {
                currentSearch.get();
            } catch (Exception ignored) {}
            System.gc();
        }

        for (int i = 0; i < 5; i++) {
            var currentSearch = parallelSearchManager.<MeshNode>applyForPathSearch(provider, multiPathSearch -> {
                goals.forEach(goal -> multiPathSearch.addFuture(goal, CancellationTaskUtil.run(new AStar<>(start, goal), phobot.getExecutorService())));
                stopWatch.reset();
                multiPathSearch.getFuture().whenComplete((r,t) -> log.info("Multi path search with A* took " + stopWatch.getPassedTime() + "ms"));
            });

            assert currentSearch != null;
            try {
                currentSearch.get();
            } catch (Exception ignored) {}
            System.gc();
        }
    }
}
