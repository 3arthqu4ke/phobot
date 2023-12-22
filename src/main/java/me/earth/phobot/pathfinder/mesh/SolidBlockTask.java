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
 * Recalculates the Graph at a certain position if a block turned into a block we can collide with.
 */
@Slf4j
public class SolidBlockTask extends AbstractMeshInvalidationTask {
    public SolidBlockTask(BlockableEventLoop<Runnable> scheduler, Level level, ChunkWorker worker, NavigationMeshManager manager, BlockPos pos) {
        super(scheduler, level, worker, manager, pos);
    }

    @Override
    public void execute() {
        if (changedPos.getY() > getMaxY() || changedPos.getY() < getMinY()) {
            return;
        }
        /* What we need to check:
               ...
              6   6
              6 2 6
              3 1 3
              3 4 3
              7 5 7
              7   7

             1. is the block that turned into a block. A node at this spot could now be invalid.
             2. the block above. If there was a node here we do not need to do anything
             3. nodes at these spots could now be blocked from jumping down to 4,5,6, but could step up to 2
             4. this nodes air is now blocked, invalidate
             5. nodes headspace is gone, we can no longer step up to 3
             6. we can no longer drop down to 1,4 or 5
             7. we can no longer step up to 4/1, this is already checked when invalidating 4 or 1
         */
        getPos().set(changedPos.getX(), changedPos.getY() + 1, changedPos.getZ());
        // checking 2
        MeshNode node = getMap().get(getPos());
        if (node != null) { // changedPos was already solid, nothing changed
            return;
        }
        // calculate if a new node formed on 2
        node = calcNode(getPos());
        // check nodes at 1 and 4
        getPos().set(changedPos);
        for (int y = changedPos.getY(); y >= changedPos.getY() - 1; y--) {
            getPos().setY(y);
            MeshNode blockedNode = getMap().get(getPos());
            if (blockedNode != null) {
                removeNode(getPos(), blockedNode);
                break;
            }
        }
        // check 5
        getPos().setY(changedPos.getY() - 2);
        MeshNode five = getMap().get(getPos());
        if (five != null) {
            five.setHeadSpace(false);
            // we can no longer step up from 5 to 3
            for (int i = 0; i < five.getAdjacent().length; i++) {
                MeshNode adjacent = five.getAdjacent()[i];
                if (adjacent != null && adjacent.getY() > five.getY()) {
                    five.getAdjacent()[i] = null;
                    if (adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]] == five) {
                        adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]] = null;
                    }
                }
            }
        }
        // check 6 and 3
        int minY = (node == null ? changedPos.getY() : node.getY() + (node.isHeadSpace() ? 2 : 1)); // minimum y which is still air above our changed block
        int maxY = Integer.MAX_VALUE; // maximum y which is another node above our node
        boolean minYFixed = false;
        for (int i = 0; i < MeshNode.OFFSETS.length; i++) {
            Vec3i offset = MeshNode.OFFSETS[i];
            for (MeshNode adjacent : getManager().getXZMap().getOrDefault(changedPos.getX() + offset.getX(), changedPos.getZ() + offset.getZ(), Collections.emptySet())) {
                int adjacentDiff;
                if (adjacent.getY() > changedPos.getY()) { // 6
                    MeshNode twoOrAbove = adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]];
                    if (twoOrAbove != null && twoOrAbove.isValid() && twoOrAbove.getY() > changedPos.getY() + 1) { // node would mean a block somewhere above 2
                        maxY = Math.min(twoOrAbove.getY() - 1, maxY);
                        continue;
                    }

                    if (twoOrAbove != null && twoOrAbove.getY() <= changedPos.getY()) {
                        adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]] = null; // cant jump down past the changed block
                    }

                    if (node != null && adjacent.getY() <= maxY) {
                        //noinspection DuplicatedCode // TODO: its only this bit where we calculate and fix minY
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

                        if (minY >= adjacent.getY() + 1) { // we can jump down from 6 to here
                            adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]] = node;
                            if (adjacent.getY() - node.getY() <= 2 && canStepUp(getPos(), node, adjacent)) { // we can step up from node to 6
                                node.getAdjacent()[i] = adjacent;
                            }
                        }
                    }
                } else if (((adjacentDiff = adjacent.getY() - changedPos.getY()) == 0 || adjacentDiff == -1)) { // 3
                    if (node == null) {
                        // we can no longer jump down from 3 to another node
                        MeshNode opposite = adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]];
                        if (opposite != null && opposite.getY() <= changedPos.getY()) {
                            adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]] = null;
                        }
                    } else {
                        boolean canStepUp = adjacent.isHeadSpace();
                        if (canStepUp && adjacentDiff == -1) { // to step up from the bottom 3 to 2 we need one more block of headspace
                            getPos().set(adjacent.getX(), adjacent.getY() + 3, adjacent.getZ());
                            if (!Objects.requireNonNull(getLevel()).getBlockState(getPos()).getCollisionShape(getLevel(), getPos()).isEmpty()) {
                                // cannot step up from the bottom 3
                                canStepUp = false;
                            }
                        }

                        if (canStepUp && canStepUp(getPos(), adjacent, node)) {
                            adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]] = node;
                        } else {
                            adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]] = null;
                        }
                    }
                }
            }
        }
    }

}
