package me.earth.phobot.modules.combat;

import me.earth.phobot.Phobot;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.services.SurroundService;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.InventoryUtil;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.loop.GameloopEvent;
import me.earth.pingbypass.api.input.Key;
import me.earth.pingbypass.api.input.Keys;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class AutoTotem extends PhobotModule {
    private final Setting<Offhand> mode = constant("Mode", Offhand.None, "What item you prefer in your offhand.");
    private final Setting<Boolean> invincibilityFrames = bool("InvincibilityFrames", false, "Makes use of Invincibility Frames.");
    private final Setting<Boolean> gap = bool("Sword-Gap", false, "Right click with a sword in your hand to eat a gap.");
    private final SurroundService surroundService;
    public AutoTotem(Phobot phobot, Suicide suicide, SurroundService surroundService) {
        super(phobot, "AutoTotem", Categories.COMBAT, "Prevents you from dying by using Totems.");
        this.surroundService = surroundService;
        listen(new SafeListener<GameloopEvent>(mc) {
            @Override
            public void onEvent(GameloopEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (suicide.isEnabled()) {
                    phobot.getInventoryService().getLockedIntoTotem().set(false);
                    return;
                }

                // TODO: take into account that we might get mined out
                float health = EntityUtil.getHealth(player);
                if (health <= 3.0f || health <= phobot.getDamageService().getHighestDamage() && !isSafe(player)) {
                    phobot.getInventoryService().use(context -> phobot.getInventoryService().getLockedIntoTotem().set(
                            context.switchTo(Items.TOTEM_OF_UNDYING, InventoryContext.DEFAULT_SWAP_SWITCH) != null));
                } else {
                    phobot.getInventoryService().getLockedIntoTotem().set(false);
                    phobot.getInventoryService().use(context -> {
                        boolean eating = gap.getValue()
                                && getPingBypass()
                                .getKeyBoardAndMouse()
                                .isPressed(Key.Type.MOUSE, Keys.MOUSE_2)
                                && InventoryUtil.isWeapon(context.getSelectedSlot().getItem());
                        if (eating) {
                            offhand(context, Items.ENCHANTED_GOLDEN_APPLE);
                            return;
                        }

                        switch (mode.getValue()) {
                            case Totem -> offhand(context, Items.TOTEM_OF_UNDYING);
                            case Crystal -> offhand(context, Items.END_CRYSTAL);
                        }
                    });
                }
            }
        });
    }

    private boolean isSafe(LocalPlayer player) {
        return surroundService.isSurrounded()
                || invincibilityFrames.getValue() && !phobot.getInvincibilityFrameService().willDamageKill(player, phobot.getDamageService().getHighestDamage(), 400);
    }

    private void offhand(InventoryContext context, Item item) {
        if (!context.getOffhand().getItem().is(item)) {
            context.switchTo(item, InventoryContext.DEFAULT_SWAP_SWITCH);
        }
    }

    private enum Offhand {
        None,
        Totem,
        Crystal
    }

}
