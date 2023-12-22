package me.earth.phobot.mixins.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.earth.phobot.event.LocalPlayerRenderEvent;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer<T extends LivingEntity> {
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    public void onRenderPre(T e, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (Minecraft.getInstance().player == e) {
            PingBypassApi.getEventBus().post(new LocalPlayerRenderEvent(true, e.getYRot(), e.getXRot(), e.yRotO, e.xRotO, e.yHeadRot, e.yBodyRot, e.yHeadRotO, e.yBodyRotO));
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("TAIL"))
    public void onRenderPost(T e, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (Minecraft.getInstance().player == e) {
            PingBypassApi.getEventBus().post(LocalPlayerRenderEvent.POST);
        }
    }

}
