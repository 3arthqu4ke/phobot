package me.earth.phobot.pathfinder.movement;

import lombok.SneakyThrows;
import me.earth.phobot.TestPhobot;
import me.earth.phobot.TestUtil;
import me.earth.phobot.pathfinder.Path;
import me.earth.phobot.pathfinder.algorithm.AStarTest;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManagerTest;
import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class MovementPathfinderTest {
    @Test
    @SneakyThrows
    public void testMovementPathfinder() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            var level = NavigationMeshManagerTest.setupLevel(clientLevel);
            var player = new MovementPlayer(level);
            var meshManager = NavigationMeshManagerTest.createNavigationMeshManager();
            NavigationMeshManagerTest.setupMesh(level, meshManager);
            var meshNodePath = AStarTest.findPath(meshManager, new BlockPos(10, 1, 10), new BlockPos(0, 3, 0));
            assertNotNull(meshNodePath);
            var path = new Path<>(new Vec3(10.5, 1, 10.5), new Vec3(0.5, 3, 0.5), new BlockPos(10, 1, 10), new BlockPos(0, 3, 0), Collections.emptySet(), Algorithm.reverse(meshNodePath), MeshNode.class);
            MovementPathfindingAlgorithm algorithm = new MovementPathfindingAlgorithm(TestPhobot.PHOBOT, level, path, player, null, null);
            var movementNodePath = algorithm.run(new Cancellation.ThreadInterrupted());
            assertNotNull(movementNodePath);
            assertTrue(movementNodePath.size() > 2);
            assertTrue(algorithm.getStart().positionEquals(movementNodePath.get(0)));
            assertTrue(movementNodePath.get(0).isStart());
            assertTrue(algorithm.getGoal().positionEquals(movementNodePath.get(movementNodePath.size() - 1)));
            assertTrue(movementNodePath.get(movementNodePath.size() - 1).isGoal());
        }
    }

    @Test
    @SneakyThrows
    public void testMovementPathfinderOnCCSpawn() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            var level = NavigationMeshManagerTest.setupLevel(clientLevel, CrystalPvPCCWinterSpawn::setupBlockStateLevel);
            var player = new MovementPlayer(level);
            player.setPos(0.0, 131.0, 0.0);
            var meshManager = NavigationMeshManagerTest.createNavigationMeshManager();
            NavigationMeshManagerTest.setupMesh(level, 141, meshManager);
            var meshNodePath = AStarTest.findPath(meshManager, new BlockPos(0, 131, 0), new BlockPos(4, 5, 5));
            assertNotNull(meshNodePath);
            var path = new Path<>(new Vec3(0.0, 131.0, 0.0), new Vec3(4.5, 5.0, 5.5), new BlockPos(0, 131, 0), new BlockPos(4, 5, 5), Collections.emptySet(), Algorithm.reverse(meshNodePath), MeshNode.class);
            MovementPathfindingAlgorithm algorithm = new MovementPathfindingAlgorithm(TestPhobot.PHOBOT, level, path, player, null, null);
            var movementNodePath = algorithm.run(new Cancellation.ThreadInterrupted());
            assertNotNull(movementNodePath);
        }
    }

}
