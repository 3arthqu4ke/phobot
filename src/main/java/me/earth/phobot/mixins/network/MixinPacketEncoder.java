package me.earth.phobot.mixins.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import me.earth.phobot.event.PacketBufferEvent;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketEncoder.class)
public abstract class MixinPacketEncoder {
    @Shadow @Final private PacketFlow flow;

    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/jfr/JvmProfiler;onPacketSent(IILjava/net/SocketAddress;I)V", shift = At.Shift.AFTER))
    private void encodeHook(ChannelHandlerContext channelHandlerContext, Packet<?> packet, ByteBuf byteBuf, CallbackInfo ci) {
        try {
            if (flow == PacketFlow.SERVERBOUND) {
                PingBypassApi.getEventBus().post(new PacketBufferEvent(byteBuf));
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

}
