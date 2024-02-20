package me.earth.phobot.holes;

import lombok.Getter;
import lombok.Setter;
import me.earth.phobot.invalidation.ChunkWorker;
import me.earth.phobot.util.mutables.MutPos;
import net.minecraft.core.BlockPos;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;

/**
 * Finds holes which could've been created when a block turned into air.
 */
@Getter
@Setter
final class AirHoleTask extends HoleTask {
    private ChunkWorker chunk;
    private int minX;
    private int maxX;
    private int minY;
    private int maxY;
    private int minZ;
    private int maxZ;

    public AirHoleTask(BlockableEventLoop<Runnable> scheduler, Level level, HoleManager holeManager) {
        super(holeManager.getMap(), scheduler, level, new MutPos(), holeManager, null, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("AirHoleTask.run() is not supported");
    }

    @Override
    public void execute() {
        for (int x = getMinX(); x < getMaxX(); x++) {
            for (int z = getMinZ(); z < getMaxZ(); z++) {
                for (int y = getMinY(); y <= getMaxY(); y++) {
                    Hole hole = getMap().get(getPos().set(x, y, z));
                    if (hole == null || !hole.isValid()) {
                        calc(getPos());
                    }
                }
            }
        }
    }

    public void setPos(BlockPos pos) {
        // we calculate a 3 block deep (along the y-axis), 2x2 cuboid
        //    x a <- this block turned into air
        //    x x
        // because we can make use of the fact that the holecalc only uses the bottom left airblock of the hole.
        // that means this 2x2x3 cubicle will find all holes which could've been created by the pos becoming air.
        setMaxX(pos.getX() + 1);
        setMinX(pos.getX() - 1);
        setMaxY(pos.getY());
        setMinY(pos.getY() - 2);
        setMaxZ(pos.getZ() + 1);
        setMinZ(pos.getZ() - 1);
    }

}