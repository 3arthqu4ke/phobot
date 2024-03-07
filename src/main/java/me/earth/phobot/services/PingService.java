package me.earth.phobot.services;

import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.event.network.PostListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

public class PingService extends SubscriberImpl {
    private int currentPing;

    public PingService(Minecraft mc) {
        listen(new PostListener.Safe<ClientboundPlayerInfoUpdatePacket>(mc) {
            @Override
            public void onSafeEvent(PacketEvent.PostReceive<ClientboundPlayerInfoUpdatePacket> event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (event.getPacket().actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY)) {
                    for (ClientboundPlayerInfoUpdatePacket.Entry entry : event.getPacket().entries()) {
                        if (player.getGameProfile().getId().equals(entry.profileId())) {
                            currentPing = entry.latency();
                            return;
                        }
                    }
                }
            }
        });
    }

}
