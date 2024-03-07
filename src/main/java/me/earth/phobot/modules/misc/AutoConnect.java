package me.earth.phobot.modules.misc;

import com.mojang.realmsclient.client.RealmsClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.event.ConnectEvent;
import me.earth.phobot.event.FirstTitleScreenRenderEvent;
import me.earth.phobot.mixins.screen.IDisconnectScreen;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.util.math.MathUtil;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.api.event.gui.GuiScreenEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class AutoConnect extends PhobotModule {
    private final Setting<Mode> reconnect = constant("Reconnect", Mode.Auto, "Automatically reconnects you when you lose your connection.");
    private final Setting<Integer> delay = number("Delay", 5, 0, 20, "Delay in seconds until we reconnect.");

    public AutoConnect(Phobot phobot) {
        super(phobot, "AutoConnect", Categories.MISC, "Automatically connects you to servers.");
        Setting<Boolean> connect = bool("Connect", false, "Automatically connects you to a server at start up.");
        Setting<String> ip = string("IP", "crystalpvp.cc", "The ip to automatically connect to.");
        Setting<Integer> port = number("Port", 25565, 0, 65535, "The port of the server to connect to.");
        MutableObject<Listener<?>> reference = new MutableObject<>();
        reference.setValue(new Listener<FirstTitleScreenRenderEvent>(-100_000) {
            @Override
            public void onEvent(FirstTitleScreenRenderEvent event) {
                if (reference.getValue() != null) {
                    getListeners().remove(reference.getValue());
                    getPingBypass().getEventBus().unregister(reference.getValue());
                }

                if (connect.getValue()) {
                    log.info("Connecting to " + ip.getValue() + ":" + port.getValue());
                    QuickPlay.connect(mc, new GameConfig.QuickPlayData(null, null, ip.getValue() + ":" + port.getValue(), null), RealmsClient.create(mc));
                }
            }
        });

        listen(reference.getValue());
        listen(new Listener<GuiScreenEvent<DisconnectedScreen>>() {
            @Override
            public void onEvent(GuiScreenEvent<DisconnectedScreen> event) {
                if (reconnect.getValue() != Mode.None && event.getScreen() instanceof IDisconnectScreen access) {
                    event.setCancelled(true);
                    mc.setScreen(new ReconnectScreen(AutoConnect.this, new TitleScreen(), event.getScreen().getTitle(), access.getReason()));
                }
            }
        });
    }

    public void reconnect() {
        ConnectEvent connectEvent = phobot.getServerService().getCurrent();
        if (connectEvent != null) {
            ConnectScreen.startConnecting(new TitleScreen(), mc, connectEvent.serverAddress(), connectEvent.serverData(), false);
        }
    }

    public enum Mode {
        None,
        Screen,
        Auto
    }

    public static class ReconnectScreen extends Screen {
        private static final Component TO_SERVER_LIST = Component.translatable("gui.toMenu");
        private static final Component TO_TITLE = Component.translatable("gui.toTitle");
        private final StopWatch.ForSingleThread timer = new StopWatch.ForSingleThread();
        private final Component buttonText = TO_SERVER_LIST;
        private final GridLayout layout = new GridLayout();
        private final AutoConnect module;
        private final Component reason;
        private final Screen parent;
        private Button toggle;
        private Button reconnect;

        public ReconnectScreen(AutoConnect module, Screen parent, Component title, Component reason) {
            super(title);
            this.module = module;
            this.parent = parent;
            this.reason = reason;
            this.timer.reset();
        }

        @Override
        protected void init() {
            // TODO: smaller spacing between buttons!
            this.layout.defaultCellSetting().alignHorizontallyCenter().padding(10);
            GridLayout.RowHelper rowHelper = this.layout.createRowHelper(1);
            rowHelper.addChild(new StringWidget(this.title, this.font));
            rowHelper.addChild(new MultiLineTextWidget(this.reason, this.font).setMaxWidth(this.width - 50).setCentered(true));

            reconnect = Button.builder(Component.literal(getReconnectButtonText()), button -> module.reconnect()).build();
            toggle = Button.builder(getToggleText(), button -> {
                if (module.reconnect.getValue() == Mode.Auto) {
                    module.getReconnect().setValue(Mode.Screen);
                } else {
                    module.getReconnect().setValue(Mode.Auto);
                }

                timer.reset();
                if (reconnect != null) {
                    reconnect.setMessage(Component.literal(getReconnectButtonText()));
                }

                if (toggle != null) {
                    toggle.setMessage(getToggleText());
                }
            }).build();

            rowHelper.addChild(reconnect);
            rowHelper.addChild(toggle);
            assert this.minecraft != null;
            Button backToMenu = this.minecraft.allowsMultiplayer()
                    ? Button.builder(this.buttonText, button -> this.minecraft.setScreen(this.parent)).build()
                    : Button.builder(TO_TITLE, button -> this.minecraft.setScreen(new TitleScreen())).build();
            rowHelper.addChild(backToMenu);
            this.layout.arrangeElements();
            this.layout.visitWidgets(this::addRenderableWidget);
            this.repositionElements();
        }

        @Override
        public void tick() {
            if (toggle != null) {
                toggle.setMessage(getToggleText());
                toggle.setFocused(false);
            }

            if (reconnect != null) {
                reconnect.setMessage(Component.literal(getReconnectButtonText()));
            }

            if (module.isEnabled() && module.reconnect.getValue() == Mode.Auto && timer.passed(TimeUnit.SECONDS.toMillis(module.delay.getValue()))) {
                module.reconnect();
            }
        }

        @Override
        protected void repositionElements() {
            FrameLayout.centerInRectangle(this.layout, this.getRectangle());
        }

        @Override
        public @NotNull Component getNarrationMessage() {
            return CommonComponents.joinForNarration(this.title, this.reason);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int x, int y, float delta) {
            this.renderBackground(guiGraphics, x, y, delta);
            super.render(guiGraphics, x, y, delta);
        }

        private String getReconnectButtonText() {
            return "Reconnect" + ((module.isEnabled() && module.getReconnect().getValue() == Mode.Auto)
                    ? (" (" + MathUtil.round(((module.getDelay().getValue() - timer.getPassedTime() / 1000.0)), 1) + "s)")
                    : "");
        }

        private Component getToggleText() {
            return Component.literal(module.getName())
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(module.isEnabled() && module.getReconnect().getValue() == Mode.Auto ? "On" : "Off")
                            .withStyle(module.isEnabled() && module.getReconnect().getValue() == Mode.Auto ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
    }

}
