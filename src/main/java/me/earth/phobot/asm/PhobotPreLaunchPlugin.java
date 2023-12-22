package me.earth.phobot.asm;

import lombok.extern.slf4j.Slf4j;
import me.earth.pingbypass.api.plugin.PluginMixinConnector;
import me.earth.pingbypass.api.side.Side;
import me.earth.pingbypass.commons.launch.PreLaunchServiceImpl;

@Slf4j
@SuppressWarnings("unused")
public class PhobotPreLaunchPlugin implements PluginMixinConnector {
    @Override
    public void connect(Side side) {
        log.info("Loading PhobotPreLaunchPlugin");
        PreLaunchServiceImpl.INSTANCE.getTransformerRegistry().register(new MultiPlayerGameModeTransformer());
        PreLaunchServiceImpl.INSTANCE.getTransformerRegistry().register(new InventoryTransformer());
        PreLaunchServiceImpl.INSTANCE.getTransformerRegistry().register(new FutureLevelPatch());
    }

}
