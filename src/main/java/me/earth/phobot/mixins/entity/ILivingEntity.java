package me.earth.phobot.mixins.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface ILivingEntity {
    @Accessor("DATA_HEALTH_ID")
    static EntityDataAccessor<Float> getDataHealthId() {
        throw new IllegalStateException("DATA_HEALTH_ID accessor has not been mixed in!");
    }

    @Accessor("lerpX")
    double getLerpX();

    @Accessor("lerpY")
    double getLerpY();

    @Accessor("lerpZ")
    double getLerpZ();

}
