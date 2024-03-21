package me.earth.phobot.pathfinder.mesh;

import lombok.SneakyThrows;
import me.earth.phobot.BlockableEventLoopImpl;
import me.earth.phobot.Phobot;
import me.earth.phobot.TestPhobot;
import me.earth.phobot.TestUtil;
import me.earth.phobot.invalidation.ChunkWorker;
import me.earth.phobot.modules.client.Pathfinding;
import me.earth.phobot.movement.BunnyHopCC;
import me.earth.phobot.util.collections.XZMap;
import me.earth.phobot.util.mutables.MutPos;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NavigationMeshManagerTest {
    @Test
    @SneakyThrows
    public void testNavigationMeshManager() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            var level = setupLevel(clientLevel);
            var meshManager = createNavigationMeshManager();
            setupMesh(level, meshManager);
            assertEquals(new MeshNode(meshManager.getPooling(), new ChunkWorker(), 0, 3, 0), meshManager.getMap().get(new BlockPos(0, 3, 0)));
            assertEquals(new MeshNode(meshManager.getPooling(), new ChunkWorker(), 4, 4, 4), meshManager.getMap().get(new BlockPos(4, 4, 4)));
            assertEquals(new MeshNode(meshManager.getPooling(), new ChunkWorker(), 1, 1, 1), meshManager.getMap().get(new BlockPos(1, 1, 1)));
            assertNull(meshManager.getMap().get(new BlockPos(1, 2, 1)));
        }
    }

    public static NavigationMeshManager createNavigationMeshManager() {
        return createNavigationMeshManager(0);
    }

    public static NavigationMeshManager createNavigationMeshManager(int minHeight) {
        Phobot phobot = TestPhobot.createNewTestPhobot();
        Pathfinding module = new Pathfinding(phobot.getPingBypass(), phobot.getExecutorService());
        module.getSetting("MinHeight", Integer.class).orElseThrow().setValue(minHeight);
        module.getCalcAsync().setValue(false);
        return new NavigationMeshManager(module, new BunnyHopCC(), new HashMap<>(), new XZMap<>(new HashMap<>()));
    }

    public static BlockStateLevel.Delegating setupLevel(ClientLevel clientLevel) {
        return TestUtil.setupLevel(clientLevel, NavigationMeshManagerTest::setupBlockStateLevel);
    }

    public static void setupBlockStateLevel(BlockStateLevel.Delegating level) {
        for (int x = -20; x < 20; x++) {
            for (int z = -20; z < 20; z++) {
                BlockPos pos = new BlockPos(x, 0, z);
                level.getMap().put(pos, Blocks.BEDROCK.defaultBlockState());
            }
        }

        level.getMap().put(new BlockPos(0, 1, 0), Blocks.BEDROCK.defaultBlockState());
        level.getMap().put(new BlockPos(0, 2, 0), Blocks.BEDROCK.defaultBlockState());

        level.getMap().put(new BlockPos(4, 3, 4), Blocks.BEDROCK.defaultBlockState());
    }

    public static void setupMesh(BlockStateLevel.Delegating level, NavigationMeshManager navigationMeshManager) {
        setupMesh(level, 5, navigationMeshManager);
    }

    public static void setupMesh(BlockStateLevel.Delegating level, int maxY, NavigationMeshManager navigationMeshManager) {
        setupMesh(level, -1, maxY, navigationMeshManager);
    }

    public static void setupMesh(BlockStateLevel.Delegating level, int minY, int maxY, NavigationMeshManager navigationMeshManager) {
        BlockableEventLoop<Runnable> eventLoop = new BlockableEventLoopImpl();
        MeshTask task = new MeshTask(navigationMeshManager.getMap(), eventLoop, level, new MutPos(), navigationMeshManager, new ChunkWorker(), -21, 21, minY, maxY, -21, 21);
        task.run();
    }

    public static void setupMeshForBlockStateLevelMap(BlockStateLevel.Delegating level, NavigationMeshManager navigationMeshManager) {
        BlockableEventLoop<Runnable> eventLoop = new BlockableEventLoopImpl();
        Queue<BlockPos> queue = new PriorityQueue<>(); // <- BlockPos compares by y, lowest first, which is just right for GraphTasks!
        queue.addAll(level.getMap().keySet());
        MeshTask task = new MeshTask(navigationMeshManager.getMap(), eventLoop, level, new MutPos(), navigationMeshManager, new ChunkWorker(), 0, 0, 0, 0, 0, 0) {
            @Override
            public void execute() {
                while (!queue.isEmpty()) {
                    BlockPos pos = queue.poll();
                    getPos().set(pos.getX(), pos.getY() + 1, pos.getZ());
                    if (!navigationMeshManager.getMap().containsKey(getPos())) {
                        calc(getPos());
                    }
                }
            }
        };

        task.run();
    }

}
