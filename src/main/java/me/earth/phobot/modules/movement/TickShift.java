package me.earth.phobot.modules.movement;

import me.earth.phobot.Phobot;
import me.earth.phobot.modules.PhobotModule;
import me.earth.pingbypass.api.module.impl.Categories;

// TODO:!
public class TickShift extends PhobotModule {
    public TickShift(Phobot phobot) {
        super(phobot, "TickShift", Categories.MOVEMENT, "Allows you to timer after not having moved for a bit.");
    }

}
