package me.earth.phobot.modules.movement;

import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.module.impl.ModuleImpl;
import me.earth.pingbypass.api.event.loop.LocalPlayerUpdateEvent;
import net.minecraft.client.player.LocalPlayer;

public class Sprint extends ModuleImpl {
    public Sprint(PingBypass pingBypass) {
        super(pingBypass, "Sprint", Categories.MOVEMENT, "Makes you always sprint.");
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
        return (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown())
                && !(player.isCrouching() || player.horizontalCollision || player.getFoodData().getFoodLevel() <= 6f);
    }

}
