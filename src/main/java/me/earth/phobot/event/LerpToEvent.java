package me.earth.phobot.event;

import lombok.Data;
import net.minecraft.world.entity.LivingEntity;

@Data
public final class LerpToEvent {
    private final LivingEntity entity;
    private final double x;
    private final double y;
    private final double z;
    private final float yRot;
    private final float xRot;
    private final long lastLerp;

}
