package me.earth.phobot.modules.combat;

import me.earth.phobot.Phobot;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.services.SurroundService;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.commons.event.SafeListener;
import me.earth.pingbypass.commons.event.loop.GameloopEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

public class AutoTotem extends PhobotModule {
    private final Setting<Boolean> invincibilityFrames = bool("InvincibilityFrames", false, "Makes use of Invincibility Frames.");
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
                }
            }
        });
    }

    private boolean isSafe(LocalPlayer player) {
        return surroundService.isSurrounded()
                || invincibilityFrames.getValue() && !phobot.getInvincibilityFrameService().willDamageKill(player, phobot.getDamageService().getHighestDamage(), 400);
    }

}
