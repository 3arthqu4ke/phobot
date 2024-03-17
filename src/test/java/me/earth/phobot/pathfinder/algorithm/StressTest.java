package me.earth.phobot.pathfinder.algorithm;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.BlockableEventLoopImpl;
import me.earth.phobot.Phobot;
import me.earth.phobot.TestPhobot;
import me.earth.phobot.TestUtil;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManagerTest;
import me.earth.phobot.pathfinder.parallelization.HasPriority;
import me.earth.phobot.pathfinder.parallelization.ParallelSearchManager;
import me.earth.phobot.services.TaskService;
import me.earth.phobot.util.time.StopWatch;
import me.earth.phobot.util.world.BlockStateLevel;
import me.earth.pingbypass.api.event.api.EventListener;
import me.earth.pingbypass.api.event.loop.GameloopEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static me.earth.phobot.TestUtil.setupBlockStateLevelFromJson;

@Slf4j
@SuppressWarnings("unchecked")
public class StressTest {
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

        Thread thread = Thread.currentThread();
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

        StopWatch.ForSingleThread timer = new StopWatch.ForSingleThread();
        StopWatch.ForSingleThread fpsTimer = new StopWatch.ForSingleThread();
        int fps = 0;
        while (!thread.isInterrupted()) {
            if (!parallelSearchManager.isSearching()) {
                parallelSearchManager.<MeshNode>applyForPathSearch(provider, multiPathSearch -> {
                    log.info("Starting 50 path searches after " + timer.getPassedTime() + "ms...");
                    goals.forEach(goal -> multiPathSearch.addFuture(goal, phobot.getPathfinder().findPath(start, goal, false)));
                    timer.reset();
                });
            }

            if (fpsTimer.passed(1_000L)) {
                log.info("Fps: " + fps);
                fpsTimer.reset();
                fps = 0;
            }

            fps++;
            blockableEventLoop.runAllTasks();
            phobot.getTaskService().getListeners()
                    .stream()
                    .filter(l -> l.getType() == GameloopEvent.class)
                    .forEach(l -> ((EventListener<GameloopEvent>) l).onEvent(GameloopEvent.INSTANCE));
        }
    }

}
