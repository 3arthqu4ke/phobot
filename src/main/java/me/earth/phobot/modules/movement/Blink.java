package me.earth.phobot.modules.movement;

import com.mojang.authlib.GameProfile;
import lombok.Synchronized;
import me.earth.phobot.Phobot;
import me.earth.phobot.modules.AbstractFakePlayerModule;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.player.FakePlayer;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.loop.LocalPlayerUpdateEvent;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.gui.hud.DisplaysHudInfo;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;

import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class Blink extends AbstractFakePlayerModule implements DisplaysHudInfo {
    private static final int ID = -2352351;

    private final Setting<Integer> auto = number("Auto", 0, 0, 1000, "Automatically disables this module if this many packets have been cancelled. 0 means off.");
    private final Deque<Packet<?>> packets = new ConcurrentLinkedDeque<>();
    private final Deque<Packet<?>> pongs = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean noCancel = new AtomicBoolean();

    public Blink(Phobot phobot) {
        super(phobot, "Blink", Categories.MOVEMENT, "Move normally but appear to be teleporting for others.", ID);
        ResetUtil.onRespawnOrWorldChange(this, mc, () -> {
            packets.clear();
            pongs.clear();
            disable();
        });

        listen(new Listener<LocalPlayerUpdateEvent>() {
            @Override
            public void onEvent(LocalPlayerUpdateEvent event) {
                if (auto.getValue() > 0 && packets.size() >= auto.getValue()) {
                    disable();
                }
            }
        });

        listen(new Listener<PacketEvent.Send<?>>() {
            @Override
            public void onEvent(PacketEvent.Send<?> event) {
                Packet<?> packet = event.getPacket();
                synchronized (noCancel) {
                    if (packet instanceof ServerboundPongPacket) {
                        event.setCancelled(true);
                        pongs.add(packet);
                    } else if (!(packet instanceof ServerboundChatPacket
                            || packet instanceof ServerboundClientCommandPacket
                            || packet instanceof ServerboundKeepAlivePacket
                            || packet instanceof ServerboundSeenAdvancementsPacket
                            || packet instanceof ServerboundAcceptTeleportationPacket)) {
                        event.setCancelled(true);
                        packets.add(packet);
                    }
                }
            }
        });
    }

    @Override
    protected void onEnable() {
        super.onEnable();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        LocalPlayer player = mc.player;
        if (player != null) {
            send(player, packets);
            send(player, pongs);
        }
    }

    @Override
    protected FakePlayer instantiate(LocalPlayer player, ClientLevel level) {
        return new FakePlayer(level, new GameProfile(UUID.randomUUID(), player.getName() + " (Blink)"));
    }

    @Synchronized("noCancel")
    private void send(LocalPlayer player, Deque<Packet<?>> packets) {
        while (!packets.isEmpty()) {
            try {
                Packet<?> packet = packets.removeFirst();
                player.connection.send(packet);
            } catch (NoSuchElementException e) {
                return;
            }
        }
    }

    @Override
    public String getHudInfo() {
        return String.valueOf(packets.size());
    }

}
