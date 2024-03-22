package me.earth.phobot.bot;

import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.phobot.bot.behaviours.*;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.pathfinder.parallelization.ParallelSearchManager;
import me.earth.phobot.services.SurroundService;
import me.earth.pingbypass.api.event.api.Subscriber;
import me.earth.pingbypass.api.gui.hud.DisplaysHudInfo;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.world.entity.Entity;

/**
 * A module for configuring and turning the Phobot on and off.
 */
@Getter
public class Bot extends PhobotModule implements DisplaysHudInfo {
    private final Setting<Integer> spawnHeight = number("BuildHeight", 57, -64, 320, "Above this height the bot will jump down.");
    private final Setting<Integer> parallelSearches = number("Parallel-Searches", 6, 1, 32, "The amount of path searches the bot is allowed to execute in parallel.");
    private final Setting<Double> damageLenience = precise("Damage-Leniency", 0.0, -15.0, 15.0, "How much more or less damage we are willing to accept when jumping back into a hole.");
    private final Setting<Double> leaveHealth = precise("Leave-Health", 15.0, 0.0, 20.0, "How much health the bot should have before leaving a hole.");
    private final Setting<Boolean> rotate = bool("Rotate", true, "Makes you look at the current target.");
    private final Setting<Boolean> duel = bool("Duel", false, "Turn on for when you want to /duel someone.");
    private final Setting<Boolean> aura = bool("Aura", true, "Uses KillAura.");
    private final Setting<Boolean> suicide = bool("Suicide", false, "Suicides you if you are out of gear, or far away from Spawn with no enemies in sight.");

    private final ParallelSearchManager pathSearchManager = new ParallelSearchManager();
    private final SurroundService surroundService;
    private final BotModules modules;

    private final JumpDownFromSpawn jumpDownFromSpawn;
    private final RunningAway runningAway;
    private final Targeting targeting;
    private final Escape escape;

    private boolean acEnabled = false;

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
        registerBehaviour(new AlwaysOnModules(this));
        registerBehaviour(new Chasing(this));
        registerBehaviour(new Suiciding(this));
        registerBehaviour(new Rotate(this));
        registerBehaviour(new Swording(this));
        registerBehaviour(new Surrounding(this));
        registerBehaviour(new Trapping(this));
        registerBehaviour(new MineObsidian(this));
        registerBehaviour(new Repairing(this));
        registerBehaviour(new Roaming(this));
    }

    @Override
    protected void onEnable() {
        acEnabled = getModules().getAutoCrystal().isEnabled();
    }

    @Override
    protected void onDisable() {
        if (!acEnabled) {
            getModules().getAutoCrystal().disable();
        }

        getModules().getAutoEat().disable();
        getModules().getSuicide().disable();
        getModules().getAutoTrap().disable();
        getModules().getAutoEchest().disable();
        getModules().getScaffold().disable();
        if (!getModules().getSelfTrap().getAuto().getValue()) {
            getModules().getSelfTrap().disable();
        }

        pathSearchManager.cancel();
        phobot.getPathfinder().getLevelBoundTaskManager().cancelAll();
        phobot.getPathfinder().cancel();
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

    @Override
    public String getHudInfo() {
        Entity target = getTarget();
        if (target != null) {
            return target.getScoreboardName();
        }

        return null;
    }

}
