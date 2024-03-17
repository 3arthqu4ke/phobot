package me.earth.phobot.movement;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.Vec3;

/**
 * BunnyHop movement based on NCPs MagicBunny.
 */
public class BunnyHop extends Movement {
    public static final double BUNNY_DIV_FRICTION = 159.0/*160.0?*/;
    public static final double BUNNY_FRICTION = 0.01;
    public static final double BUNNY_SLOPE = 0.66;

    @Override
    public State move(Player player, CollisionGetter level, State previous, Vec3 directionAndYMovement) {
        State state = previous.copy();
        state.setReset(false);
        if (shouldNotUseMovementHacks(player)) {
            state.reset();
            return state;
        }

        double speed = state.speed;
        double ySpeed = directionAndYMovement.y();
        if (state.stage == 1) {
            // TODO: check this, this basically never happens
            speed = 1.35 * getSpeed(player) - BUNNY_FRICTION;
        } else if (state.stage == 2 && player.verticalCollisionBelow) {
            ySpeed = getJumpY();
            speed = speed * getStage2Speed(state.boost);
            state.boost = !state.boost;
        } else if (state.stage == 3) {
            speed = state.distance - (BUNNY_SLOPE * (state.distance - getSpeed(player)));
        } else {
            if (player.verticalCollisionBelow || level.getCollisions(player, player.getBoundingBox().move(0.0, player.getDeltaMovement().y, 0.0)).iterator().hasNext()) {
                state.stage = 1; // TODO: I think 1, but +1 is going to be added later, check?
            }

            speed = state.distance - state.distance / BUNNY_DIV_FRICTION;
        }

        speed = Math.max(speed, getSpeed(player));
        state.delta = new Vec3(speed * directionAndYMovement.x(), ySpeed, speed * directionAndYMovement.z());
        state.speed = speed;
        state.stage += 1;
        return state;
    }

    public double getJumpY() {
        return 0.4;
    }

    public double getStage2Speed(boolean boost) {
        return boost ? 1.6835 : 1.395;
    }

}
