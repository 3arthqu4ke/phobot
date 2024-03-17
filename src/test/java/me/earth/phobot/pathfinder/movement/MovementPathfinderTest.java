package me.earth.phobot.pathfinder.movement;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.BlockableEventLoopImpl;
import me.earth.phobot.DelegatingPingBypass;
import me.earth.phobot.TestPhobot;
import me.earth.phobot.TestUtil;
import me.earth.phobot.event.MoveEvent;
import me.earth.phobot.event.PathfinderUpdateEvent;
import me.earth.phobot.movement.BunnyHopCC;
import me.earth.phobot.movement.NoStepMovement;
import me.earth.phobot.pathfinder.algorithm.AStarTest;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManagerTest;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.TestPingBypass;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class MovementPathfinderTest {
    @Test
    @SneakyThrows
    public void testMovementPathfinderFallingDown() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            var level = TestUtil.setupLevelFromJson(clientLevel, "worlds/CCWinterSpawn.json");
            var player = new MovementPlayer(level);
            player.setOnGround(true);
            player.verticalCollision = true;
            player.verticalCollisionBelow = true;
            player.setDeltaMovement(new Vec3(0.0, NoStepMovement.INSTANCE.getDeltaYOnGround(), 0.0));
            player.setPos(0.0, 131.0, 0.0);
            player.setMovement(new BunnyHopCC());
            var meshManager = NavigationMeshManagerTest.createNavigationMeshManager();
            NavigationMeshManagerTest.setupMesh(level, 141, meshManager);
            var meshNodePath = AStarTest.findPath(meshManager, new BlockPos(0, 131, 0), new BlockPos(4, 5, 5));
            assertNotNull(meshNodePath);
            var mc = TestUtil.createMinecraft(level);
            var pingBypass = new DelegatingPingBypass.WithMc(new TestPingBypass(), mc);
            var movementPathfinder = new MovementPathfinder(pingBypass) {
                @Override
                protected Player getPlayer(Player localPlayer) {
                    return player;
                }
            };

            pingBypass.getEventBus().subscribe(movementPathfinder);
            var phobot = TestPhobot.createNewTestPhobot();
            CompletableFuture<MovementPathfinder.Result> future = movementPathfinder.followScheduled(
                    phobot, new BlockableEventLoopImpl(), (action, orElse) -> action.accept(mc.player, mc.level, mc.gameMode), meshNodePath, new Vec3(4.5, 5.0, 5.5));
            assertTrue(movementPathfinder.isFollowingPath());

            updatePlayerForTicks(10_000, player, pingBypass, movementPathfinder);

            var result = future.getNow(null);
            assertEquals(MovementPathfinder.Result.FINISHED, result);
            assertEquals(new Vec3(4.5, 5.0, 5.5), player.position());
        }
    }

    /**
     * We had a big problem with "hot swapping" paths for the {@link MovementPathfinder}.
     * This means that we might be following a MeshNode path with our Pathfinder while concurrently
     * starting an A-Star Algorithm to calculate a path from the MeshNode our player is currently on to somewhere else.
     * The problem arises when the A-Star Algorithm finishes, and we want to follow its result path:
     * We have since then moved further with the MovementPathfinder and are out of range of the initial starting MeshNode.
     * This test covers the simplest case, we are falling down from spawn, and are pathfinding from the MeshNode we are going to land on,
     * then change paths mid-fall.
     */
    @Test
    @SneakyThrows
    public void testMovementPathfinderHotSwapping() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            var level = TestUtil.setupLevelFromJson(clientLevel, "worlds/CCWinterSpawn.json");
            var player = new MovementPlayer(level);
            player.setOnGround(true);
            player.verticalCollision = true;
            player.verticalCollisionBelow = true;
            player.setDeltaMovement(new Vec3(0.0, NoStepMovement.INSTANCE.getDeltaYOnGround(), 0.0));
            player.setPos(0.0, 131.0, 0.0);
            player.setMovement(new BunnyHopCC());
            var meshManager = NavigationMeshManagerTest.createNavigationMeshManager();
            NavigationMeshManagerTest.setupMesh(level, 141, meshManager);
            var meshNodePath = AStarTest.findPath(meshManager, new BlockPos(0, 131, 0), new BlockPos(4, 5, 5));
            assertNotNull(meshNodePath);
            var mc = TestUtil.createMinecraft(level);
            var pingBypass = new DelegatingPingBypass.WithMc(new TestPingBypass(), mc);
            var movementPathfinder = new MovementPathfinder(pingBypass) {
                @Override
                protected Player getPlayer(Player localPlayer) {
                    return player;
                }
            };

            pingBypass.getEventBus().subscribe(movementPathfinder);
            var phobot = TestPhobot.createNewTestPhobot();
            CompletableFuture<MovementPathfinder.Result> future = movementPathfinder.followScheduled(
                    phobot, new BlockableEventLoopImpl(), (action, orElse) -> action.accept(mc.player, mc.level, mc.gameMode), meshNodePath, new Vec3(4.5, 5.0, 5.5));
            assertTrue(movementPathfinder.isFollowingPath());

            Algorithm.Result<MeshNode> newPath = null;
            boolean followingNewPath = false;
            for (int i = 0; i < 10_000; i++) {
                AtomicBoolean called = new AtomicBoolean();
                player.setMoveCallback(delta -> {
                    var moveEvent = new MoveEvent(Vec3.ZERO);
                    pingBypass.getEventBus().post(moveEvent);
                    called.set(true);
                    return moveEvent.getVec();
                });

                player.aiTravel();
                assertTrue(called.get());
                pingBypass.getEventBus().post(new PathfinderUpdateEvent());
                if (player.getY() <= 40.0 && !followingNewPath) { // trigger at y "AStar has finished"
                    followingNewPath = true;
                    assertNotNull(newPath);
                    CompletableFuture<MovementPathfinder.Result> newFuture = movementPathfinder.followScheduled(
                            phobot, new BlockableEventLoopImpl(), (action, orElse) -> action.accept(mc.player, mc.level, mc.gameMode), meshNodePath, new Vec3(4.5, 5.0, 5.5));
                    var result = future.getNow(null);
                    assertNotNull(result);
                    assertEquals(MovementPathfinder.Result.NEW_PATH, result);
                    future = newFuture;
                }

                if (player.getY() <= 57.0 && newPath == null) { // start calculating at y 57, which is build height, bot behaviour "Chasing" has found path
                    newPath = AStarTest.findPath(meshManager, meshManager.getStartNode(player).map(MeshNode::asBlockPos).orElseThrow(), new BlockPos(4, 5, 5));
                }

                if (!movementPathfinder.isFollowingPath()) {
                    break;
                }
            }

            var result = future.getNow(null);
            assertEquals(MovementPathfinder.Result.FINISHED, result);
            assertEquals(new Vec3(4.5, 5.0, 5.5), player.position());
        }
    }

    public static void updatePlayerForTicks(int ticks, MovementPlayer player, PingBypass pingBypass, MovementPathfinder movementPathfinder) {
        for (int i = 0; i < ticks; i++) {
            AtomicBoolean called = new AtomicBoolean();
            player.setMoveCallback(delta -> {
                var moveEvent = new MoveEvent(delta);
                pingBypass.getEventBus().post(moveEvent);
                called.set(true);
                return moveEvent.getVec();
            });

            // TODO: stuckSpeedMultiplier?
            // TODO: set player speed! this.setSpeed((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED));
            player.aiTravel();
            assertTrue(called.get());
            pingBypass.getEventBus().post(new PathfinderUpdateEvent());
            if (!movementPathfinder.isFollowingPath()) {
                break;
            }
        }
    }

}
