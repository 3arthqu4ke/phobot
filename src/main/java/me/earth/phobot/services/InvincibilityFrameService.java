package me.earth.phobot.services;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.damagecalc.DamageCalculator;
import me.earth.phobot.modules.client.anticheat.AntiCheat;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.phobot.util.player.DamageCalculatorPlayer;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.event.network.PostListener;
import me.earth.pingbypass.api.event.network.ReceiveListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Computes the "invincibility frames".
 * After receiving damage in Minecraft no damage can be received for 10 ticks (500ms), except for damage higher than the damage already taken during such a frame.
 * In that case only the difference in damage will be applied.
 */
@Slf4j // TODO: make this better?
public class InvincibilityFrameService extends SubscriberImpl {
    public static final long FRAME_LENGTH_MS = 500L;

    private final Deque<ExplosionEntry> explosions = new ArrayDeque<>();
    private volatile InvincibilityFrame lastDamage = InvincibilityFrame.OLD;
    private volatile InvincibilityFrame previousFrame = InvincibilityFrame.OLD; // TODO: rather than previousFrame, maintain a list of frames and discard ones that are older than a second.
    private volatile InvincibilityFrame frame = InvincibilityFrame.OLD;
    private volatile float lastHealth = -1.0f;

    public InvincibilityFrameService(Minecraft mc, AntiCheat antiCheat, PlayerPositionService playerPositionService) {
        if (playerPositionService instanceof LocalPlayerPositionService) {
            listen(new PostListener.Safe<ClientboundRespawnPacket>(mc) {
                @Override
                public void onSafeEvent(PacketEvent.PostReceive<ClientboundRespawnPacket> event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                    lastHealth = EntityUtil.getHealth(player);
                    lastDamage = InvincibilityFrame.OLD;
                    previousFrame = InvincibilityFrame.OLD;
                    frame = InvincibilityFrame.OLD;
                }
            });
        }

        listen(new PostListener.Safe<ClientboundSetEntityDataPacket>(mc) {
            @Override
            public void onSafeEvent(PacketEvent.PostReceive<ClientboundSetEntityDataPacket> event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (event.getPacket().id() == getPlayer(player).getId()) {
                    calculate(getPlayer(player));
                }
            }
        });

        /*listen(new Listener<TickEvent>() {
            @Override
            public void onEvent(TickEvent tickEvent) {
                PingBypassApi.instances().forEach(instance -> {
                    instance.getChat().sendWithoutLogging(Component.literal("InvincibilityFrame " + getDamage(500) + " " + TimeUtil.getPassedTimeSince(frame.timeStamp) + "ms"), "InvincibilityFrame");
                });
            }
        });*/

        listen(new ReceiveListener.Scheduled.Safe<ClientboundExplodePacket>(mc) {
            @Override
            public void onSafeEvent(PacketEvent.Receive<ClientboundExplodePacket> event, LocalPlayer localPlayer, ClientLevel level, MultiPlayerGameMode gameMode) {
                AbstractClientPlayer player = getPlayer(localPlayer);
                ExplosionEntry entry = new ExplosionEntry(new ArrayList<>(), TimeUtil.getMillis());
                DamageCalculatorPlayer damageCalculatorPlayer = new DamageCalculatorPlayer(player);
                boolean noneChecked = true;
                DamageCalculator damageCalculator = antiCheat.getDamageCalculator();
                for (PlayerPosition playerPosition : playerPositionService.getPositions()) {
                    noneChecked = false;
                    if (TimeUtil.isTimeStampOlderThan(playerPosition.getTimeStamp(), 100L)) {
                        break; // only older positions will follow
                    } else {
                        playerPosition.applyTo(damageCalculatorPlayer);
                        entry.damages.add(damageCalculator.getDamage(damageCalculatorPlayer, level, event.getPacket().getPower(), event.getPacket().getX(), event.getPacket().getY(), event.getPacket().getZ()));
                    }
                }

                if (noneChecked) {
                    playerPositionService.getPosition().applyTo(damageCalculatorPlayer);
                    entry.damages.add(damageCalculator.getDamage(damageCalculatorPlayer, level, event.getPacket().getPower(), event.getPacket().getX(), event.getPacket().getY(), event.getPacket().getZ()));
                }

                if (entry.damages.stream().anyMatch(d -> d > 0.0f)) {
                    explosions.add(entry);
                }
            }
        });
    }

    public boolean willDamageKill(LocalPlayer player, float damage, long time) {
        return EntityUtil.getHealth(player) + getDamage(time) - damage <= 0;
    }

    public float getDamage(long time) {
        return TimeUtil.isTimeStampOlderThan(frame.timeStamp, time) ? 0.0f : frame.damage;
    }

    @Synchronized
    private void calculate(Player player) {
        if (EntityUtil.isDead(player)) {
            return;
        }

        float health = EntityUtil.getHealth(player);
        if (health != lastHealth) {
            if (health < lastHealth) {
                float damage = lastHealth - health;
                lastDamage = new InvincibilityFrame();
                lastDamage.damage += damage;
                if (TimeUtil.isTimeStampOlderThan(frame.timeStamp, 750L)) {
                    setFrame(new InvincibilityFrame());
                    frame.damage += damage;
                } else if (explosions.isEmpty()) {
                    if (TimeUtil.isTimeStampOlderThan(frame.timeStamp, 495L)) {
                        setFrame(new InvincibilityFrame());
                    }

                    frame.damage += damage;
                } else {
                    for (ExplosionEntry entry : explosions) {
                        computeExplosionEntry(player, damage, entry);
                    }
                }

                explosions.clear();
            }
        }

        lastHealth = health;
    }

    private void setFrame(InvincibilityFrame frame) {
        this.previousFrame = this.frame;
        this.frame = frame;
    }

    private void computeExplosionEntry(Player player, float damage, ExplosionEntry explosion) {
        boolean isOld = TimeUtil.isTimeStampOlderThan(explosion.timeStamp, 55L);
        float bestNonFrameDifference = Float.MAX_VALUE;
        float bestNonFrameMatch = -1.0f;

        float bestFrameDifference = Float.MAX_VALUE;
        float bestFrameMatch = -1.0f;

        boolean inFrameWithoutDamage = false;
        for (float explosionDamage : explosion.damages) {
            float difference = Math.abs(explosionDamage - damage);
            if (difference < bestNonFrameDifference) {
                bestNonFrameDifference = difference;
                bestNonFrameMatch = explosionDamage;
            }

            difference = explosionDamage - frame.damage;
            if (difference < 0.0f) {
                inFrameWithoutDamage = true;
            }

            difference = Math.abs(difference);
            if (difference < bestFrameDifference) {
                bestFrameDifference = difference;
                bestFrameMatch = explosionDamage;
            }

            difference = explosionDamage - previousFrame.damage;
            if (difference < 0.0f) {
                inFrameWithoutDamage = true;
            }
        }

        if (isOld) {
            if (!inFrameWithoutDamage) {
                log.warn("[" + player.getScoreboardName() + "] Explosion " + TimeUtil.getPassedTimeSince(explosion.timeStamp) + "ms ago was supposed to deal damage!");
            }
        } else {
            if (bestNonFrameMatch >= 0.0f && bestFrameMatch >= 0.0f) {
                if (bestNonFrameDifference < bestFrameDifference) {
                    if (TimeUtil.getPassedTimeSince(frame.timeStamp) < 400) {
                        log.warn("Creating new frame even though last frame was only " + TimeUtil.getPassedTimeSince(frame.timeStamp) + "ms ago!");
                    }

                    if (bestNonFrameDifference >= 1.0f) {
                        log.warn("BestNonFrameMatch was " + bestNonFrameDifference + " off for damage " + damage + ", predicted " + bestNonFrameMatch);
                    }

                    setFrame(new InvincibilityFrame());
                    frame.damage = damage;
                } else {
                    if (bestFrameDifference >= 1.0f) {
                        log.warn("BestFrameMatch was " + bestFrameDifference + " off for damage " + damage + ", predicted " + bestFrameMatch);
                    }

                    frame.damage += damage;
                }
            } else {
                log.error("No explosion damages recorded!");
                setFrame(new InvincibilityFrame());
            }
        }
    }

    // overridden by InvincibilityFrameService.forPlayer to create InvincibilityFrameServices for other players than the LocalPlayer
    protected AbstractClientPlayer getPlayer(LocalPlayer localPlayer) {
        return localPlayer;
    }

    // TODO: also configure the thresholds for the packets, for other players we can take 500ms, without any leniency
    @Deprecated // not to be used yet
    public static InvincibilityFrameService forPlayer(Minecraft mc, AntiCheat antiCheat, AbstractClientPlayer player) {
        // TODO: the PlayerPositionService also kinda has to track one packet into the future because we receive the explosion outside of ticks, but the entity movement during a tick
        PlayerPositionService playerPositionService = new OtherPlayerPositionService(player, 1);
        InvincibilityFrameService invincibilityFrameService = new InvincibilityFrameService(mc, antiCheat, playerPositionService) {
            @Override
            protected AbstractClientPlayer getPlayer(LocalPlayer localPlayer) {
                return player;
            }
        };

        playerPositionService.getListeners().forEach(invincibilityFrameService::listen);
        return invincibilityFrameService;
    }

    private record DamageEntry(float damage, long time) { }

    private record ExplosionEntry(List<Float> damages, long timeStamp) { }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class InvincibilityFrame {
        private static final InvincibilityFrame OLD = new InvincibilityFrame();
        private final long timeStamp;
        private float damage;

        public InvincibilityFrame() {
            this(TimeUtil.getMillis());
        }
    }

}
