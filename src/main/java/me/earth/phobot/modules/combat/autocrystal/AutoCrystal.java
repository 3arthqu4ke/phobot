package me.earth.phobot.modules.combat.autocrystal;

import me.earth.phobot.Phobot;
import me.earth.phobot.services.SurroundService;
import me.earth.pingbypass.api.module.impl.Categories;

public class AutoCrystal extends CrystalPlacingModule {
    public AutoCrystal(Phobot phobot, SurroundService surroundService) {
        super(phobot, surroundService, "AutoCrystal", Categories.COMBAT, "Automatically places crystals. The settings were designed specifically for a ping of about 30ms.");
    }

}
