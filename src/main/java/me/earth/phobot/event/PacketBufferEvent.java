package me.earth.phobot.event;

import io.netty.buffer.ByteBuf;

public record PacketBufferEvent(ByteBuf buffer) {

}
