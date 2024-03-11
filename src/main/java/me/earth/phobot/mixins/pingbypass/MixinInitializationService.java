package me.earth.phobot.mixins.pingbypass;

import com.google.gson.JsonParseException;
import me.earth.phobot.PhobotApi;
import me.earth.phobot.PhobotPlugin;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.config.JsonSerializable;
import me.earth.pingbypass.api.launch.InitializationService;
import me.earth.pingbypass.api.launch.PreLaunchService;
import me.earth.pingbypass.api.platform.PlatformProvider;
import me.earth.pingbypass.api.plugin.PluginConfig;
import me.earth.pingbypass.api.plugin.PluginContainer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

// TODO: PingBypass PluginInitializer make this a method
/**
 * Allows us to load Phobot from the mods folder.
 */
@Pseudo
@Mixin(value = InitializationService.class, remap = false)
public class MixinInitializationService {
    @Shadow @Final private static Logger log;
    @Shadow @Final private PingBypass pingBypass;

    @Shadow @Final private PreLaunchService preLaunchService;

    @Inject(method = "initializePlugins", at = @At("RETURN"))
    private void initializePluginsHook(CallbackInfo ci) {
        if (PhobotApi.getPhobot() == null) {
            try (var is = InitializationService.class.getClassLoader().getResourceAsStream("PhobotPluginConfig.json")) {
                assert is != null;
                var isr = new InputStreamReader(is);
                PluginConfig pluginConfig = JsonSerializable.GSON.fromJson(isr, PluginConfig.class);
                if (!Arrays.asList(pluginConfig.getSupports()).contains(pingBypass.getSide())
                        || !Arrays.asList(pluginConfig.getPlatforms()).contains(PlatformProvider.getInstance().getCurrent())) {
                    return;
                }

                log.info("Phobot not initialized via InitializationService, adding it manually.");
                PhobotPlugin plugin = new PhobotPlugin();
                PluginContainer pluginContainer = new PluginContainer(plugin, null, pluginConfig);
                pingBypass.getPluginManager().register(pluginContainer);
                plugin.load(pingBypass);
            } catch (IOException | JsonParseException e) {
                log.error("Failed to initialize Phobot", e);
            }
        }
    }

}
