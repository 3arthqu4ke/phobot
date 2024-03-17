package me.earth.phobot.services;

import lombok.Getter;
import me.earth.phobot.ducks.IEntity;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.modules.combat.AntiWeakness;
import me.earth.phobot.services.inventory.InventoryService;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.SubscriberImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.SwordItem;

import static me.earth.phobot.services.inventory.InventoryContext.*;

@Getter
public class AttackService extends SubscriberImpl {
    private final InventoryService inventoryService;
    private final AntiWeakness antiWeakness;
    private final Minecraft minecraft;
    private volatile boolean weakness;

    public AttackService(Minecraft mc, AntiWeakness antiWeakness, InventoryService inventoryService) {
        this.inventoryService = inventoryService;
        this.antiWeakness = antiWeakness;
        this.minecraft = mc;
        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                weakness = player.hasEffect(MobEffects.WEAKNESS);
            }
        });
    }

    // TODO: blow up items
    public void attack(LocalPlayer player, Entity entity) {
        if (entity instanceof EndCrystal && weakness && antiWeakness.isEnabled()) {
            inventoryService.use(ctx -> {
                int flags = PREFER_MAINHAND | SKIP_OFFHAND | SWITCH_BACK;
                if (antiWeakness.getMode().getValue() == AntiWeakness.Mode.Switch) {
                    flags |= SET_CARRIED_ITEM;
                }

                ctx.switchTo(stack -> stack.getItem() instanceof SwordItem, flags);
                attackWithoutAntiWeakness(player, entity);
            });
        } else {
            attackWithoutAntiWeakness(player, entity);
        }
    }

    public void attackWithoutAntiWeakness(LocalPlayer player, Entity entity) {
        player.connection.send(ServerboundInteractPacket.createAttackPacket(entity, false));
        player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        ((IEntity) entity).phobot$setAttackTime(TimeUtil.getMillis());
        minecraft.submit(() -> player.swing(InteractionHand.MAIN_HAND, false));
    }

}
