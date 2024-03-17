package me.earth.phobot.pathfinder.movement;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.earth.phobot.mixins.entity.IEntity;
import me.earth.phobot.util.reflection.AccessorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * A snapshot of all movement related properties of an {@link net.minecraft.world.entity.Entity},
 * e.g. after {@link net.minecraft.world.entity.LivingEntity#travel(Vec3)} has been called.
 */
@Data
@RequiredArgsConstructor
@Builder(toBuilder = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class EntityMovementSnapshot {
    // TODO: feetblockstate, gets set to null in Entity.baseTick
    /**
     * @see Entity#position()
     */
    private final Vec3 position;
    /**
     * @see Entity#horizontalCollision
     */
    private final boolean horizontalCollision;
    /**
     * @see Entity#minorHorizontalCollision
     */
    private final boolean minorHorizontalCollision;
    /**
     * @see Entity#verticalCollisionBelow
     */
    private final boolean verticalCollisionBelow;
    /**
     * @see Entity#verticalCollision
     */
    private final boolean verticalCollision;
    /**
     * @see Entity#onGround()
     */
    private final boolean onGround;
    /**
     * @see Entity#fallDistance
     */
    private final float fallDistance;
    /**
     * The {@link Player#getDeltaMovement()} after {@link Player#travel(Vec3)}
     */
    private final Vec3 postTravelDelta;
    /**
     * @see Entity#mainSupportingBlockPos
     */
    private final Optional<BlockPos> mainSupportingBlockPos;
    /**
     * @see IEntity#getOnGroundNoBlocks()
     */
    private final boolean onGroundNoBlocks;
    /**
     * @see IEntity#getStuckSpeedMultiplier()
     */
    private final Vec3 stuckSpeedMultiplier;

    public EntityMovementSnapshot(Entity entity) {
        this(entity.position(), entity.horizontalCollision, entity.minorHorizontalCollision, entity.verticalCollisionBelow,
                entity.verticalCollision, entity.onGround(), entity.fallDistance, entity.getDeltaMovement(), entity.mainSupportingBlockPos,
                AccessorUtil.getAsIEntity(entity).getOnGroundNoBlocks(), AccessorUtil.getAsIEntity(entity).getStuckSpeedMultiplier());
    }

    public void apply(Entity entity) {
        // TODO: find all fields that influence entity movement, we could have made oversights
        entity.setPos(position);
        entity.horizontalCollision = horizontalCollision;
        entity.minorHorizontalCollision = minorHorizontalCollision;
        entity.verticalCollision = verticalCollision;
        entity.verticalCollisionBelow = verticalCollisionBelow;
        entity.setOnGround(onGround);
        entity.fallDistance = fallDistance;
        entity.setDeltaMovement(postTravelDelta);
        entity.mainSupportingBlockPos = mainSupportingBlockPos;
        AccessorUtil.getAsIEntity(entity).setOnGroundNoBlocks(onGroundNoBlocks);
        AccessorUtil.getAsIEntity(entity).setStuckSpeedMultiplier(stuckSpeedMultiplier);
    }

    public static EntityMovementSnapshot dummy(Vec3 position, Vec3 gravity) {
        return new EntityMovementSnapshot(position, false, false, false, false, false, 0.0f, gravity, Optional.empty(), false, Vec3.ZERO);
    }

}
