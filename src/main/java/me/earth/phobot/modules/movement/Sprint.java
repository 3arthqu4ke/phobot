package me.earth.phobot.modules.movement;

import me.earth.phobot.Phobot;
import me.earth.phobot.modules.PhobotModule;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.loop.LocalPlayerUpdateEvent;
import me.earth.pingbypass.api.module.impl.Categories;
import net.minecraft.client.player.LocalPlayer;

public class Sprint extends PhobotModule {
    public Sprint(Phobot phobot) {
        super(phobot, "Sprint", Categories.MOVEMENT, "Makes you always sprint.");
        listen(new Listener<LocalPlayerUpdateEvent>() {
            @Override
            public void onEvent(LocalPlayerUpdateEvent event) {
                if (canSprint(event.getPlayer())) {
                    event.getPlayer().setSprinting(true);
                }
            }
        });
    }

    @Override
    protected void onDisable() {
        LocalPlayer player = mc.player;
        if (player != null) {
            player.setSprinting(false);
        }
    }

    private boolean canSprint(LocalPlayer player) {
        return (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown() || phobot.getPathfinder().isFollowingPath())
                && !(player.isCrouching() || player.horizontalCollision || player.getFoodData().getFoodLevel() <= 6f);
    }

}
