package me.earth.phobot.commands.arguments;

import me.earth.pingbypass.api.command.impl.arguments.NameableArgumentType;
import me.earth.pingbypass.api.traits.Nameable;
import me.earth.pingbypass.api.traits.Streamable;

// TODO: fix suggestion not showing up!!!
public class PacketArgumentType implements NameableArgumentType<Nameable> {
    private static final NameablePacketRegistry PACKET_REGISTRY = NameablePacketRegistry.load();
    
    @Override
    public String getType() {
        return "packet";
    }

    @Override
    public Streamable<Nameable> getNameables() {
        return PACKET_REGISTRY;
    }

}
