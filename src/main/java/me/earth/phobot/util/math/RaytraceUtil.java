package me.earth.phobot.util.math;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@UtilityClass
public class RaytraceUtil {
    public static @Nullable EntityHitResult raytraceEntity(Entity entity, Entity target) {
        return raytraceEntities(entity, entity.distanceTo(target) + 4.0, e -> e.equals(target));
    }

    public static @Nullable EntityHitResult raytraceEntities(Entity entity, double distance, Predicate<Entity> entityCheck) {
        Vec3 eyePos = entity.getEyePosition();
        Vec3 viewVec = getViewVec(entity);
        Vec3 goal = eyePos.add(viewVec.x * distance, viewVec.y * distance, viewVec.z * distance);
        AABB bb = entity.getBoundingBox().expandTowards(viewVec.scale(distance)).inflate(1.0, 1.0, 1.0);
        return ProjectileUtil.getEntityHitResult(entity, eyePos, goal, bb, entityCheck, distance * distance);
    }

    public static @Nullable BlockHitResult raytrace(Entity entity, Level level, BlockPos target, @Nullable Direction direction, boolean invert) {
        return raytraceChecked(entity, level, target, direction == null ? null : hitResult -> hitResult.getDirection().equals(direction), invert);
    }

    public static @Nullable BlockHitResult raytraceChecked(Entity entity, Level level, BlockPos target, @Nullable Predicate<BlockHitResult> direction, boolean invert) {
        double distance = Math.sqrt(entity.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5 - entity.getEyeHeight(), target.getZ() + 0.5)) + 3.0;
        BlockHitResult result = raytrace(entity, level, distance, hitResult -> {
            if (hitResult != null && hitResult.getBlockPos().equals(target) && (direction == null || direction.test(hitResult))) {
                return hitResult;
            }

            return null;
        });
        // its possible that we hit the block, just with the wrong direction and our ray exits on the other side at the right direction, so we do another raytrace and invert
        if (invert && result == null && direction != null) {
            result = invertedRaytrace(entity, level, distance, hitResult -> {
                if (hitResult != null && hitResult.getBlockPos().equals(target) && direction.test(hitResult)) {
                    return hitResult;
                }

                return null;
            });
        }

        return result;
    }

    public static @Nullable BlockHitResult raytraceToPlaceTarget(Entity entity, Level level, BlockPos target, BiPredicate<BlockPos, Direction> directionCheck, boolean invert) {
        double distance = Math.sqrt(entity.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5 - entity.getEyeHeight(), target.getZ() + 0.5)) + 3.0;
        Function<@Nullable BlockHitResult, @Nullable BlockHitResult> function = hitResult -> {
            if (hitResult != null && hitResult.getBlockPos().relative(hitResult.getDirection()).equals(target) && directionCheck.test(hitResult.getBlockPos(), hitResult.getDirection())) {
                return hitResult;
            }

            return null;
        };

        BlockHitResult result = raytrace(entity, level, distance, function);
        // its possible that we hit the block, just with the wrong direction and our ray exits on the other side at the right direction, so we do another raytrace and invert
        if (invert && result == null) {
            result = invertedRaytrace(entity, level, distance, function);
        }

        return result;
    }

    public static @Nullable BlockHitResult raytrace(Entity entity, Level level, double distance, Function<@Nullable BlockHitResult, @Nullable BlockHitResult> action) {
        Vec3 eyePos = entity.getEyePosition();
        Vec3 viewVec = getViewVec(entity);
        Vec3 goal = eyePos.add(viewVec.x * distance, viewVec.y * distance, viewVec.z * distance);
        return raytrace(eyePos, goal, entity, level, action);
    }

    public static @Nullable BlockHitResult invertedRaytrace(Entity entity, Level level, double distance, Function<@Nullable BlockHitResult, @Nullable BlockHitResult> action) {
        Vec3 eyePos = entity.getEyePosition();
        Vec3 viewVec = getViewVec(entity);
        Vec3 goal = eyePos.add(viewVec.x * distance, viewVec.y * distance, viewVec.z * distance);
        return raytrace(goal, eyePos, entity, level, action);
    }

    public static Vec3 getViewVec(Entity entity) {
        // somehow entity.getViewVector(1.0f) does not work?
        float xRot = entity.getXRot() * ((float) Math.PI / 180);
        float yRot = -entity.getYRot() * ((float) Math.PI / 180);
        float yCos = Mth.cos(yRot);
        float ySin = Mth.sin(yRot);
        float xCos = Mth.cos(xRot);
        float xSin = Mth.sin(xRot);
        return new Vec3(ySin * xCos, -xSin, yCos * xCos);
    }

    @SuppressWarnings("DataFlowIssue") // ofc it can return null?!
    public static @Nullable BlockHitResult raytrace(Vec3 start, Vec3 goal, Entity entity, Level level, Function<@Nullable BlockHitResult, @Nullable BlockHitResult> action) {
        ClipContext ctx = new ClipContext(start, goal, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity);
        return BlockGetter.traverseBlocks(ctx.getFrom(), ctx.getTo(), ctx, (clipContext, pos) -> {
            BlockState blockState = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);
            Vec3 from = clipContext.getFrom();
            Vec3 to = clipContext.getTo();
            VoxelShape blockShape = clipContext.getBlockShape(blockState, level, pos);
            BlockHitResult interactionHitResult = level.clipWithInteractionOverride(from, to, pos, blockShape, blockState);
            VoxelShape shape = clipContext.getFluidShape(fluidState, level, pos);
            BlockHitResult fluidHitResult = shape.clip(from, to, pos);
            double interactionDistance = interactionHitResult == null ? Double.MAX_VALUE : clipContext.getFrom().distanceToSqr(interactionHitResult.getLocation());
            double fluidDistance = fluidHitResult == null ? Double.MAX_VALUE : clipContext.getFrom().distanceToSqr(fluidHitResult.getLocation());
            return action.apply(interactionDistance <= fluidDistance ? interactionHitResult : fluidHitResult);
        }, clipContext -> null);
    }

}
