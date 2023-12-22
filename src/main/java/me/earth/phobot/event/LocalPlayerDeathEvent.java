package me.earth.phobot.event;

import net.minecraft.client.player.LocalPlayer;

public record LocalPlayerDeathEvent(LocalPlayer player) {
}
