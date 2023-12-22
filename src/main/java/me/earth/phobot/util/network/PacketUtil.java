package me.earth.phobot.util.network;

import io.netty.buffer.Unpooled;
import lombok.experimental.UtilityClass;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;

@UtilityClass
public class PacketUtil {
    public static ServerboundInteractPacket getAttackPacket(int entityId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(entityId);
            // buf.writeEnum(ServerboundInteractPacket.ActionType.ATTACK)
            buf.writeVarInt(1); // ordinal of ActionType.ATTACK, since it is private
            // Action.ATTACK does not write anything on the ByteBuffer, we skip that
            buf.writeBoolean(false); // not using usingSecondaryAction
            return new ServerboundInteractPacket(buf);
        } finally {
            BufferUtil.release(buf);
        }
    }

}
