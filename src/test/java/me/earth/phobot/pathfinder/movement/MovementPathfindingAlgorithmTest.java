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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MovementPathfindingAlgorithmTest {
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
            var path = new Path<>(new Vec3(10.5, 1, 10.5), new Vec3(0.5, 3, 0.5), new BlockPos(10, 1, 10), new BlockPos(0, 3, 0),
                    meshNodePath.order(Algorithm.Result.Order.START_TO_GOAL).getPath(), MeshNode.class);
            MovementPathfindingAlgorithm algorithm = new MovementPathfindingAlgorithm(TestPhobot.createNewTestPhobot(), level, path, player, null, null);
            var movementNodePath = algorithm.run(new Cancellation.ThreadInterrupted());
            assertNotNull(movementNodePath);
            assertTrue(movementNodePath.getPath().size() > 2);
            assertTrue(algorithm.getStart().positionEquals(movementNodePath.getPath().get(0)));
            assertTrue(movementNodePath.getPath().get(0).isStart());
            assertTrue(algorithm.getGoal().positionEquals(movementNodePath.getPath().get(movementNodePath.getPath().size() - 1)));
            assertTrue(movementNodePath.getPath().get(movementNodePath.getPath().size() - 1).isGoal());
        }
    }

    @Test
    @SneakyThrows
    public void testMovementPathfinderOnCCSpawn() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            var level = TestUtil.setupLevelFromJson(clientLevel, "worlds/CCWinterSpawn.json");
            var player = new MovementPlayer(level);
            player.setPos(0.0, 131.0, 0.0);
            var meshManager = NavigationMeshManagerTest.createNavigationMeshManager();
            NavigationMeshManagerTest.setupMesh(level, 141, meshManager);
            var meshNodePath = AStarTest.findPath(meshManager, new BlockPos(0, 131, 0), new BlockPos(4, 5, 5));
            assertNotNull(meshNodePath);
            var path = new Path<>(new Vec3(0.0, 131.0, 0.0), new Vec3(4.5, 5.0, 5.5), new BlockPos(0, 131, 0), new BlockPos(4, 5, 5),
                    meshNodePath.order(Algorithm.Result.Order.START_TO_GOAL).getPath(), MeshNode.class);
            MovementPathfindingAlgorithm algorithm = new MovementPathfindingAlgorithm(TestPhobot.createNewTestPhobot(), level, path, player, null, null);
            var movementNodePath = algorithm.run(new Cancellation.ThreadInterrupted());
            assertNotNull(movementNodePath);
            assertTrue(movementNodePath.order(Algorithm.Result.Order.GOAL_TO_START).getPath().get(0).isGoal());
            assertEquals(4.5, movementNodePath.order(Algorithm.Result.Order.GOAL_TO_START).getPath().get(0).getX());
            assertEquals(5.0, movementNodePath.order(Algorithm.Result.Order.GOAL_TO_START).getPath().get(0).getY());
            assertEquals(5.5, movementNodePath.order(Algorithm.Result.Order.GOAL_TO_START).getPath().get(0).getZ());
        }
    }

    @Test
    @SneakyThrows
    public void testSinglePlayerWorld1GettingStuckWhenFallingWithFastFall() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            var level = TestUtil.setupLevelFromJson(clientLevel, "worlds/SinglePlayer1.json");
            var player = new MovementPlayer(level);
            player.setPos(0.5, 1.0, 0.5);
            var meshManager = NavigationMeshManagerTest.createNavigationMeshManager(-12);
            NavigationMeshManagerTest.setupMesh(level, -12, 12, meshManager);
            var meshNodePath = AStarTest.findPath(meshManager, new BlockPos(0, 1, 0), new BlockPos(-4, -6, 1));
            assertNotNull(meshNodePath);
            var path = new Path<>(new Vec3(0.5, 1.0, 0.5), new Vec3(-4, -6, 1), new BlockPos(0, 1, 0), new BlockPos(-4, -6, 1),
                    meshNodePath.order(Algorithm.Result.Order.START_TO_GOAL).getPath(), MeshNode.class);
            MovementPathfindingAlgorithm algorithm = new MovementPathfindingAlgorithm(TestPhobot.createNewTestPhobot(), level, path, player, null, null);
            var movementNodePath = algorithm.run(new Cancellation.ThreadInterrupted());
            assertNotNull(movementNodePath);
        }
    }

    @Test
    @SneakyThrows
    public void testFallDownNarrowHole() {
        /* A problem, funnily for players too, is getting into 1x1 holes, if you are too fast you might just walk over it
           The world to test looks something like this (s == start, g == goal):

           x x x   x x x x x x s
               x   x
               x g x
               x x x
         */
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            var level = TestUtil.setupLevelFromJson(clientLevel, "worlds/FallDownNarrowHole.json");
            var player = new MovementPlayer(level);
            player.setPos(0.5, 1.0, 0.5);
            var meshManager = NavigationMeshManagerTest.createNavigationMeshManager(-12);
            NavigationMeshManagerTest.setupMesh(level, -12, 12, meshManager);
            var meshNodePath = AStarTest.findPath(meshManager, new BlockPos(0, 1, 0), new BlockPos(0, -3, 8));
            assertNotNull(meshNodePath);
            var path = new Path<>(new Vec3(0.5, 1.0, 0.5), new Vec3(0.5, -3.0, 8.5), new BlockPos(0, 1, 0), new BlockPos(0, -3, 8),
                    meshNodePath.order(Algorithm.Result.Order.START_TO_GOAL).getPath(), MeshNode.class);
            MovementPathfindingAlgorithm algorithm = new MovementPathfindingAlgorithm(TestPhobot.createNewTestPhobot(), level, path, player, null, null);
            var movementNodePath = algorithm.run(new Cancellation.ThreadInterrupted());
            assertNotNull(movementNodePath);
        }
    }

    @Test
    @SneakyThrows
    public void testFallDownNarrowHoleVeryCloseToEdge() {
        // the same, as above, just now we are sitting right at the edge
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            var level = TestUtil.setupLevelFromJson(clientLevel, "worlds/FallDownNarrowHole.json");
            var player = new MovementPlayer(level);
            player.setPos(0.5, 1.0, 8.3); // we can stand exactly 0.3 units over the edge here
            player.verticalCollision = true;
            player.verticalCollisionBelow = true;
            player.setOnGround(true);
            var meshManager = NavigationMeshManagerTest.createNavigationMeshManager(-12);
            NavigationMeshManagerTest.setupMesh(level, -12, 12, meshManager);
            var meshNodePath = AStarTest.findPath(meshManager, new BlockPos(0, 1, 7), new BlockPos(0, -3, 8));
            assertNotNull(meshNodePath);
            var path = new Path<>(new Vec3(0.5, 1.0, 8.3), new Vec3(0.5, -3.0, 8.5), new BlockPos(0, 1, 7), new BlockPos(0, -4, 8),
                                  meshNodePath.order(Algorithm.Result.Order.START_TO_GOAL).getPath(), MeshNode.class);
            MovementPathfindingAlgorithm algorithm = new MovementPathfindingAlgorithm(TestPhobot.createNewTestPhobot(), level, path, player, null, null);
            var movementNodePath = algorithm.run(new Cancellation.ThreadInterrupted());
            // TODO: seems to strafe back?
            assertNotNull(movementNodePath);
        }
    }

}
