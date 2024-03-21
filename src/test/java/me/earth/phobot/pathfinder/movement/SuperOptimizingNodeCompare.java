package me.earth.phobot.pathfinder.movement;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.TestPhobot;
import me.earth.phobot.TestUtil;
import me.earth.phobot.movement.NoStepMovement;
import me.earth.phobot.pathfinder.Path;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.algorithm.pooling.PooledAStar;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManagerTest;
import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

import static me.earth.phobot.TestUtil.setupBlockStateLevelFromJson;

@Slf4j
public class SuperOptimizingNodeCompare {
    @SneakyThrows
    public static void main(String[] args) {
        Phobot phobot = TestPhobot.createNewTestPhobot();
        @Cleanup
        ClientLevel clientLevel = TestUtil.createClientLevel();
        BlockStateLevel.Delegating level = new BlockStateLevel.Delegating(clientLevel);
        setupBlockStateLevelFromJson("worlds/CCWinterSpawn.json", (pos, block) -> {
            for (int x = -10; x < 10; x++) {
                for (int z = -10; z < 10; z++) {
                    level.getMap().put(new BlockPos(pos.getX() + x * 5, pos.getY(), pos.getZ() + z * 5), block.defaultBlockState());
                }
            }
        });

        NavigationMeshManagerTest.setupMeshForBlockStateLevelMap(level, phobot.getNavigationMeshManager());
        // Looking for path from 3i(2, 3, 2) to 3i(-3, 3, -3)
        //MeshNode start = phobot.getNavigationMeshManager().getMap().values().stream().min(Comparator.comparingDouble(m -> m.distanceSq(10000, -100, 10000))).orElseThrow();
        MeshNode start = phobot.getNavigationMeshManager().getMap().values().stream().min(Comparator.comparingDouble(m -> m.distanceSq(8, -10, 8))).orElseThrow();
        MovementPlayer player = new MovementPlayer(level);
        player.setPos(start.getCenter(new BlockPos.MutableBlockPos(), level));
        player.setOnGround(true);
        player.setDeltaMovement(new Vec3(0.0, NoStepMovement.INSTANCE.getDeltaYOnGround(), 0.0));
        MeshNode goal = phobot.getNavigationMeshManager().getMap().values().stream().min(Comparator.comparingDouble(m -> m.distanceSq(-8, -10, -8))).orElseThrow();

        log.info("Looking for path from " + start + " to " + goal);
        log.info("Flight distance: " + start.distance(goal) + "m.");
        Algorithm.Result<MeshNode> aStarPath = new PooledAStar<>(start, goal, phobot.getNavigationMeshManager().getPooling()).run(new Cancellation());
        assert aStarPath != null;
        log.info("Found A* path, length " + aStarPath.getPath().size());
        var path = new Path<>(start.getCenter(new BlockPos.MutableBlockPos(), level), goal.getCenter(new BlockPos.MutableBlockPos(), level), start.asBlockPos(), goal.asBlockPos(), aStarPath.order(Algorithm.Result.Order.START_TO_GOAL).getPath(), MeshNode.class);

        int horizontalCollisions = 0;
        OptimizingMovementPathfindingAlgorithm optimizingAlg = null;
        Algorithm.Result<OptimizingNode> optimizedPath = null;
        for (int i = 0; i < 1; i++) {
            optimizingAlg = OptimizingMovementPathfindingAlgorithm.optimize(new MovementPathfindingAlgorithm(phobot, level, path, player, null, null));
            long time = System.nanoTime();
            optimizedPath = optimizingAlg.run(new Cancellation());
            time = System.nanoTime() - time;
            log.info("Optimized MovementPathfinder took " + time + "ns., loops: " + optimizingAlg.loops);
        }

        assert optimizedPath != null;
        horizontalCollisions = 0;
        for (OptimizingNode node : optimizedPath.getPath()) {
            if (node.movementNode.snapshot().isHorizontalCollision()) {
                horizontalCollisions++;
            }
        }

        log.info("Optimized Algorithm, size: " + optimizedPath.getPath().size() + ", collisions: " + horizontalCollisions);
        log.info("\n----\n");

        for (int i = 0; i < 1; i++) {
            optimizingAlg = OptimizingMovementPathfindingAlgorithm.superOptimizeSingleDirectPath(new MovementPathfindingAlgorithm(phobot, level, path, player, null, null));
            long time = System.nanoTime();
            optimizedPath = optimizingAlg.run(new Cancellation());
            time = System.nanoTime() - time;
            log.info("Optimized MovementPathfinder took " + time + "ns., loops: " + optimizingAlg.loops);
        }

        assert optimizedPath != null;
        horizontalCollisions = 0;
        for (OptimizingNode node : optimizedPath.getPath()) {
            if (node.movementNode.snapshot().isHorizontalCollision()) {
                horizontalCollisions++;
            }
        }

        log.info("Super-Optimized Algorithm, size: " + optimizedPath.getPath().size() + ", collisions: " + horizontalCollisions);
    }

}
