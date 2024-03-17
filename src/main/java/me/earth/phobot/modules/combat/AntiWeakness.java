package me.earth.phobot.modules.combat;

import lombok.Getter;
import me.earth.phobot.modules.PhobotNameSpacedModule;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;

/**
 * @see me.earth.phobot.services.AttackService
 */
@Getter
public class AntiWeakness extends PhobotNameSpacedModule {
    private final Setting<Mode> mode = constant("Mode", Mode.Switch, "Switch uses a normal switch, Click will use Inventory actions.");

    public AntiWeakness(PingBypass pingBypass) {
        super(pingBypass, "AntiWeakness", Categories.COMBAT, "Switches to a tool and back to attack crystals when you have weakness.");
    }

    public enum Mode {
        Switch,
        Click
    }

}
