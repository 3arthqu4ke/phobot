package me.earth.phobot;

import me.earth.phobot.services.BlockPlacer;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.TestPingBypass;
import me.earth.pingbypass.api.module.CommonModuleInitializer;
import me.earth.pingbypass.api.plugin.impl.PluginUnloadingService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestPhobot {
    public static Phobot createNewTestPhobot() {
        TestUtil.bootstrap();
        PingBypass pingBypass = new TestPingBypass();
        new CommonModuleInitializer() {}.registerCommonModules(pingBypass);
        PhobotPlugin plugin = new PhobotPlugin();
        plugin.load(pingBypass, new PluginUnloadingService(pingBypass));
        Phobot phobot = plugin.getPhobot();
        assertNotNull(phobot);
        return phobot;
    }

    public static BlockPlacer getBlockPlacer() {
        Phobot phobot = createNewTestPhobot();
        return new BlockPlacer(phobot.getLocalPlayerPositionService(), phobot.getMotionUpdateService(), phobot.getInventoryService(),
                                phobot.getMinecraft(), phobot.getAntiCheat(), phobot.getAttackService());
    }

}
