package me.earth.phobot.holes;

import lombok.Getter;
import lombok.Setter;
import me.earth.phobot.invalidation.ChunkWorker;
import me.earth.phobot.util.mutables.MutPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;

/**
 * Finds holes which could have been created when a block was placed.
 */
@Getter
@Setter
final class BlockHoleTask extends HoleTask {
    private static final Vec3i[] OFFSETS = new Vec3i[]{
        // When a block is added at 'p' we check these x offsets:
        // this makes use of the fact that our hole check function only
        // finds 2x1 holes and 2x2 holes on the bottom left air block.
        //    x x
        //  x x p x    and at 4 positions y + 1:   x x
        //  x x x x                                x x
        //    x x
        // I attempted to order the offsets in a way where the ones that
        // invalidate most of the other offsets if they are a hole come first.
        new Vec3i(-1, 0, -1),
        new Vec3i(0, 0, -1),
        new Vec3i(-1, 0, 0),
        new Vec3i(-1, 1, -1),
        new Vec3i(-1, 0, -2),
        new Vec3i(-2, 0, -1),
        new Vec3i(-1, 0, 1),
        new Vec3i(1, 0, -1),
        new Vec3i(-1, 1, 0),
        new Vec3i(0, 1, -1),
        new Vec3i(0, 0, 1),
        new Vec3i(-2, 0, 0),
        new Vec3i(1, 0, 0),
        new Vec3i(0, 0, -2),
        new Vec3i(0, 1, 0)
    };

    private ChunkWorker chunk;
    private int x;
    private int y;
    private int z;

    public BlockHoleTask(BlockableEventLoop<Runnable> scheduler, Level level, HoleManager holeManager) {
        super(holeManager.getMap(), scheduler, level, new MutPos(), holeManager, null, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("BlockHoleTask.run() is not supported");
    }

    @Override
    public void execute() {
        for (Vec3i off : OFFSETS) {
            Hole hole = getMap().get(getPos().set(getX() + off.getX(), getY() + off.getY(), getZ() + off.getZ()));
            if (hole == null || !hole.isValid()) {
                calc(getPos());
            }
        }
    }

    public void setPos(BlockPos pos) {
        setX(pos.getX());
        setY(pos.getY());
        setZ(pos.getZ());
    }

}

