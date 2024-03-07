package me.earth.phobot.modules.misc;

import me.earth.phobot.Phobot;
import me.earth.phobot.modules.PhobotModule;
import me.earth.pingbypass.api.event.event.CancellableEvent;
import me.earth.pingbypass.api.event.gui.GuiScreenEvent;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.module.impl.Categories;
import net.minecraft.client.gui.screens.DeathScreen;

public class AutoRespawn extends PhobotModule {
    public AutoRespawn(Phobot phobot) {
        super(phobot, "AutoRespawn", Categories.MISC, "Automatically respawns you when you die.");
        listen(new Listener<GuiScreenEvent<DeathScreen>>() {
            @Override
            public void onEvent(GuiScreenEvent<DeathScreen> event) {
                respawn(event);
            }
        });

        listen(new Listener<DeathScreenEvent>() {
            @Override
            public void onEvent(DeathScreenEvent event) {
                respawn(event);
            }
        });
    }

    private void respawn(CancellableEvent event) {
        if (mc.player != null) {
            mc.player.respawn();
        }

        event.setCancelled(true);
    }

    public static final class DeathScreenEvent extends CancellableEvent { }

}
