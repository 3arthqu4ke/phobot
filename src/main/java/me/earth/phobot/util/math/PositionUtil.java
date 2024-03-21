package me.earth.phobot.util.math;

import lombok.experimental.UtilityClass;
import me.earth.phobot.pathfinder.algorithm.Abstract3iNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class PositionUtil {
    public static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[] { Direction.NORTH, Direction.WEST, Direction.EAST, Direction.SOUTH };
    public static final Direction[] DIRECTIONS = Direction.values();

    public static Set<BlockPos> getPositionsUnderEntity(Entity entity, double yOffset) {
        double y = entity.getBoundingBox().minY - 1;
        if (y > Math.floor(y) + yOffset) {
            y++;
        }

        return getPositionsBlockedByEntityAtY(entity, y);
    }

    public static Set<BlockPos> getPositionsBlockedByEntityAtY(Entity entity, double y) {
        Set<BlockPos> positions = new HashSet<>();
        getPositionsBlockedByEntityAtY(positions, entity, y);
        return positions;
    }

    public static void getPositionsBlockedByEntityAtY(Set<BlockPos> positions, Entity entity, double y) {
        AABB bb = entity.getBoundingBox();
        positions.add(BlockPos.containing(bb.minX, y, bb.minZ));
        positions.add(BlockPos.containing(bb.maxX, y, bb.minZ));
        positions.add(BlockPos.containing(bb.minX, y, bb.maxZ));
        positions.add(BlockPos.containing(bb.maxX, y, bb.maxZ));
    }

    public static AABB getAABBOfRadius(Entity entity, double radius) {
        return new AABB(Mth.floor(entity.getX() - radius), Mth.floor(entity.getY() - radius), Mth.floor(entity.getZ() - radius),
                        Mth.floor(entity.getX() + radius), Mth.floor(entity.getY() + radius), Mth.floor(entity.getZ() + radius));
    }

    public static Vec3 getBBCoords(AABB bb, Vec3 from) {
        double x = bb.minX - from.x > from.x - bb.maxX ? bb.minX : bb.maxX;
        double y = bb.minY - from.y > from.y - bb.maxY ? bb.minY : bb.maxY;
        double z = bb.minZ - from.z > from.z - bb.maxZ ? bb.minZ : bb.maxZ;
        return new Vec3(x, y, z);
    }

    public static String toSimpleString(Vec3i vec3i) {
        return vec3i.getX() + ", " + vec3i.getY() + ", " + vec3i.getZ();
    }

    public static String toSimpleString(Position position) {
        return position.x() + ", " + position.y() + ", " + position.z();
    }

    public static String toSimpleString(Abstract3iNode<?> position) {
        return position.getX() + ", " + position.getY() + ", " + position.getZ();
    }

    public static double getMaxYAtPosition(BlockPos.MutableBlockPos pos, Level level) {
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        pos.setY(pos.getY() - 1);
        VoxelShape underneath = level.getBlockState(pos).getCollisionShape(level, pos);
        pos.setY(pos.getY() + 1);
        double fenceY = pos.getY() - 1 + underneath.max(Direction.Axis.Y);
        double carpetY = pos.getY() + shape.max(Direction.Axis.Y);
        return Math.max(pos.getY(), Math.max(fenceY, carpetY));
    }

}
