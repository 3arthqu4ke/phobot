package me.earth.phobot.mixins.entity;

import me.earth.phobot.event.InventorySetItemEvent;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public abstract class MixinInventory {
    @Shadow @Final public Player player;

    @Inject(method = "setItem", at = @At("RETURN"))
    private void setItemHook(int slot, ItemStack itemStack, CallbackInfo ci) {
        PingBypassApi.getEventBus().post(new InventorySetItemEvent(player, slot, itemStack));
    }

}
