package me.earth.phobot.movement;

import net.minecraft.world.entity.LivingEntity;

public class NoStepMovement extends Movement {
    public static final NoStepMovement INSTANCE = new NoStepMovement();

    @Override
    public boolean canStep(LivingEntity entity) {
        return false;
    }

    @Override
    public float getStepHeight() {
        return 0.6f;
    }

}
