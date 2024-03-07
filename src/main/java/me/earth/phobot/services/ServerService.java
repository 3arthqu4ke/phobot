package me.earth.phobot.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.event.ConnectEvent;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

@Slf4j
@Getter
public class ServerService extends SubscriberImpl {
    public static final String IP_OVH = "ovh.crystalpvp.cc.";
    public static final String IP = "crystalpvp.cc";
    public static final String IP_US = "us.crystalpvp.cc";
    public static final @Unmodifiable Set<String> IPS = Set.of(IP_OVH, IP, IP_US);
    private boolean currentServerCC;
    private ConnectEvent current;

    public ServerService() {
        listen(new Listener<PacketEvent.Send<ClientIntentionPacket>>() {
            @Override
            public void onEvent(PacketEvent.Send<ClientIntentionPacket> event) {
                if (event.getPacket().intention() == ClientIntent.LOGIN) {
                    currentServerCC = IPS.contains(event.getPacket().hostName());
                    log.info("Joining " + event.getPacket().hostName());
                    log.info("Is cc: " + currentServerCC);
                }
            }
        });

        listen(new Listener<ConnectEvent>() {
            @Override
            public void onEvent(ConnectEvent event) {
                current = event;
            }
        });
    }

}
