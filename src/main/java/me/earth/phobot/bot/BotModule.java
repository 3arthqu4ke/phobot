package me.earth.phobot.bot;

import me.earth.phobot.Phobot;
import me.earth.phobot.modules.PhobotModule;
import me.earth.pingbypass.api.module.impl.Categories;

public class BotModule extends PhobotModule {
    public BotModule(Phobot phobot) {
        super(phobot, "Phobot", Categories.CLIENT, "Turns Phobot on and off.");
    }

}
