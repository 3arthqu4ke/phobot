package me.earth.phobot.util;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

@UtilityClass
public class NullabilityUtil {
    public static void safe(Minecraft mc, PlayerLevelAndGamemodeConsumer action) {
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        MultiPlayerGameMode gameMode = mc.gameMode;
        if (level != null && player != null && gameMode != null) {
            action.accept(player, level, gameMode);
        }
    }

    @FunctionalInterface
    public interface PlayerLevelAndGamemodeConsumer {
        void accept(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode);
    }

}
