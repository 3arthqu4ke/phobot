package me.earth.phobot.mixins.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import me.earth.phobot.event.PacketBufferEvent;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketEncoder.class)
public abstract class MixinPacketEncoder {
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private AttributeKey<ConnectionProtocol.CodecData<?>> codecKey;

    @Inject(
        method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/profiling/jfr/JvmProfiler;onPacketSent(Lnet/minecraft/network/ConnectionProtocol;ILjava/net/SocketAddress;I)V",
            shift = At.Shift.AFTER))
    private void encodeHook(ChannelHandlerContext channelHandlerContext, Packet<?> packet, ByteBuf byteBuf, CallbackInfo ci) {
        try {
            Attribute<ConnectionProtocol.CodecData<?>> attribute = channelHandlerContext.channel().attr(this.codecKey);
            ConnectionProtocol.CodecData<?> codec = attribute.get();
            // TODO: instead capture locals?
            if (codec != null && codec.flow() == PacketFlow.SERVERBOUND) {
                PingBypassApi.getEventBus().post(new PacketBufferEvent(byteBuf));
            }
        } catch (Throwable throwable) {
            LOGGER.error("Failed in PacketBuffer encodeHook", throwable);
        }
    }

}
