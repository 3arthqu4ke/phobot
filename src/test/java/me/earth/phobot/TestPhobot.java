package me.earth.phobot;

import me.earth.phobot.services.BlockPlacer;
import me.earth.pingbypass.AbstractPingBypass;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.PingBypassApi;
import me.earth.pingbypass.api.command.Chat;
import me.earth.pingbypass.api.command.impl.CommandManagerImpl;
import me.earth.pingbypass.api.config.impl.ConfigManagerImpl;
import me.earth.pingbypass.api.files.FileManagerImpl;
import me.earth.pingbypass.api.input.DummyKeyboardAndMouse;
import me.earth.pingbypass.api.module.CommonModuleInitializer;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.module.impl.ModuleManagerImpl;
import me.earth.pingbypass.api.players.impl.PlayerRegistryImpl;
import me.earth.pingbypass.api.plugin.impl.PluginManagerImpl;
import me.earth.pingbypass.api.plugin.impl.PluginUnloadingService;
import me.earth.pingbypass.api.security.SecurityManagerDisabled;
import me.earth.pingbypass.api.side.Side;
import net.minecraft.network.chat.Component;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestPhobot {
    public static final Phobot PHOBOT;

    static {
        TestUtil.bootstrap();
        PingBypass pingBypass = new AbstractPingBypass(PingBypassApi.getEventBus(), DummyKeyboardAndMouse.INSTANCE, new CommandManagerImpl(),
                new ModuleManagerImpl(new Categories()), new ConfigManagerImpl(),
                new FileManagerImpl(Paths.get("")), new FileManagerImpl(Paths.get("")), new SecurityManagerDisabled(),
                new PluginManagerImpl(), new PlayerRegistryImpl(), new PlayerRegistryImpl(), null, new Chat() {
            @Override
            public void send(Component message) {

            }

            @Override
            public void send(Component message, String identifier) {

            }

            @Override
            public void sendWithoutLogging(Component message) {

            }

            @Override
            public void sendWithoutLogging(Component message, String identifier) {

            }

            @Override
            public void delete(String identifier) {

            }
        }, Side.CLIENT) {};

        new CommonModuleInitializer() {}.registerCommonModules(pingBypass);
        PhobotPlugin plugin = new PhobotPlugin();
        plugin.load(pingBypass, new PluginUnloadingService(pingBypass));
        PHOBOT = PhobotApi.getPhobot();
        assertNotNull(PHOBOT);
    }

    public static BlockPlacer getBlockPlacer() {
        return new BlockPlacer(PHOBOT.getLocalPlayerPositionService(), PHOBOT.getMotionUpdateService(), PHOBOT.getInventoryService(), PHOBOT.getMinecraft(), PHOBOT.getAntiCheat());
    }

}
