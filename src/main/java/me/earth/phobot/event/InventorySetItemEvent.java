package me.earth.phobot.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record InventorySetItemEvent(Player player, int slot, ItemStack stack) {

}
