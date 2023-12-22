package me.earth.phobot.mixins.entity;

import me.earth.phobot.ducks.IEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEntity {
    @Unique
    private volatile long phobot$attackTime;

    @Override
    public long phobot$getAttackTime() {
        return phobot$attackTime;
    }

    @Override
    public void phobot$setAttackTime(long time) {
        this.phobot$attackTime = time;
    }

}
