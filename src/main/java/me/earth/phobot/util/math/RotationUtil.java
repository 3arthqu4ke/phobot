package me.earth.phobot.util.math;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class RotationUtil {
    /**
     * Pitch that looks down.
     *
     * @see Entity#setXRot(float)
     */
    public static final float X_ROT_DOWN = 90.0f;

    public static float[] getRotations(Entity entity, Level level, BlockPos pos, @Nullable Direction direction) {
        Vec3 hitVec = getHitVec(pos, level, direction);
        return getRotations(entity, hitVec.x, hitVec.y, hitVec.z);
    }

    public static Vec3 getHitVec(BlockPos pos, Level level, @Nullable Direction direction) {
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getShape(level, pos);
        AABB bb = shape.isEmpty() ? Shapes.block().bounds() : shape.bounds(); // TODO: make this better?
        double x = pos.getX() + (direction == Direction.EAST  ? bb.maxX : direction == Direction.WEST  ? bb.minX : (bb.minX + bb.maxX) / 2.0);
        double y = pos.getY() + (direction == Direction.UP    ? bb.maxY : direction == Direction.DOWN  ? bb.minY : (bb.minY + bb.maxY) / 2.0);
        double z = pos.getZ() + (direction == Direction.SOUTH ? bb.maxZ : direction == Direction.NORTH ? bb.minZ : (bb.minZ + bb.maxZ) / 2.0);
        return new Vec3(x, y, z);
    }

    public static float[] getRotations(Entity entity, double toX, double toY, double toZ) {
        return getRotations(entity.getX(), entity.getEyeY(), entity.getZ(), toX, toY, toZ, entity.yRotO);
    }

    public static float[] getLerpRotations(Minecraft mc, Entity entity, double toX, double toY, double toZ) {
        double x = Mth.lerp(mc.getFrameTime(), entity.xo, entity.getX());
        double y = Mth.lerp(mc.getFrameTime(), entity.yo, entity.getY()) + entity.getEyeHeight();
        double z = Mth.lerp(mc.getFrameTime(), entity.zo, entity.getZ());
        return getRotations(x, y, z, toX, toY, toZ, entity.yRotO);
    }

    public static float[] getRotations(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, float prevYRot) {
        double xDiff = toX - fromX;
        double yDiff = toY - fromY;
        double zDiff = toZ - fromZ;

        double dist = Math.sqrt(xDiff * xDiff + zDiff * zDiff);

        float yaw = (float) (Math.atan2(zDiff, xDiff) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-(Math.atan2(yDiff, dist) * 180.0 / Math.PI));
        float diff = yaw - prevYRot;

        if (diff < -180.0f || diff > 180.0f) {
            float round = Math.round(Math.abs(diff / 360.0f));
            diff = diff < 0.0f ? diff + 360.0f * round : diff - (360.0f * round);
        }

        return new float[] { prevYRot + diff, pitch };
    }

}
