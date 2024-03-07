package me.earth.phobot.bot.behaviours;

import lombok.Getter;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.modules.combat.KillAura;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.entity.EntityUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Finds targets for our bot.
 */
@Getter
public class Targeting extends Behaviour {
    private Entity target;
    private Entity lastTarget;

    public Targeting(Bot bot) {
        super(bot, PRIORITY_TARGET);
        ResetUtil.onRespawnOrWorldChange(this, mc, () -> {
            target = null;
            lastTarget = null;
        });
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (bot.getJumpDownFromSpawn().isAboveSpawn(player)) {
            return;
        }

        lastTarget = target;
        if (lastTarget != null && lastTarget.isRemoved() || lastTarget instanceof LivingEntity livingEntity &&  EntityUtil.isDead(livingEntity)) {
            lastTarget = null;
        }

        Entity target = bot.getModules().getAutoCrystal().getTarget();
        if (target == null) {
            KillAura.Target killAuraTarget = bot.getModules().getKillAura().getTarget();
            if (killAuraTarget != null) {
                target = killAuraTarget.entity();
            } else {
                target = lastTarget;
                if (target == null) {
                    target = findTarget(player, level, true);
                    if (target == null) {
                        target = findTarget(player, level, false);
                    }
                }
            }
        }

        this.target = target;
    }

    private @Nullable Entity findTarget(LocalPlayer player, ClientLevel level, boolean enemy) {
        Entity result = null;
        double closestDistance = Double.MAX_VALUE;
        for (Player entity : level.players()) {
        if (!(entity instanceof RemotePlayer)
                || player == entity
                || pingBypass.getFriendManager().contains(player.getUUID())
                || enemy && !pingBypass.getEnemyManager().contains(player.getUUID())
                || entity.getY() > bot.getSpawnHeight().getValue() + 10.0/* some leniency if they jump*/) {
                continue;
            }
            // TODO: make this great
            double distance = player.distanceToSqr(entity);
            if (result == null || closestDistance > distance) {
                result = entity;
                closestDistance = distance;
            }
        }

        return result;
    }

}
