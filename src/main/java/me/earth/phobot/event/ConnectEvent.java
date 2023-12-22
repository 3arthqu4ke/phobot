package me.earth.phobot.event;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public record ConnectEvent(ServerAddress serverAddress, ServerData serverData) {

}
