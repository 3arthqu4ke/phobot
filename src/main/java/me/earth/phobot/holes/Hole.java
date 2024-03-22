package me.earth.phobot.holes;

import lombok.*;
import me.earth.phobot.invalidation.CanBeInvalidated;
import me.earth.phobot.invalidation.ChunkWorker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// TODO: tbh this should not have a special equals/hashcode implementation
@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Hole implements CanBeInvalidated {
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final Set<BlockPos> positions = new HashSet<>();
    @ToString.Exclude
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
    private final Vec3 center;
    /**
     * Because Holes might occur multiple times in {@link HoleManager#getMap()} this is to quickly check if we have already visited a hole while iterating over the map.
     * This value is only to be accessed from Minecrafts main thread!
     */
    private int visitId;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Map<BlockPos, Hole> map;

    public Hole(ChunkWorker chunk, int x, int y, int z, int maxX, int maxZ, boolean is2x1, boolean is2x2, boolean safe) {
        // TODO: chunk.getVersion() is not exactly safe, the Task producing this should supply it!
        this(chunk, chunk.getVersion(), x, y, z, maxX, maxZ, safe, !is2x1 && !is2x2, is2x1, is2x2, new Vec3(x + (maxX - x) / 2.0, y, z + (maxZ - z) / 2.0));
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

    public double getDistanceSqr(Vec3 position) {
        return position.distanceToSqr(getX() + (getMaxX() - getX()) / 2.0, getY(), getZ() + (getMaxZ() - getZ()) / 2.0);
    }

    /**
     * @return the one, two or four blocks that form the actual hole.
     */
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
