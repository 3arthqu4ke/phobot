package me.earth.phobot.movement;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.Vec3;

public class Movement {
    public static final double BASE_SPEED = 0.287/*3?*/;
    public static final float STEP_HEIGHT = 2.0f;
    public static final double GRAVITY = 0.08;
    public static final double DRAG = 0.98;

    public double getBaseSpeed() {
        return BASE_SPEED;
    }

    public double getGravity() {
        return GRAVITY;
    }

    public double getDrag() {
        return DRAG;
    }

    public float getStepHeight() {
        return STEP_HEIGHT;
    }

    public boolean canStep(LivingEntity entity) {
        return entity.onGround()
                && entity.verticalCollision
                && entity.fallDistance < 0.1
                && !entity.isInWater()
                && !entity.onClimbable()
                && !entity.isInLava();
    }

    public double getSpeed(Player player) {
        double speed = getBaseSpeed();
        var speedEffect = player.getEffect(MobEffects.MOVEMENT_SPEED);
        if (speedEffect != null) {
            speed *= 1.0 + 0.2 * (speedEffect.getAmplifier() + 1);
        }

        var slowEffect = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slowEffect != null) {
            speed /= 1.0 + 0.2 * (slowEffect.getAmplifier() + 1);
        }

        return speed;
    }

    public State move(Player player, CollisionGetter level, State previous, Vec3 directionAndYMovement) {
        return strafe(player, level, previous, directionAndYMovement);
    }

    public State strafe(Player player, @SuppressWarnings("unused")/* for impls */ CollisionGetter level, State previous, Vec3 directionAndYMovement) {
        State state = previous.copy();
        state.setReset(false);
        if (shouldNotUseMovementHacks(player)) {
            state.reset();
            return state;
        }

        state.speed = getSpeed(player);
        state.delta = new Vec3(state.speed * directionAndYMovement.x(), directionAndYMovement.y(), state.speed * directionAndYMovement.z());
        state.stage = 0;
        return state;
    }

    public boolean shouldNotUseMovementHacks(Player player) {
        // TODO: maybe this fallDistance is not enough? got lags when strafing above it
        return player.fallDistance > 5.0 || player.isInWater() || player.isInPowderSnow || player.isInLava() || player.onClimbable() || player.isFallFlying();
    }

    public double getDeltaYOnGround() {
        return -GRAVITY * DRAG;
    }

    @Data
    @AllArgsConstructor
    public static class State {
        protected double speed;
        protected double distance;
        protected Vec3 delta;
        protected int stage;
        protected boolean boost;
        protected boolean reset;

        public State() {
            this(0.0, 0.0, Vec3.ZERO, 0, false, false);
        }

        public void reset() {
            this.speed = 0.0;
            this.stage = 0;
            this.distance = 0.0;
            this.delta = Vec3.ZERO;
            this.boost = false;
            this.reset = true;
        }

        public State copy() {
            return new State(speed, distance, delta, stage, boost, reset);
        }
    }

}
