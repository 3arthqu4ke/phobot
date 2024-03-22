package me.earth.phobot.bot.behaviours;

import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.pathfinder.parallelization.HasPriority;
import me.earth.phobot.pathfinder.parallelization.ParallelSearchManager;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.loop.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

/**
 * Base class for {@link Bot} behaviour.
 */
public abstract class Behaviour extends SubscriberImpl implements HasPriority {
    public static final int PRIORITY_FIRST = 0;
    public static final int PRIORITY_JUMP_DOWN = -1;
    public static final int PRIORITY_TARGET = -2;
    public static final int PRIORITY_SUICIDE = -3;
    public static final int PRIORITY_MINE_AND_REPAIR = -4;
    // TODO: get into safe hole?
    public static final int PRIORITY_RUN_AWAY = -5;
    public static final int PRIORITY_ESCAPE = -6;
    public static final int PRIORITY_CHASE = -7;
    public static final int PRIORITY_SCAFFOLD = -8;
    public static final int PRIORITY_SWORD = -9;
    public static final int PRIORITY_LAST = -10;

    @Getter
    protected final int priority;
    protected final ParallelSearchManager pathSearchManager;
    protected final PingBypass pingBypass;
    protected final Phobot phobot;
    protected final Minecraft mc;
    protected final Bot bot;

    public Behaviour(Bot bot, int priority) {
        this.pathSearchManager = bot.getPathSearchManager();
        this.pingBypass = bot.getPingBypass();
        this.priority = priority;
        this.phobot = bot.getPhobot();
        this.mc = bot.getPingBypass().getMinecraft();
        this.bot = bot;
        listen(new SafeListener<TickEvent>(mc, priority) {
            @Override
            public void onEvent(TickEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                update(player, level, gameMode);
            }
        });
    }

    protected abstract void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode);

}
