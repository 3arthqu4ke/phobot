package me.earth.phobot.mixins.render;

import me.earth.phobot.ducks.IEntityRenderDispatcher;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher implements IEntityRenderDispatcher {
    @Shadow public Camera camera;

    @Override
    public double phobot$renderPosX() {
        return this.camera.getPosition().x;
    }

    @Override
    public double phobot$renderPosY() {
        return this.camera.getPosition().y;
    }

    @Override
    public double phobot$renderPosZ() {
        return this.camera.getPosition().z;
    }

}
