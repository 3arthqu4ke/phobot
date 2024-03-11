package me.earth.phobot.modules.client;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.earth.phobot.event.AuthenticationEvent;
import me.earth.phobot.mixins.network.IServerboundHelloPacket;
import me.earth.phobot.modules.PhobotNameSpacedModule;
import me.earth.phobot.settings.UUIDSetting;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.command.CommandSource;
import me.earth.pingbypass.api.command.impl.builder.ExtendedLiteralArgumentBuilder;
import me.earth.pingbypass.api.command.impl.module.HasCustomModuleCommand;
import me.earth.pingbypass.api.event.CancellingListener;
import me.earth.pingbypass.api.event.impl.PingBypassInitializedEvent;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Random;
import java.util.UUID;

public class AccountSpoof extends PhobotNameSpacedModule implements HasCustomModuleCommand {
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_";

    private final Setting<String> name = string("Name", "Phobot", "The name to spoof");
    private final Setting<UUID> uuid = new UUIDSetting().withName("UUID")
                                                        .withDescription("The UUID of to spoof.")
                                                        .register(this);
    private final Setting<Boolean> rnd = bool("Random", false, "Gives you a random name every time the game starts.");

    public AccountSpoof(PingBypass pingBypass) {
        super(pingBypass, "AccountSpoof", Categories.CLIENT, "Act like someone else on cracked servers.");
        listen(new CancellingListener<>(AuthenticationEvent.class));
        listen(new Listener<PacketEvent.Send<ServerboundHelloPacket>>() {
            @Override
            public void onEvent(PacketEvent.Send<ServerboundHelloPacket> event) {
                IServerboundHelloPacket accessor = IServerboundHelloPacket.class.cast(event.getPacket());
                var accountName = name.getValue();
                var accountUUID = uuid.getValue();
                //noinspection DataFlowIssue
                accessor.setName(accountName);
                accessor.setProfileId(accountUUID);
                IntegratedServer server = mc.getSingleplayerServer();
                if (server != null) {
                    server.setSingleplayerProfile(new GameProfile(accountUUID, accountName));
                }
            }
        });

        name.addPreObserver(event -> {
            if (event.getValue() != null && event.getValue().length() > 16) {
                pingBypass.getChat().send(Component.literal("")
                        .append(Component.literal(event.getValue()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                        .append(Component.literal(" is longer than 16 characters!").withStyle(ChatFormatting.RED)));
                event.setCancelled(true);
            }
        });

        MutableObject<Listener<?>> reference = new MutableObject<>();
        reference.setValue(new Listener<PingBypassInitializedEvent>() {
            @Override
            public void onEvent(PingBypassInitializedEvent event) {
                if (reference.getValue() != null) {
                    getListeners().remove(reference.getValue());
                    getPingBypass().getEventBus().unregister(reference.getValue());
                }

                if (rnd.getValue()) {
                    selectRandomName();
                    uuid.setValue(UUID.randomUUID());
                }
            }
        });

        listen(reference.getValue());
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(new ExtendedLiteralArgumentBuilder<CommandSource>("Shuffle").executes(ctx -> {
            selectRandomName();
            uuid.setValue(UUID.randomUUID());
            return Command.SINGLE_SUCCESS;
        }));
    }

    public void selectRandomName() {
        StringBuilder builder = new StringBuilder(16);
        Random random = new Random();
        for (int i = 0; i < 16; i++) {
            builder.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }

        name.setValue(builder.toString());
    }

}
