package me.earth.phobot.mixins.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface IEntity {
    @Accessor("onGroundNoBlocks")
    boolean getOnGroundNoBlocks();

    @Accessor("onGroundNoBlocks")
    void setOnGroundNoBlocks(boolean onGroundNoBlocks);

    @Accessor("stuckSpeedMultiplier")
    Vec3 getStuckSpeedMultiplier();

    @Accessor("stuckSpeedMultiplier")
    void setStuckSpeedMultiplier(Vec3 stuckSpeedMultiplier);

}
