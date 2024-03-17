package me.earth.phobot.pathfinder.algorithm.pooling;

import lombok.SneakyThrows;
import me.earth.phobot.TestUtil;
import me.earth.phobot.invalidation.ChunkWorker;
import me.earth.phobot.pathfinder.algorithm.AStar;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManager;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManagerTest;
import me.earth.phobot.pathfinder.util.Cancellation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PooledAStarTest {
    @Test
    @SneakyThrows
    public void testOnNavigationMeshManager() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            var level = NavigationMeshManagerTest.setupLevel(clientLevel);
            var meshManager = NavigationMeshManagerTest.createNavigationMeshManager();
            NavigationMeshManagerTest.setupMesh(level, meshManager);
            var path = findPathPooled(meshManager, new BlockPos(10, 1, 10), new BlockPos(0, 3, 0));
            assertNotNull(path);
            assertEquals(21, path.getPath().size());
            assertEquals(new MeshNode(meshManager.getPooling(), new ChunkWorker(), 0, 3, 0), path.getPath().get(0));
            assertEquals(new MeshNode(meshManager.getPooling(), new ChunkWorker(), 10, 1, 10), path.getPath().get(path.getPath().size() - 1));
        }
    }

    public static @Nullable Algorithm.Result<MeshNode> findPathPooled(NavigationMeshManager meshManager, BlockPos startPos, BlockPos goalPos) {
        MeshNode start = meshManager.getMap().get(startPos);
        assertNotNull(start);
        MeshNode goal = meshManager.getMap().get(goalPos);
        assertNotNull(goal);
        AStar<MeshNode> aStar = new PooledAStar<>(start, goal, meshManager.getPooling());
        return aStar.run(new Cancellation());
    }

}
