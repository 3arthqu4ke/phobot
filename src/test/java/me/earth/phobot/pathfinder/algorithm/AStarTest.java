package me.earth.phobot.pathfinder.algorithm;

import lombok.SneakyThrows;
import me.earth.phobot.TestUtil;
import me.earth.phobot.invalidation.ChunkWorker;
import me.earth.phobot.pathfinder.blocks.BlockNode;
import me.earth.phobot.pathfinder.blocks.BlockNodePoolBuilder;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManager;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManagerTest;
import me.earth.phobot.pathfinder.util.Cancellation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AStarTest {
    @Test
    public void testAStar() {
        var builder = new BlockNodePoolBuilder();
        var nodes = builder.build(10, BlockNode::new);
        var start = builder.getCenter();
        BlockNode goal = null;
        for (var node : nodes) {
            node.setToOffsetFromCenter(BlockPos.ZERO);
            if (node.getCurrent().equals(new BlockPos(3, 2, 2))) {
                goal = node;
            }
        }

        assertNotNull(goal);
        Cancellation cancellation = new Cancellation();
        AStar<BlockNode> aStar = new AStar<>(start, goal);
        var result = aStar.run(cancellation);
        assertNotNull(result);
        assertEquals(new BlockNode(new BlockPos(3, 2, 2)), result.getPath().get(0));
        assertEquals(new BlockNode(new BlockPos(3, 1, 2)), result.getPath().get(1));
        assertEquals(new BlockNode(new BlockPos(3, 1, 1)), result.getPath().get(2));
        assertEquals(new BlockNode(new BlockPos(2, 1, 1)), result.getPath().get(3));
        assertEquals(new BlockNode(new BlockPos(2, 0, 1)), result.getPath().get(4));
        assertEquals(new BlockNode(new BlockPos(2, 0, 0)), result.getPath().get(5));
        assertEquals(new BlockNode(new BlockPos(1, 0, 0)), result.getPath().get(6));
        assertEquals(new BlockNode(new BlockPos(0, 0, 0)), result.getPath().get(7));
        assertFalse(cancellation.isCancelled());
        cancellation.setCancelled(true);
        assertTrue(cancellation.isCancelled());
        aStar = new AStar<>(start, goal);
        result = aStar.run(cancellation);
        assertNull(result);
    }

    @Test
    @SneakyThrows
    public void testOnNavigationMeshManager() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            var level = NavigationMeshManagerTest.setupLevel(clientLevel);
            var meshManager = NavigationMeshManagerTest.createNavigationMeshManager();
            NavigationMeshManagerTest.setupMesh(level, meshManager);
            var path = findPath(meshManager, new BlockPos(10, 1, 10), new BlockPos(0, 3, 0));
            assertNotNull(path);
            assertEquals(21, path.getPath().size());
            assertEquals(new MeshNode(meshManager.getPooling(), new ChunkWorker(), 0, 3, 0), path.getPath().get(0));
            assertEquals(new MeshNode(meshManager.getPooling(), new ChunkWorker(), 10, 1, 10), path.getPath().get(path.getPath().size() - 1));
        }
    }

    public static @Nullable Algorithm.Result<MeshNode> findPath(NavigationMeshManager meshManager, BlockPos startPos, BlockPos goalPos) {
        MeshNode start = meshManager.getMap().get(startPos);
        assertNotNull(start);
        MeshNode goal = meshManager.getMap().get(goalPos);
        assertNotNull(goal);
        AStar<MeshNode> aStar = new AStar<>(start, goal);
        return aStar.run(new Cancellation());
    }

}
