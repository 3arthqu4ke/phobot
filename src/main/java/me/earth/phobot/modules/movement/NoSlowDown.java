package me.earth.phobot.modules.movement;

import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.modules.client.anticheat.MovementAntiCheat;
import me.earth.phobot.util.world.PredictionUtil;
import me.earth.pingbypass.api.event.CancellingListener;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.event.CancellableEvent;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;

@Getter
public class NoSlowDown extends PhobotModule {
    private final Setting<HandMode> offhand = constant("Offhand", HandMode.Use, "Offhand might be tricky, depending on the Grim version.");

    public NoSlowDown(Phobot phobot) {
        super(phobot, "NoSlowDown", Categories.MOVEMENT, "Prevents slowdown from using items.");
        // TODO: Grim Check, offhand is food/shield, but active item is main hand?
        listen(new CancellingListener<>(UseItemEvent.class));
        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (phobot.getAntiCheat().getMovement().getValue() == MovementAntiCheat.Grim) {
                    if (offhand.getValue() != HandMode.None && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
                        if (offhand.getValue() == HandMode.SetCarried) {
                            phobot.getInventoryService().use(ctx -> {
                                player.connection.send(new ServerboundSetCarriedItemPacket(player.getInventory().selected % 8 + 1));
                                player.connection.send(new ServerboundSetCarriedItemPacket(player.getInventory().selected));
                            });
                        } else if (offhand.getValue() == HandMode.Use) {
                            PredictionUtil.predict(player.clientLevel, i -> player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, i)));
                        }
                    } else if (player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                        PredictionUtil.predict(player.clientLevel, i -> player.connection.send(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, i)));
                    }
                }
            }
        });
    }

    public enum HandMode {
        None,
        Use,
        SetCarried
    }

    public static final class UseItemEvent extends CancellableEvent {}

}
