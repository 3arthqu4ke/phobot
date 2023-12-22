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
import java.util.function.Consumer;

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
            assertEquals(new MeshNode(new ChunkWorker(), 0, 3, 0), meshManager.getMap().get(new BlockPos(0, 3, 0)));
            assertEquals(new MeshNode(new ChunkWorker(), 4, 4, 4), meshManager.getMap().get(new BlockPos(4, 4, 4)));
            assertEquals(new MeshNode(new ChunkWorker(), 1, 1, 1), meshManager.getMap().get(new BlockPos(1, 1, 1)));
            assertNull(meshManager.getMap().get(new BlockPos(1, 2, 1)));
        }
    }

    public static NavigationMeshManager createNavigationMeshManager() {
        Phobot phobot = TestPhobot.PHOBOT;
        Pathfinding module = new Pathfinding(phobot.getPingBypass(), phobot.getExecutorService());
        module.getCalcAsync().setValue(false);
        return new NavigationMeshManager(module, new BunnyHopCC(), new HashMap<>(), new XZMap<>(new HashMap<>()));
    }

    public static BlockStateLevel.Delegating setupLevel(ClientLevel clientLevel) {
        return setupLevel(clientLevel, NavigationMeshManagerTest::setupBlockStateLevel);
    }

    public static BlockStateLevel.Delegating setupLevel(ClientLevel clientLevel, Consumer<BlockStateLevel.Delegating> consumer) {
        BlockStateLevel.Delegating level = new BlockStateLevel.Delegating(clientLevel) {
            @Override
            public boolean hasChunkAt(BlockPos pos) {
                return true;
            }
        };

        consumer.accept(level);
        return level;
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
        BlockableEventLoop<Runnable> eventLoop = new BlockableEventLoopImpl();
        MeshTask task = new MeshTask(navigationMeshManager.getMap(), eventLoop, level, new MutPos(), navigationMeshManager, new ChunkWorker(), -21, 21, -1, maxY, -21, 21);
        task.run();
    }

}
