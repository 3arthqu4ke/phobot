package me.earth.phobot.holes;

import lombok.*;
import me.earth.phobot.invalidation.CanBeInvalidated;
import me.earth.phobot.invalidation.ChunkWorker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// TODO: tbh this should not have a special equals/hashcode implementation
@Data
@RequiredArgsConstructor
public final class Hole implements CanBeInvalidated {
    @EqualsAndHashCode.Exclude
    private final Set<BlockPos> positions = new HashSet<>();
    private final ChunkWorker chunk;
    private final int version;
    private boolean valid = true;
    private final int x;
    private final int y;
    private final int z;
    private final int maxX;
    private final int maxZ;
    private final boolean safe;
    @Getter(AccessLevel.NONE)
    private final boolean _1x1;
    @Getter(AccessLevel.NONE)
    private final boolean _2x1;
    @Getter(AccessLevel.NONE)
    private final boolean _2x2;
    /**
     * Because Holes might occur multiple times in {@link HoleManager#getMap()} this is to quickly check if we have already visited a hole while iterating over the map.
     * This value is only to be accessed from Minecrafts main thread!
     */
    private int visitId;
    @EqualsAndHashCode.Exclude
    private Map<BlockPos, Hole> map;

    public Hole(ChunkWorker chunk, int x, int y, int z, int maxX, int maxZ, boolean is2x1, boolean is2x2, boolean safe) {
        // TODO: chunk.getVersion() is not exactly safe, the Task producing this should supply it!
        this(chunk, chunk.getVersion(), x, y, z, maxX, maxZ, safe, !is2x1 && !is2x2, is2x1, is2x2);
    }

    @Override
    public boolean isValid() {
        return valid && chunk.getVersion() == this.version;
    }

    @Override
    public void invalidate() {
        setValid(false);
        if (map != null) {
            for (BlockPos pos : positions) {
                map.remove(pos, this);
            }
        }
    }

    public boolean is1x1() {
        return _1x1;
    }

    public boolean is2x1() {
        return _2x1;
    }

    public boolean is2x2() {
        return _2x2;
    }

    public boolean isAirPart(BlockPos pos) {
        return isAirPart(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean isAirPart(int x, int y, int z) {
        return x >= getX()
                && y >= getY()
                && z >= getZ()
                && x < getMaxX()
                && y < getY() + 2
                && z < getMaxZ();
    }

    public double getDistanceSqr(Entity entity) {
        return entity.distanceToSqr(getX() + (getMaxX() - getX()) / 2.0, getY(), getZ() + (getMaxZ() - getZ()) / 2.0);
    }

    public Set<BlockPos> getAirParts() {
        Set<BlockPos> airParts = new HashSet<>();
        for (int airX = x; airX < maxX; airX++) {
            for (int airZ = z; airZ < maxZ; airZ++) {
                airParts.add(new BlockPos(airX, getY(), airZ));
            }
        }

        return airParts;
    }

}
