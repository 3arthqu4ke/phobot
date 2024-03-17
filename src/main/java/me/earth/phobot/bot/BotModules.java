package me.earth.phobot.bot;

import lombok.Getter;
import me.earth.phobot.modules.combat.*;
import me.earth.phobot.modules.combat.autocrystal.AutoCrystal;
import me.earth.phobot.modules.misc.AutoEat;
import me.earth.phobot.modules.misc.AutoEchest;
import me.earth.phobot.modules.misc.Repair;
import me.earth.phobot.modules.movement.Scaffold;
import me.earth.pingbypass.api.module.ModuleManager;

// TODO: module caches?
/**
 * Modules that Phobot actively makes use of.
 */
@Getter
public class BotModules {
    private final Bomber bomber;
    private final AutoEat autoEat;
    private final AutoCrystal autoCrystal;
    private final KillAura killAura;
    private final Suicide suicide;
    private final AutoTrap autoTrap;
    private final AutoMine autoMine;
    private final SelfTrap selfTrap;
    private final Surround surround;
    private final AutoEchest autoEchest;
    private final Scaffold scaffold;
    private final Repair repair;

    public BotModules(ModuleManager modules) {
        this.bomber = modules.getByClass(Bomber.class).orElseThrow();
        this.autoEat = modules.getByClass(AutoEat.class).orElseThrow();
        this.autoCrystal = modules.getByClass(AutoCrystal.class).orElseThrow();
        this.killAura = modules.getByClass(KillAura.class).orElseThrow();
        this.suicide = modules.getByClass(Suicide.class).orElseThrow();
        this.autoTrap = modules.getByClass(AutoTrap.class).orElseThrow();
        this.autoMine = modules.getByClass(AutoMine.class).orElseThrow();
        this.selfTrap = modules.getByClass(SelfTrap.class).orElseThrow();
        this.surround = modules.getByClass(Surround.class).orElseThrow();
        this.autoEchest = modules.getByClass(AutoEchest.class).orElseThrow();
        this.scaffold = modules.getByClass(Scaffold.class).orElseThrow();
        this.repair = modules.getByClass(Repair.class).orElseThrow();
    }

}
