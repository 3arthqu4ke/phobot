package me.earth.phobot.modules;

import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.pingbypass.api.traits.Nameable;

@Getter
public class PhobotModule extends PhobotNameSpacedModule {
    protected final Phobot phobot;

    public PhobotModule(Phobot phobot, String name, Nameable category, String description) {
        super(phobot.getPingBypass(), name, category, description);
        this.phobot = phobot;
    }

}
