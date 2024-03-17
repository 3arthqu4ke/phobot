package me.earth.phobot.modules.movement;

import me.earth.phobot.Phobot;
import me.earth.phobot.event.PostInputTickEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

/**
 * Starts crouching when lagged.
 */
public class AntiRubberBand extends PhobotModule {
    private final Setting<Integer> time = number("Time", 500, 0, 5_000, "Time in milliseconds to crouch for after lagging.");

    public AntiRubberBand(Phobot phobot) {
        super(phobot, "AntiRubberBand", Categories.MOVEMENT, "Starts crouching when you get rubber-banded.");
        listen(new SafeListener<PostInputTickEvent>(mc) {
            @Override
            public void onEvent(PostInputTickEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (!phobot.getLagbackService().passed(time.getValue())) {
                    player.input.shiftKeyDown = true;
                }
            }
        });
    }

}
