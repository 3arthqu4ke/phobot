package me.earth.phobot.modules.misc;

import me.earth.phobot.modules.PhobotNameSpacedModule;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.network.protocol.common.*;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

import java.util.concurrent.atomic.AtomicInteger;

// TODO: remove, this is a bad idea?
public class Packets extends PhobotNameSpacedModule {
    private final AtomicInteger processingPosition = new AtomicInteger();

    public Packets(PingBypass pingBypass) {
        super(pingBypass, "Packets", Categories.MISC, "Tweaks for packets.");
        Setting<Boolean> fast = bool("Fast", true, "Quickly answers the received packets.");
        listen(new Listener<PacketEvent.Receive<ClientboundKeepAlivePacket>>() {
            @Override
            public void onEvent(PacketEvent.Receive<ClientboundKeepAlivePacket> event) {
                synchronized (processingPosition) {
                    if (fast.getValue() && processingPosition.get() == 0) {
                        event.setCancelled(true);
                        event.getConnection().pingbypass$send(new ServerboundKeepAlivePacket(event.getPacket().getId()));
                    }
                }
            }
        });

        listen(new Listener<PacketEvent.Receive<ClientboundPingPacket>>() {
            @Override
            public void onEvent(PacketEvent.Receive<ClientboundPingPacket> event) {
                synchronized (processingPosition) {
                    if (fast.getValue() && processingPosition.get() == 0) {
                        event.setCancelled(true);
                        event.getConnection().pingbypass$send(new ServerboundPongPacket(event.getPacket().getId()));
                    }
                }
            }
        });

        listen(new Listener<PacketEvent.Receive<ClientboundResourcePackPushPacket>>() {
            @Override
            public void onEvent(PacketEvent.Receive<ClientboundResourcePackPushPacket> event) {
                synchronized (processingPosition) {
                    if (fast.getValue() && processingPosition.get() == 0) {
                        event.setCancelled(true);
                        event.getConnection().pingbypass$send(new ServerboundResourcePackPacket(event.getPacket().id(), ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD));
                    }
                }
            }
        });

        listen(new Listener<PacketEvent.Receive<ClientboundPlayerPositionPacket>>() {
            @Override
            public void onEvent(PacketEvent.Receive<ClientboundPlayerPositionPacket> event) {
                synchronized (processingPosition) {
                    processingPosition.incrementAndGet();
                }
            }
        });

        listen(new Listener<PacketEvent.PostReceive<ClientboundPlayerPositionPacket>>() {
            @Override
            public void onEvent(PacketEvent.PostReceive<ClientboundPlayerPositionPacket> event) {
                mc.submit(() -> {
                    synchronized (processingPosition) {
                        processingPosition.decrementAndGet();
                    }
                });
            }
        });
    }

    @Override
    protected void onEnable() {
        processingPosition.set(0);
    }

}
