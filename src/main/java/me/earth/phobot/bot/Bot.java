package me.earth.phobot.bot;

import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.phobot.bot.behaviours.*;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.services.SurroundService;
import me.earth.pingbypass.api.event.api.Subscriber;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.world.entity.Entity;

/**
 * A module for configuring and turning the Phobot on and off.
 */
@Getter
public class Bot extends PhobotModule {
    private final Setting<Integer> spawnHeight = number("BuildHeight", 57, -64, 320, "Above this height the bot will jump down.");
    private final Setting<Boolean> rotate = bool("Rotate", true, "Makes you look at the current target.");
    private final Setting<Boolean> duel = bool("Duel", false, "Turn on for when you want to /duel someone.");

    private final SurroundService surroundService;
    private final BotModules modules;

    private final JumpDownFromSpawn jumpDownFromSpawn;
    private final RunningAway runningAway;
    private final Targeting targeting;
    private final Escape escape;

    /**
     * Creates a new bot module.
     * This method requires that all modules in {@link BotModules} are registered on the PingBypass instance for this!
     *
     * @param phobot the Phobot instance for this module.
     * @param surroundService a surroundService for checking if we are surrounded.
     */
    public Bot(Phobot phobot, SurroundService surroundService) {
        super(phobot, "Phobot", Categories.CLIENT, "Turns Phobot on and off.");
        this.surroundService = surroundService;
        this.modules = new BotModules(getPingBypass().getModuleManager());
        this.jumpDownFromSpawn = registerBehaviour(new JumpDownFromSpawn(this));
        this.runningAway = registerBehaviour(new RunningAway(this));
        this.targeting = registerBehaviour(new Targeting(this));
        this.escape = registerBehaviour(new Escape(this));
        registerBehaviour(new Chasing(this));
        registerBehaviour(new Suiciding(this));
        registerBehaviour(new Rotate(this));
        registerBehaviour(new Swording(this));
        registerBehaviour(new Surrounding(this));
        registerBehaviour(new Trapping(this));
        registerBehaviour(new MineObsidian(this));
        registerBehaviour(new Repairing(this));
    }

    // TODO: duel detection?, theres quite some issues with that
    public boolean isDueling() {
        return duel.getValue();
    }

    public Entity getTarget() {
        return targeting.getTarget();
    }

    protected <T extends Subscriber> T registerBehaviour(T behaviour) {
        behaviour.getListeners().forEach(this::listen);
        return behaviour;
    }

}
