package me.earth.phobot.modules.movement;

import me.earth.phobot.Phobot;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.modules.client.anticheat.MovementAntiCheat;
import me.earth.phobot.util.world.PredictionUtil;
import me.earth.pingbypass.api.event.CancellingListener;
import me.earth.pingbypass.api.event.event.CancellableEvent;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.loop.LocalPlayerUpdateEvent;
import me.earth.pingbypass.api.module.impl.Categories;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;

public class NoSlowDown extends PhobotModule {
    public NoSlowDown(Phobot phobot) {
        super(phobot, "NoSlowDown", Categories.MOVEMENT, "Prevents slowdown from using items.");
        // TODO: Grim Check, offhand is food/shield, but active item is main hand?
        listen(new CancellingListener<>(UseItemEvent.class));
        listen(new Listener<LocalPlayerUpdateEvent>() {
            @Override
            public void onEvent(LocalPlayerUpdateEvent event) {
                if (phobot.getAntiCheat().getMovement().getValue() == MovementAntiCheat.Grim) {
                    LocalPlayer player = event.getPlayer();
                    if (player.getUsedItemHand() == InteractionHand.OFF_HAND) {
                        phobot.getInventoryService().use(ctx -> {
                            player.connection.send(new ServerboundSetCarriedItemPacket(player.getInventory().selected % 8 + 1));
                            player.connection.send(new ServerboundSetCarriedItemPacket(player.getInventory().selected));
                        });
                    } else {
                        PredictionUtil.predict(player.clientLevel, i -> player.connection.send(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, i)));
                    }
                }
            }
        });
    }

    public static final class UseItemEvent extends CancellableEvent {}

}
