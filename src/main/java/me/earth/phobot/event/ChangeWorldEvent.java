package me.earth.phobot.event;

import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

public record ChangeWorldEvent(@Nullable ClientLevel level) {

}
