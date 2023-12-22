package me.earth.phobot.util.player;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import lombok.Setter;
import me.earth.phobot.movement.Movement;
import me.earth.phobot.movement.NoStepMovement;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.function.Function;

@Setter
@Getter
@SuppressWarnings("resource") // this.level()
public class MovementPlayer extends FakePlayer {
    private Function<Vec3, Vec3> moveCallback = Function.identity();
    private Movement movement = NoStepMovement.INSTANCE;

    public MovementPlayer(ClientLevel level) {
        super(level);
    }

    public MovementPlayer(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Override
    public float maxUpStep() {
        float maxUpStep = super.maxUpStep();
        if (movement.canStep(this)) {
            return Math.max(maxUpStep, movement.getStepHeight());
        }

        return maxUpStep;
    }

    @Override
    public void copyPosition(Entity entity) {
        super.copyPosition(entity);
        this.setOnGround(entity.onGround());
        this.verticalCollision = entity.verticalCollision;
        this.verticalCollisionBelow = entity.verticalCollisionBelow;
        this.horizontalCollision = entity.horizontalCollision;
    }

    @Override
    public void move(MoverType moverType, Vec3 motion) {
        motion = moveCallback.apply(motion);
        if (this.stuckSpeedMultiplier.lengthSqr() > Shapes.EPSILON) {
            motion = motion.multiply(this.stuckSpeedMultiplier);
            this.stuckSpeedMultiplier = Vec3.ZERO;
            this.setDeltaMovement(Vec3.ZERO);
        }

        Vec3 collideVec;
        double collideDistance;
        if ((collideDistance = (collideVec = this.collide(motion = this.maybeBackOffFromEdge(motion, moverType))).lengthSqr()) > Shapes.EPSILON) {
            if (this.fallDistance != 0.0f
                    && collideDistance >= 1.0
                    && this.level()
                        .clip(new ClipContext(this.position(), this.position().add(collideVec), ClipContext.Block.FALLDAMAGE_RESETTING, ClipContext.Fluid.WATER, this))
                        .getType() != HitResult.Type.MISS) {
                this.resetFallDistance();
            }

            this.setPos(this.getX() + collideVec.x, this.getY() + collideVec.y, this.getZ() + collideVec.z);
        }

        boolean xDiff = !Mth.equal(motion.x, collideVec.x);
        boolean zDiff = !Mth.equal(motion.z, collideVec.z);
        this.horizontalCollision = xDiff || zDiff;
        this.verticalCollision = motion.y != collideVec.y;
        this.verticalCollisionBelow = this.verticalCollision && motion.y < 0.0;
        this.minorHorizontalCollision = this.horizontalCollision && this.isHorizontalCollisionMinor(collideVec);
        this.setOnGroundWithKnownMovement(this.verticalCollisionBelow, collideVec);

        @SuppressWarnings("deprecation") BlockPos blockPos = this.getOnPosLegacy();
        BlockState onPosState = this.level().getBlockState(blockPos);
        updateFallDistance(collideVec.y, this.onGround());
        if (this.horizontalCollision) {
            Vec3 deltaMovement = this.getDeltaMovement();
            this.setDeltaMovement(xDiff ? 0.0 : deltaMovement.x, deltaMovement.y, zDiff ? 0.0 : deltaMovement.z);
        }

        Block block = onPosState.getBlock();
        if (motion.y != collideVec.y) {
            block.updateEntityAfterFallOn(this.level(), this);
        }

        this.tryCheckInsideBlocks();
        float blockSpeedFactor = this.getBlockSpeedFactor();
        this.setDeltaMovement(this.getDeltaMovement().multiply(blockSpeedFactor, 1.0, blockSpeedFactor));
    }

    @Override
    public Vec3 maybeBackOffFromEdge(Vec3 vec3, MoverType moverType) {
        // make public
        return super.maybeBackOffFromEdge(vec3, moverType);
    }

    // TODO: swimming, elytra flying, basically LivingEntity.travel(Vec3)
    /**
     * @see net.minecraft.world.entity.LivingEntity#travel(Vec3)
     */
    @SuppressWarnings("deprecation")
    public void travel() {
        Vec3 delta = this.getDeltaMovement();
        double gravity = movement.getGravity(); // 0.08;
        boolean falling = this.getDeltaMovement().y <= 0.0;
        if (falling && this.hasEffect(MobEffects.SLOW_FALLING)) {
            gravity = 0.01;
        }

        BlockPos pos = this.getBlockPosBelowThatAffectsMyMovement();
        float friction = this.level().getBlockState(pos).getBlock().getFriction();
        float nextFriction = this.onGround() ? friction * 0.91f : 0.91f;
        Vec3 afterMove = this.handleRelativeFrictionAndCalculateMovement(delta, friction);
        double frictionY = afterMove.y;
        MobEffectInstance levitation = this.getEffect(MobEffects.LEVITATION);
        if (levitation != null && this.hasEffect(MobEffects.LEVITATION)) {
            frictionY += (0.05 * (double) (levitation.getAmplifier() + 1) - afterMove.y) * 0.2;
        } else if (this.level().hasChunkAt(pos)) {
            if (!this.isNoGravity()) {
                frictionY -= gravity;
            }
        } else {
            frictionY = this.getY() > (double)this.level().getMinBuildHeight() ? -0.1 : 0.0;
        }

        // TODO: currently we do not take this into account for Speed/MovementPathfinding, because we just use the delta from the previous Movement.State. Does this matter?
        if (this.shouldDiscardFriction()) {
            this.setDeltaMovement(afterMove.x, frictionY, afterMove.z);
        } else {
            this.setDeltaMovement(afterMove.x * (double) nextFriction, frictionY * (double) 0.98f, afterMove.z * (double) nextFriction);
        }
    }

    public Vec3 collide(Vec3 motion) {
        AABB bb = this.getBoundingBox();
        List<VoxelShape> list = this.level().getEntityCollisions(this, bb.expandTowards(motion));
        Vec3 result = motion.lengthSqr() == 0.0 ? motion : Entity.collideBoundingBox(this, motion, bb, this.level(), list);
        boolean xDiff = motion.x != result.x;
        boolean yDiff = motion.y != result.y;
        boolean zDiff = motion.z != result.z;
        boolean onGround = this.onGround() || yDiff && motion.y < 0.0;
        if (this.maxUpStep() > 0.0f && onGround && (xDiff || zDiff)) {
            Vec3 afterEntityCollisions = Entity.collideBoundingBox(this, new Vec3(motion.x, this.maxUpStep(), motion.z), bb, this.level(), list);
            Vec3 stepCollisions = Entity.collideBoundingBox(this, new Vec3(0.0, this.maxUpStep(), 0.0), bb.expandTowards(motion.x, 0.0, motion.z), this.level(), list);
            if (stepCollisions.y < this.maxUpStep()) {
                Vec3 afterMotion = Entity.collideBoundingBox(this, new Vec3(motion.x, 0.0, motion.z), bb.move(stepCollisions), this.level(), list).add(stepCollisions);
                if (afterMotion.horizontalDistanceSqr() > afterEntityCollisions.horizontalDistanceSqr()) {
                    afterEntityCollisions = afterMotion;
                }
            }

            if (afterEntityCollisions.horizontalDistanceSqr() > result.horizontalDistanceSqr()) {
                return afterEntityCollisions.add(Entity.collideBoundingBox(this, new Vec3(0.0, -afterEntityCollisions.y + motion.y, 0.0), bb.move(afterEntityCollisions), this.level(), list));
            }
        }

        return result;
    }

    private void updateFallDistance(double fallDistanceThisTick, boolean onGround) {
        if (onGround) {
            this.resetFallDistance();
        } else if (fallDistanceThisTick < 0.0) {
            this.fallDistance -= (float) fallDistanceThisTick;
        }
    }

}
