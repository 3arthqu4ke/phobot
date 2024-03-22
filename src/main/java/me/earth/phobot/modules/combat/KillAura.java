package me.earth.phobot.modules.combat;

import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.phobot.ducks.ITotemPoppingEntity;
import me.earth.phobot.event.PostMotionPlayerUpdateEvent;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.services.PlayerPosition;
import me.earth.phobot.util.InventoryUtil;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.phobot.util.math.MathUtil;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.math.RaytraceUtil;
import me.earth.phobot.util.math.RotationUtil;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.gui.hud.DisplaysHudInfo;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

public class KillAura extends PhobotModule implements DisplaysHudInfo {
    private final Setting<Boolean> teleport = bool("Teleport", true, "Teleports a bit to hit players that are outside of your range.");
    private final Setting<Boolean> weapon = bool("Weapon", true, "Only attacks if you are holding a Sword or an Axe.");
    private final Setting<Boolean> players = bool("Players", true, "Targets players.");
    private final Setting<Boolean> entities = bool("Entities", true, "Targets entities.");
    private final Setting<Boolean> other = bool("Others", false, "Targets other entities.");
    @Getter
    private @Nullable Target target;
    private boolean attacked;

    public KillAura(Phobot phobot) {
        super(phobot, "KillAura", Categories.COMBAT, "Attacks players in range.");
        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                attacked = false;
                target = computeTarget(player, level);
                attack(target, player, gameMode, true);
            }
        });

        listen(new SafeListener<PostMotionPlayerUpdateEvent>(mc) {
            @Override
            public void onEvent(PostMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                attack(target, player, gameMode, false);
            }
        });

        ResetUtil.onRespawnOrWorldChange(this, mc, () -> target = null);
    }

    @Override
    public String getHudInfo() {
        // Target[entity=RemotePlayer['§9§r§9§d§e§5§n§1'/65, l='ClientLevel', x=-4.47, y=156.00, z=1.51], inRangeForCurrentPos=true, inRangeForLastPos=false, teleportPos=null]
        LocalPlayer player = mc.player;
        Target target = this.target;
        if (player != null && target != null && target.entity() != null) {
            return target.entity().getName().getString() + ", " + MathUtil.round(Math.sqrt(target.entity().getBoundingBox().distanceToSqr(player.getEyePosition())), 2);
        }

        return null;
    }

    @Override
    protected void onDisable() {
        target = null;
    }

    private void attack(@Nullable Target target, LocalPlayer player, MultiPlayerGameMode gameMode, boolean preMotion) {
        if (target == null
                || attacked
                || player.isSpectator()
                || player.getAttackStrengthScale(0.5f) < 1.0f
                || weapon.getValue() && !InventoryUtil.isHoldingWeapon(player)) {
            return;
        }

        // we have not yet send our position to the server
        if (preMotion) {
            // we can hit the target from the position we currently have on the server
            if (target.inRangeForLastPos()) {
                // our rotations are legit, we can attack
                if (!phobot.getAntiCheat().getAttackRotations().getValue() || RaytraceUtil.areRotationsLegit(phobot, target.entity)) {
                    executeAttack(target, player, gameMode);
                } else {
                    // rotate and wait for postMotion event
                    float[] rotations = RotationUtil.getRotations(player, target.entity);
                    phobot.getMotionUpdateService().rotate(player, rotations[0], rotations[1]);
                }
            // we have to wait for the PostMotionUpdateEvent
            } else {
                // target is not in range for the position we have on the server, we have to teleport
                if (!target.inRangeForCurrentPos() && target.teleportPos() != null) {
                    phobot.getMotionUpdateService().setPosition(player, target.teleportPos());
                }

                if (phobot.getAntiCheat().getAttackRotations().getValue() && !RaytraceUtil.areRotationsLegit(player, target.entity)) {
                    float[] rotations = RotationUtil.getRotations(player, target.entity);
                    phobot.getMotionUpdateService().rotate(player, rotations[0], rotations[1]);
                }
            }
        } else { // we are in the post motion update event and have sent our position to the server
            if ((!phobot.getAntiCheat().getAttackRotations().getValue() || RaytraceUtil.areRotationsLegit(phobot, target.entity))
                    && isInRange(target.entity, phobot.getLocalPlayerPositionService().getPosition(), player.getEyeHeight())) {
                executeAttack(target, player, gameMode);
            }
        }
    }

    private void executeAttack(Target target, LocalPlayer player, MultiPlayerGameMode gameMode) {
        gameMode.attack(player, target.entity());
        player.swing(InteractionHand.MAIN_HAND);
        attacked = true;
    }

    public @Nullable Target computeTarget(LocalPlayer player, ClientLevel level) {
        Target result = null;
        MovementPlayer teleportPlayer = null;
        for (Entity entity : level.getEntities(null, PositionUtil.getAABBOfRadius(player, 8.0))) {
            if (entity == null
                    || entity.isRemoved()
                    || entity.getId() == player.getId()
                    || !entity.isAttackable()
                    || entity instanceof EndCrystal
                    || entity instanceof ExperienceOrb
                    || entity instanceof AbstractArrow
                    || entity instanceof ItemEntity
                    || entity instanceof ThrownExperienceBottle
                    || player.getPassengers().contains(entity)
                    || entity.getPassengers().contains(player)
                    || entity instanceof Player && !players.getValue()
                    || entity instanceof LivingEntity && !(entity instanceof Player) && !entities.getValue()
                    || !(entity instanceof LivingEntity) && !other.getValue()
                    || entity instanceof Player && getPingBypass().getFriendManager().contains(entity.getUUID())
                    || !level.getWorldBorder().isWithinBounds(entity.blockPosition())) {
                continue;
            }

            Vec3 teleportPos = null;
            boolean inRangeForLastPos = false;
            boolean inRangeForCurrentPos = isInRange(entity, player.getEyePosition(), 0.0);
            if (!inRangeForCurrentPos) {
                inRangeForLastPos = isInRange(entity, phobot.getLocalPlayerPositionService().getPosition(), player.getEyeHeight());
                if (!inRangeForLastPos && teleport.getValue() && player.onGround()) {
                    PlayerPosition last = phobot.getLocalPlayerPositionService().getPosition();
                    Vec3 own = last.add(0.0, player.getEyeHeight(), 0.0);
                    Vec3 bbCords = PositionUtil.getBBCoords(entity.getBoundingBox(), own);
                    Vec3 vec = new Vec3(bbCords.x, own.y, bbCords.z);
                    /*
                        We want the t, which is 6 blocks from bbCords:

                         vec - b - t -------- own
                          .       /
                          a      c (6 blocks long)
                          .    /
                         bbCords
                     */
                    double cSq = Mth.square(6.0 - Shapes.EPSILON);
                    double aSq = vec.distanceToSqr(bbCords);
                    double b = Math.sqrt(cSq - aSq);
                    Vec3 t = vec.add(own.subtract(vec).normalize().scale(b));
                    teleportPos = t.subtract(0.0, player.getEyeHeight(), 0.0);
                    if (teleportPos.distanceToSqr(last) > Mth.square(phobot.getMovementService().getMovement().getSpeed(player))) {
                        continue;
                    }

                    if (teleportPlayer == null) {
                        teleportPlayer = new MovementPlayer(level);
                    }

                    teleportPlayer.setPos(last);
                    Vec3 movementVector = teleportPos.subtract(last);
                    teleportPlayer.setDeltaMovement(movementVector);
                    teleportPlayer.setOnGround(last.isOnGround());
                    teleportPlayer.move(MoverType.SELF, movementVector);
                    if (!teleportPlayer.position().equals(teleportPos)) {
                        continue;
                    }
                }
            }

            Target targetForEvaluation = new Target(entity, inRangeForCurrentPos, inRangeForLastPos, teleportPos);
            if (targetForEvaluation.isBetterThan(result, player, false)) {
                result = targetForEvaluation;
            }
        }

        return result;
    }

    private boolean isInRange(Entity entity, Vec3 position, double yOffset) {
        return entity.getBoundingBox().distanceToSqr(new Vec3(position.x, position.y + yOffset, position.z)) < ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE;
    }

    public record Target(Entity entity, boolean inRangeForCurrentPos, boolean inRangeForLastPos, @Nullable Vec3 teleportPos) {
        public boolean isBetterThan(@Nullable Target last, LocalPlayer player, boolean recursive) {
            if (last == null || entity instanceof Player && !(last.entity instanceof Player) || last.teleportPos != null && teleportPos == null/* we prefer to not teleport*/) {
                return true;
            }

            if (last.entity instanceof Player lastPlayer && entity instanceof Player currentPlayer) {
                float lastHealth = EntityUtil.getHealth(lastPlayer);
                float currentHealth = EntityUtil.getHealth(currentPlayer);
                boolean lastPoppedTotemRecently = !((ITotemPoppingEntity) lastPlayer).phobot$getLastTotemPop().passed(2000L);
                boolean currentPoppedTotemRecently = !((ITotemPoppingEntity) currentPlayer).phobot$getLastTotemPop().passed(2000L);
                if (currentHealth <= 4.0f && currentHealth < lastHealth) {
                    return true;
                }

                if (currentHealth < lastHealth && currentPoppedTotemRecently && !lastPoppedTotemRecently) {
                    return true;
                }

                float durability = getLowestDurability(lastPlayer);
                float lastDurability = getLowestDurability(currentPlayer);
                if (durability <= 0.3f && durability < lastDurability) {
                    return true;
                }
            }

            if (!recursive && last.isBetterThan(this, player, true)) {
                return false;
            }

            return entity.distanceToSqr(player) < last.entity.distanceToSqr(player);
        }

        private float getLowestDurability(Player player) {
            float lowest = 1.0f;
            for (ItemStack stack : player.getInventory().armor) {
                if (!stack.isEmpty() && stack.getMaxDamage() > 0 && stack.getDamageValue() > 0) {
                    float durability = (float) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
                    if (durability < lowest) {
                        lowest = durability;
                    }
                }
            }

            return lowest;
        }
    }

}
