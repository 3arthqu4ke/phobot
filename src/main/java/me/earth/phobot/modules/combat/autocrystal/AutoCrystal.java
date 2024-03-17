package me.earth.phobot.modules.combat.autocrystal;

import me.earth.phobot.Phobot;
import me.earth.phobot.services.SurroundService;
import me.earth.pingbypass.api.gui.hud.DisplaysHudInfo;
import me.earth.pingbypass.api.module.impl.Categories;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class AutoCrystal extends CrystalPlacingModule implements DisplaysHudInfo {
    public AutoCrystal(Phobot phobot, SurroundService surroundService) {
        super(phobot, surroundService, "AutoCrystal", Categories.COMBAT, "Automatically places crystals. The settings were designed specifically for a ping of about 30ms.");
    }

    @Override
    public String getHudInfo() {
        Entity target = this.getTarget();
        if (target instanceof Player player) {
            return player.getScoreboardName();
        }

        return null;
    }

}
