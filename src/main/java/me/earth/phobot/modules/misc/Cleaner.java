package me.earth.phobot.modules.misc;

import me.earth.phobot.Phobot;
import me.earth.phobot.modules.PhobotModule;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.loop.TickEvent;
import me.earth.pingbypass.api.module.impl.Categories;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

public class Cleaner extends PhobotModule {
    public Cleaner(Phobot phobot) {
        super(phobot, "Cleaner", Categories.MISC, "Throws away items if you have too many of them.");
        listen(new SafeListener<TickEvent>(mc) {
            @Override
            public void onEvent(TickEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (player.containerMenu != player.inventoryMenu) {
                    return;
                }

                phobot.getInventoryService().use(ctx -> {
                    // TODO: throw away items!
                });
            }
        });
    }

}
