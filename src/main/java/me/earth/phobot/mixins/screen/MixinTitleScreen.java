package me.earth.phobot.mixins.screen;

import me.earth.phobot.event.FirstTitleScreenRenderEvent;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen {
    @Unique
    private static boolean phobot$called;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ModCheck;shouldReportAsModified()Z"))
    private void renderHook(GuiGraphics guiGraphics, int x, int y, float delta, CallbackInfo ci) {
        if (!phobot$called) {
            PingBypassApi.getEventBus().post(new FirstTitleScreenRenderEvent());
            phobot$called = false;
        }
    }

}
