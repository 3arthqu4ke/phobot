package me.earth.phobot.modules;

import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.module.impl.ModuleImpl;
import me.earth.pingbypass.api.traits.Nameable;

public class PhobotNameSpacedModule extends ModuleImpl {
    public PhobotNameSpacedModule(PingBypass pingBypass, String name, Nameable category, String description) {
        super(pingBypass, name, category, description);
    }

    // TODO: when pingbypass has namespace functionality: namespace "phobot"

}
