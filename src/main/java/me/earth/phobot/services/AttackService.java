package me.earth.phobot.services;

import lombok.Getter;
import me.earth.phobot.ducks.IEntity;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.SafeListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;

@Getter
public class AttackService extends SubscriberImpl {
    private final Minecraft minecraft;
    private volatile boolean weakness;

    public AttackService(Minecraft mc) {
        this.minecraft = mc;
        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                weakness = player.hasEffect(MobEffects.WEAKNESS);
            }
        });
    }

    // TODO: blow up items
    // TODO: AntiWeakness
    public void attack(LocalPlayer player, Entity entity) {
        player.connection.send(ServerboundInteractPacket.createAttackPacket(entity, false));
        player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        ((IEntity) entity).phobot$setAttackTime(TimeUtil.getMillis());
        minecraft.submit(() -> player.swing(InteractionHand.MAIN_HAND, false));
    }

}
