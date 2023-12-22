package me.earth.phobot.commands.arguments;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.earth.pingbypass.api.registry.impl.OrderedRegistryImpl;
import me.earth.pingbypass.api.traits.Nameable;
import me.earth.pingbypass.api.traits.NameableImpl;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NameablePacketRegistry extends OrderedRegistryImpl<Nameable> {
    public static NameablePacketRegistry load() {
        NameablePacketRegistry registry = new NameablePacketRegistry();
        for (ConnectionProtocol protocol : ConnectionProtocol.values()) {
            for (PacketFlow packetFlow : PacketFlow.values()) {
                Int2ObjectMap<Class<? extends Packet<?>>> map =  protocol.getPacketsByIds(packetFlow);
                for (Class<?> packet : map.values()) {
                    registry.register(new NameableImpl(packet.getSimpleName()));
                }
            }
        }

        return registry;
    }

}
