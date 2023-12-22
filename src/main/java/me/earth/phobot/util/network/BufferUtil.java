package me.earth.phobot.util.network;

import lombok.experimental.UtilityClass;
import net.minecraft.network.FriendlyByteBuf;

@UtilityClass
public class BufferUtil {
    public static void release(FriendlyByteBuf buf) {
        buf.release(buf.refCnt());
    }

}
