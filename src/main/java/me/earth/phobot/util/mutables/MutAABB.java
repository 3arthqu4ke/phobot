package me.earth.phobot.util.mutables;

import lombok.Data;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

@Data
public class MutAABB {
    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;

    public void set(AABB bb) {
        set(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    }

    public void set(MutAABB bb) {
        set(bb.getMinX(), bb.getMinY(), bb.getMinZ(), bb.getMaxX(), bb.getMaxY(), bb.getMaxZ());
    }

    public void set(BlockPos pos) {
        set(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    public void set(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        setMinX(Math.min(minX, maxX));
        setMinY(Math.min(minY, maxY));
        setMinZ(Math.min(minZ, maxZ));
        setMaxX(Math.max(minX, maxX));
        setMaxY(Math.max(minY, maxY));
        setMaxZ(Math.max(minZ, maxZ));
    }

    public void set(int x, int y, int z) {
        this.set(x, y, z, x + 1, y + 1, z + 1);
    }

    // TODO: use getters and setters instead
    public void move(double x, double y, double z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
    }

    public void move(BlockPos pos) {
        set(this.minX + pos.getX(), this.minY + pos.getY(), this.minZ + pos.getZ(), this.maxX + pos.getX(), this.maxY + pos.getY(), this.maxZ + pos.getZ());
    }

    public void grow(double by) {
        this.grow(by, by, by);
    }

    // TODO: use getters and setters instead
    public void grow(double x, double y, double z) {
        this.minX = this.minX - x;
        this.minY = this.minY - y;
        this.minZ = this.minZ - z;
        this.maxX = this.maxX + x;
        this.maxY = this.maxY + y;
        this.maxZ = this.maxZ + z;
    }

    public AABB toImmutable() {
        return new AABB(getMinX(), getMinY(), getMinZ(), getMaxX(), getMaxY(), getMaxZ());
    }

    @Nullable
    public BlockHitResult singleClip(Vec3 from, Vec3 to, BlockPos pos) {
        double[] factorHolder = new double[]{1.0};
        double x = to.x - from.x;
        double y = to.y - from.y;
        double z = to.z - from.z;
        this.move(pos);
        Direction direction = getDirection(this, from, factorHolder, null, x, y, z);
        if (direction == null) {
            return null;
        }

        double factor = factorHolder[0];
        return new BlockHitResult(from.add(factor * x, factor * y, factor * z), direction, pos, false);
    }

    @Nullable
    private static Direction getDirection(MutAABB bb, Vec3 start, double[] minDistance, @Nullable Direction facing, double deltaX, double deltaY, double deltaZ) {
        if (deltaX > Shapes.EPSILON) {
            facing = clipPoint(
                    minDistance, facing, deltaX, deltaY, deltaZ, bb.minX, bb.minY, bb.maxY, bb.minZ, bb.maxZ, Direction.WEST, start.x, start.y, start.z
            );
        } else if (deltaX < -Shapes.EPSILON) {
            facing = clipPoint(
                    minDistance, facing, deltaX, deltaY, deltaZ, bb.maxX, bb.minY, bb.maxY, bb.minZ, bb.maxZ, Direction.EAST, start.x, start.y, start.z
            );
        }

        if (deltaY > Shapes.EPSILON) {
            facing = clipPoint(
                    minDistance, facing, deltaY, deltaZ, deltaX, bb.minY, bb.minZ, bb.maxZ, bb.minX, bb.maxX, Direction.DOWN, start.y, start.z, start.x
            );
        } else if (deltaY < -Shapes.EPSILON) {
            facing = clipPoint(
                    minDistance, facing, deltaY, deltaZ, deltaX, bb.maxY, bb.minZ, bb.maxZ, bb.minX, bb.maxX, Direction.UP, start.y, start.z, start.x
            );
        }

        if (deltaZ > Shapes.EPSILON) {
            facing = clipPoint(
                    minDistance, facing, deltaZ, deltaX, deltaY, bb.minZ, bb.minX, bb.maxX, bb.minY, bb.maxY, Direction.NORTH, start.z, start.x, start.y
            );
        } else if (deltaZ < -Shapes.EPSILON) {
            facing = clipPoint(
                    minDistance, facing, deltaZ, deltaX, deltaY, bb.maxZ, bb.minX, bb.maxX, bb.minY, bb.maxY, Direction.SOUTH, start.z, start.x, start.y
            );
        }

        return facing;
    }

    @Nullable
    private static Direction clipPoint(
            double[] minDistance,
            @Nullable Direction prevDirection,
            double distanceSide,
            double distanceOtherA,
            double distanceOtherB,
            double minSide,
            double minOtherA,
            double maxOtherA,
            double minOtherB,
            double maxOtherB,
            Direction hitSide,
            double startSide,
            double startOtherA,
            double startOtherB
    ) {
        double half = (minSide - startSide) / distanceSide;
        double otherA = startOtherA + half * distanceOtherA;
        double otherB = startOtherB + half * distanceOtherB;
        if (0.0 < half && half < minDistance[0] && minOtherA - Shapes.EPSILON < otherA && otherA < maxOtherA + Shapes.EPSILON && minOtherB - Shapes.EPSILON < otherB && otherB < maxOtherB + Shapes.EPSILON) {
            minDistance[0] = half;
            return hitSide;
        } else {
            return prevDirection;
        }
    }

}
