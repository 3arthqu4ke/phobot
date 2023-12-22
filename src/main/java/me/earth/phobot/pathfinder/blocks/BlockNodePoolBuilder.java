package me.earth.phobot.pathfinder.blocks;

import lombok.Getter;
import me.earth.phobot.util.PositionPool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class BlockNodePoolBuilder extends PositionPool.Builder<BlockNode> {
    // most often we'll build up from the ground so down is first and up is last
    public static final Direction[] ALL_DIRECTIONS_UP_LAST = new Direction[] { Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP };
    public static final int[] OPPOSITE_INDEX = new int[] { 5, 2, 1, 4, 3, 0 };
    private final Map<BlockPos, BlockNode> map = new HashMap<>();
    private BlockNode center;

    @Override
    protected boolean inRange(BlockPos pos, int radius) {
        return pos.distManhattan(BlockPos.ZERO) <= radius;
    }

    @Override
    protected void add(BlockNode pos, List<BlockNode> positions) {
        super.add(pos, positions);
        map.put(pos.getOffsetToCenter(), pos);
        if (pos.getOffsetToCenter().equals(BlockPos.ZERO)) {
            center = pos;
        }

        discoverAdjacent(pos);
    }

    protected void discoverAdjacent(BlockNode pos) {
        for (int i = 0; i < ALL_DIRECTIONS_UP_LAST.length; i++) {
            Direction direction = ALL_DIRECTIONS_UP_LAST[i];
            pos.getCurrent().set(pos.getOffsetToCenter()); // reset
            pos.getCurrent().setRelative(direction);
            BlockNode adjacent = map.get(pos.getCurrent());
            if (adjacent != null) {
                pos.getAdjacent()[i] = adjacent;
                adjacent.getAdjacent()[OPPOSITE_INDEX[i]] = pos;
            }
        }
    }

}
