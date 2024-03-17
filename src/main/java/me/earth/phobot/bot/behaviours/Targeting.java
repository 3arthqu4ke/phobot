package me.earth.phobot.bot.behaviours;

import lombok.Getter;
import lombok.Setter;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.modules.combat.KillAura;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.phobot.util.player.FakePlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Finds targets for our bot.
 */
@Getter
@Setter
public class Targeting extends Behaviour {
    private Entity target;

    public Targeting(Bot bot) {
        super(bot, PRIORITY_TARGET);
        ResetUtil.onRespawnOrWorldChange(this, mc, () -> target = null);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (bot.getJumpDownFromSpawn().isAboveSpawn(player)) {
            return;
        }

        Entity lastTarget = target;
        if (isInvalid(lastTarget)) {
            target = null;
        } else {
            return;
        }

        Entity target = bot.getModules().getAutoCrystal().getTarget();
        if (isInvalid(target)) {
            KillAura.Target killAuraTarget = bot.getModules().getKillAura().getTarget();
            if (killAuraTarget != null && !isInvalid(killAuraTarget.entity())) {
                target = killAuraTarget.entity();
            } else {
                target = findTarget(player, level, true);
                if (target == null) {
                    target = findTarget(player, level, false);
                }
            }
        }

        this.target = target;
    }

    private @Nullable Entity findTarget(LocalPlayer localPlayer, ClientLevel level, boolean enemy) {
        Entity result = null;
        double closestDistance = Double.MAX_VALUE;
        for (Player entity : level.players()) {
            if (!(entity instanceof RemotePlayer || entity instanceof FakePlayer)
                    || pingBypass.getFriendManager().contains(entity.getUUID())
                    || enemy && !pingBypass.getEnemyManager().contains(entity.getUUID())
                    || entity.getY() > bot.getSpawnHeight().getValue() + 10.0/* some leniency if they jump*/) {
                continue;
            }
            // TODO: make this great
            double distance = localPlayer.distanceToSqr(entity);
            if (result == null || closestDistance > distance) {
                result = entity;
                closestDistance = distance;
            }
        }

        return result;
    }

    private boolean isInvalid(@Nullable Entity target) {
        return target == null || EntityUtil.isDeadOrRemoved(target);
    }

}
