package me.earth.phobot.pathfinder.mesh;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.invalidation.ChunkWorker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Objects;

/**
 * Recalculates the Graph at a certain position if a block turned into a non-collision block.
 */
@Slf4j
public class AirBlockTask extends AbstractMeshInvalidationTask {
    public AirBlockTask(BlockableEventLoop<Runnable> scheduler, Level level, ChunkWorker worker, NavigationMeshManager manager, BlockPos pos) {
        super(scheduler, level, worker, manager, pos);
    }

    @Override
    public void execute() {
        if (changedPos.getY() > getMaxY() || changedPos.getY() < getMinY() || getMap().get(changedPos) != null) {
            return;
        }
        /* What we need to check:
               ...
              7   7
              7 2 7
              3 1 3
              3 4 3
                4
                5
                6
                6
                6
               ...

             1. is the block that turned into air. A new node could have been formed here if the block underneath is solid.
             2. if the block above was a node we need to invalidate it, its solid block turned into air
             3. for these blocks the block could have blocked the way down to nodes 4, 5 or 6 and that way is now free, also it could mean that we can no longer step up from 3 to 2
             4. Nodes underneath could have formed.
             5. Node could have formed, gotten headspace
             6/7. We could now be able to jump down from side nodes 3 or 7 down to 4,5,6
         */
        getPos().set(changedPos.getX(), changedPos.getY() + 1, changedPos.getZ());
        // checking 2
        MeshNode node = getMap().get(getPos());
        if (node != null) {
            removeNode(getPos(), node);
        }
        // look for the node that formed underneath 1,4,5,6
        MeshNode lowest = null;
        for (int y = changedPos.getY(); y >= getMinY(); y--) {
            getPos().set(changedPos.getX(), y, changedPos.getZ());
            lowest = getMap().get(getPos());
            if (lowest == null) {
                lowest = calcNode(getPos());
            }

            if (lowest != null) {
                break;
            }
        }
        // hole through bedrock probably or something else
        if (lowest == null) {
            return;
        }
        // update nodes around 3,7
        int minY = Math.max(changedPos.getY(), lowest.getY() + (lowest.isHeadSpace() ? 2 : 1)); // minimum y which is still air above our changed block
        int maxY = Integer.MAX_VALUE; // maximum y which is another node above our node
        boolean minYFixed = false;
        for (int i = 0; i < MeshNode.OFFSETS.length; i++) {
            Vec3i offset = MeshNode.OFFSETS[i];
            getPos().set(changedPos.getX() + offset.getX(), changedPos.getY() + offset.getY(), changedPos.getZ() + offset.getZ());
            for (MeshNode adjacent : getManager().getXZMap().getOrDefault(getPos(), Collections.emptySet())) {
                if (adjacent.getY() == changedPos.getY() || adjacent.getY() == changedPos.getY() - 1) { // 3
                    // stepping up from lowest to adjacent is handled before in the calculation of lowest
                    adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]] = lowest;
                } else if (adjacent.getY() > changedPos.getY() && adjacent.getY() <= maxY) { // 7
                    MeshNode above = adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]];
                    if (above != null && above.isValid() && above != lowest && above.getY() > lowest.getY()) {
                        maxY = above.getY(); // there is a node above our changed block that we can drop down on from 7
                    } else //noinspection DuplicatedCode // TODO: its only this bit where we calculate and fix minY
                        if (!minYFixed && minY < adjacent.getY() + 1) {
                        // check blocks above our changed block to see if we can jump down from adjacent to lowest
                        for (int y = minY + 1; y <= adjacent.getY() + 1; y++) {
                            getPos().set(changedPos.getX(), y, changedPos.getZ());
                            if (!Objects.requireNonNull(getLevel()).getBlockState(getPos()).getCollisionShape(getLevel(), getPos()).isEmpty()) {
                                minYFixed = true;
                                break;
                            }

                            minY = y;
                        }
                    }

                    if (minY >= adjacent.getY() + 1) {
                        adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]] = lowest;
                        if (adjacent.getY() - lowest.getY() <= 2 && canStepUp(getPos(), lowest, adjacent)) { // we can step from 1 to 7
                            lowest.getAdjacent()[i] = adjacent;
                        }
                    }
                }
            }
        }
    }

}
