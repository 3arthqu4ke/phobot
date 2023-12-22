package me.earth.phobot.mixins.screen;

import me.earth.phobot.modules.misc.XCarry;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen {
    @Redirect(
        method = "checkHotbarMouseClicked",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;getCarried()Lnet/minecraft/world/item/ItemStack;"),
        require = 0)
    private ItemStack checkHotbarMouseClicked$getCarriedHook(AbstractContainerMenu menu) {
        return phobot$redirectGetCarried(menu);
    }

    // second redirect in case the first redirect fails due to some redirect conflict and require = 0 TODO: is there something like fallback redirects in Mixin?
    @Redirect(
        method = "checkHotbarMouseClicked",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"),
        require = 0)
    private boolean checkHotbarMouseClicked$isEmptyHook(ItemStack stack) {
        return phobot$redirectIsEmpty(stack);
    }

    @Redirect(
        method = "checkHotbarKeyPressed",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;getCarried()Lnet/minecraft/world/item/ItemStack;"),
        require = 0)
    private ItemStack checkHotbarKeyPressed$getCarriedHook(AbstractContainerMenu menu) {
        return phobot$redirectGetCarried(menu);
    }

    @Redirect(
        method = "checkHotbarKeyPressed",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"),
        require = 0)
    private boolean checkHotbarKeyPressed$isEmptyHook(ItemStack stack) {
        return phobot$redirectIsEmpty(stack);
    }

    @Unique
    private ItemStack phobot$redirectGetCarried(AbstractContainerMenu menu) {
        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            XCarry.SwapEventWithCarried event = new XCarry.SwapEventWithCarried();
            PingBypassApi.getEventBus().post(event);
            if (event.isCancelled()) {
                carried = ItemStack.EMPTY;
            }
        }

        return carried;
    }

    @Unique
    private boolean phobot$redirectIsEmpty(ItemStack stack) {
        boolean empty = stack.isEmpty();
        if (!empty) {
            XCarry.SwapEventWithCarried event = new XCarry.SwapEventWithCarried();
            PingBypassApi.getEventBus().post(event);
            empty = event.isCancelled();
        }

        return empty;
    }

}
