package me.earth.phobot.bot.behaviours;

import lombok.Getter;
import lombok.Setter;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.modules.combat.KillAura;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.phobot.util.player.FakePlayer;
import me.earth.phobot.util.time.TimeUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds targets for our bot.
 */
@Getter
@Setter
public class Targeting extends Behaviour {
    private final Map<Integer, Long> invalidTargets = new ConcurrentHashMap<>();
    private Entity target;

    public Targeting(Bot bot) {
        super(bot, PRIORITY_TARGET);
        ResetUtil.onRespawnOrWorldChange(this, mc, () -> {
            target = null;
            invalidTargets.clear();
        });
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        invalidTargets.entrySet().removeIf(entry -> TimeUtil.isTimeStampOlderThan(entry.getValue(), 1000L));
        if (bot.getJumpDownFromSpawn().isAboveSpawn(player)) {
            return;
        }

        Entity lastTarget = target;
        if (isInvalid(lastTarget)) {
            target = null;
        } else {
            return;
        }

        findNextTarget(player, level);
        if (target == null && !invalidTargets.isEmpty()) {
            invalidTargets.clear();
            findNextTarget(player, level);
        }
    }

    private void findNextTarget(Player player, Level level) {
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

    private @Nullable Entity findTarget(Player localPlayer, Level level, boolean enemy) {
        Entity result = null;
        double closestDistance = Double.MAX_VALUE;
        for (Player entity : level.players()) {
            if (isInvalid(entity) || enemy && !pingBypass.getEnemyManager().contains(entity.getUUID())) {
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

    public boolean isInvalid(@Nullable Entity target) {
        return target == null
                || EntityUtil.isDeadOrRemoved(target)
                || invalidTargets.containsKey(target.getId())
                || !(target instanceof RemotePlayer || target instanceof FakePlayer)
                || pingBypass.getFriendManager().contains(target.getUUID())
                || target.getY() > bot.getSpawnHeight().getValue() + 10.0/* some leniency if they jump*/;
    }

}
