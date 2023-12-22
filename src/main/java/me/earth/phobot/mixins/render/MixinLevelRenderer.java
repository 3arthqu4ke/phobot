package me.earth.phobot.mixins.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.earth.phobot.Phobot;
import me.earth.phobot.event.RenderEvent;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void renderLevelHook(PoseStack poseStack, float tickDelta, long limitTime, boolean bl, Camera camera,
                                 GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f,
                                 CallbackInfo ci) {
        minecraft.getProfiler().push(Phobot.NAME);

        RenderEvent event = RenderEvent.getInstance();
        event.setTickDelta(tickDelta);
        event.setLimitTime(limitTime);
        event.setPoseStack(poseStack);
        event.setCamera(camera);

        PingBypassApi.getEventBus().post(event);
        minecraft.getProfiler().pop();
    }

}
