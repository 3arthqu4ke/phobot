package me.earth.phobot.modules.movement;

import me.earth.phobot.Phobot;
import me.earth.phobot.mixins.IMinecraft;
import me.earth.phobot.modules.PhobotModule;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.loop.GameloopEvent;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;

public class Timer extends PhobotModule {
    public Timer(Phobot pingBypass) {
        super(pingBypass, "Timer", Categories.MISC, "Speeds up the game.");
        Setting<Float> factor = floating("Factor", 1.0888f, 0.1f, 10.0f, "Factor by which the game will be sped up.");
        Setting<Integer> lag = number("Lag", 1000, 0, 10_000, "Time in milliseconds for which to stop the timer after getting lagged back.");
        listen(new Listener<GameloopEvent>() {
            @Override
            public void onEvent(GameloopEvent event) {
                // SOFTTODO: sky moves buggy, probably because render uses partialTick, and server sends packet with time
                if (phobot.getLagbackService().passed(lag.getValue())) {
                    float tickDelta = ((IMinecraft) mc).getTimer().tickDelta;
                    float partialTick = ((IMinecraft) mc).getTimer().partialTick;
                    // reverse partial tick to where they were before Timer.advanceTime was called
                    partialTick += (float) event.getTicks();
                    partialTick -= tickDelta;

                    // apply our factor and act out Timer.advanceTime
                    tickDelta *= factor.getValue();
                    partialTick += tickDelta;

                    int ticks = (int) partialTick;
                    event.setTicks(ticks);
                    ((IMinecraft) mc).getTimer().tickDelta = tickDelta;
                    ((IMinecraft) mc).getTimer().partialTick = partialTick - ticks;
                }
            }
        });
    }

}
