package me.earth.phobot.pathfinder.movement;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.TestPhobot;
import me.earth.phobot.TestUtil;
import me.earth.phobot.pathfinder.Path;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.algorithm.pooling.PooledAStar;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManagerTest;
import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static me.earth.phobot.TestUtil.setupBlockStateLevelFromJson;

/**
 * Collects data on which {@link MovementPathfindingAlgorithm#getBhopDistanceSq()} produces the best results.
 * It really seems that setting the bhop distance to unlimited is the best way to do this.
 */
@Slf4j
public class BhopDistanceDataCollection {
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

        @Cleanup
        FileOutputStream fos = new FileOutputStream("bhopdistance.csv");

        NavigationMeshManagerTest.setupMeshForBlockStateLevelMap(level, phobot.getNavigationMeshManager());
        MovementPlayer player = new MovementPlayer(level);

        @Cleanup
        FileOutputStream fos2 = new FileOutputStream("bestdistances.csv");

        for (int i = 0; i < 100; i++) {
            log.info("\n----\n");
            List<MeshNode> nodeList = new ArrayList<>(phobot.getNavigationMeshManager().getMap().values());
            nodeList.removeIf(m -> m.getY() > 20);
            Random random = new Random();
            int index1 = random.nextInt(nodeList.size());
            int index2 = random.nextInt(nodeList.size());

            MeshNode start = nodeList.get(index1);
            MeshNode goal = nodeList.get(index2);

            player.setPos(start.getCenter(new BlockPos.MutableBlockPos(), level));
            var aStarPath = new PooledAStar<>(start, goal, phobot.getNavigationMeshManager().getPooling()).run(new Cancellation());
            if (aStarPath == null || start.equals(goal)) {
                continue;
            }

            log.info("Found A* path from " + start + " to " + goal + " , length " + aStarPath.getPath().size());
            var path = new Path<>(start.getCenter(new BlockPos.MutableBlockPos(), level), goal.getCenter(new BlockPos.MutableBlockPos(), level), start.asBlockPos(), goal.asBlockPos(), aStarPath.order(Algorithm.Result.Order.START_TO_GOAL).getPath(), MeshNode.class);
            int bestBhopLength = Integer.MAX_VALUE;
            List<Double> bhopDistances = new ArrayList<>();
            for (double j = 0.0; j < path.getExactStart().distanceTo(path.getExactGoal()) + 3; j++) {
                MovementPathfindingAlgorithm algorithm = new MovementPathfindingAlgorithm(phobot, level, path, player, null, null);
                long time = System.nanoTime();
                algorithm.setBhopDistanceSq(j * j);
                //algorithm.setAcceptOtherMeshNodesReached(false);
                var movementPath = algorithm.run(new Cancellation());
                time = System.nanoTime() - time;

                if (movementPath == null) {
                    log.info("Failed to find MovementPath on path from " + start + " to " + goal);
                    continue;
                }

                var horizontalCollisions = 0;
                for (MovementNode node : movementPath.getPath()) {
                    if (node.snapshot().isHorizontalCollision()) {
                        horizontalCollisions++;
                    }
                }

                log.info("MovementPathfinder unoptimized took " + time + "ns (" + (time / TimeUtil.NANOS_PER_MS) + "ms) for bhop distance " + j + ", size: " + movementPath.getPath().size() + ", collisions: " + horizontalCollisions);
                fos.write((time + ";" + start.distance(goal) + ";" + path.getPath().size() + ";"
                        + movementPath.getPath().size() + ";" + horizontalCollisions + ";"
                        + (j / path.getExactStart().distanceTo(path.getExactGoal())) * 100 + "\n").getBytes(StandardCharsets.UTF_8));

                if (movementPath.getPath().size() < bestBhopLength) {
                    bestBhopLength = movementPath.getPath().size();
                    bhopDistances.clear();
                }

                if (movementPath.getPath().size() == bestBhopLength) {
                    bhopDistances.add(j);
                }
            }

            bhopDistances.forEach(d -> {
                try {
                    System.out.println((((d / path.getExactStart().distanceTo(path.getExactGoal())) * 100)) + " == " + ((int) ((d / path.getExactStart().distanceTo(path.getExactGoal())) * 100)));
                    fos2.write((d + "," + Math.min((int) ((d / path.getExactStart().distanceTo(path.getExactGoal())) * 100), 101)  + "\n").getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

}
