package me.earth.phobot.util;

import lombok.experimental.UtilityClass;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

@UtilityClass
public class InventoryUtil {
    public static boolean isHoldingWeapon(LivingEntity entity) {
        return isWeapon(getMainHand(entity));
    }

    public static ItemStack getMainHand(LivingEntity entity) {
        return entity.getItemInHand(InteractionHand.MAIN_HAND);
    }

    public static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
    }

}
