package me.earth.phobot.modules.combat;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.damagecalc.CrystalPosition;
import me.earth.phobot.ducks.IEntity;
import me.earth.phobot.modules.BlockPlacingModule;
import me.earth.phobot.modules.combat.autocrystal.AutoCrystal;
import me.earth.phobot.modules.combat.autocrystal.CrystalPlacingModule;
import me.earth.phobot.modules.misc.Speedmine;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.mutables.MutPos;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.phobot.util.world.BlockStateLevel;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.commons.event.SafeListener;
import me.earth.pingbypass.commons.event.network.PacketEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

// TODO: stop bombing ourselves!
// TODO: Anvil Bomber
// TODO: distance to target player!!!
// TODO: Spam crystals against surround without AutoCrystal calculation
@Slf4j
public class Bomber extends BlockPlacingModule {
    private final PositionsThatBlowUpDrops positions = new PositionsThatBlowUpDrops();
    @Getter
    private final AutoCrystal autoCrystal;
    private final Speedmine speedmine;
    private final AutoTrap autoTrap;

    private volatile BlockPos currentSpeedminePos;
    private volatile BlockPos currentCrystalPos;
    private volatile long time;

    public Bomber(Phobot phobot, Speedmine speedmine, AutoCrystal autoCrystal, AutoTrap autoTrap) {
        super(phobot, phobot.getBlockPlacer(), "Bomber", Categories.COMBAT, "Place End Crystals when mining blocks.", autoCrystal.getPriority());
        this.autoCrystal = autoCrystal;
        this.speedmine = speedmine;
        this.autoTrap = autoTrap;
        listen(new Listener<Speedmine.BreakEvent>() {
            @Override
            public void onEvent(Speedmine.BreakEvent event) {
                if (currentCrystalPos != null) {
                    if (TimeUtil.isTimeStampOlderThan(time, 150L)) {
                        reset();
                    } else {
                        event.setCancelled(true);
                    }

                    return;
                }

                BlockStateLevel.Delegating level = new BlockStateLevel.Delegating(event.getLevel());
                CrystalPosition crystal = findCrystalThatBlowsUpDrop(positions, event.getPos(), level, event.getPlayer());
                Entity crystalEntity;
                if (crystal != null && (crystalEntity = crystal.getCrystals()[0]) != null) {
                    phobot.getInventoryService().use(context -> {
                        if (breakBlock(context, event.getPos(), event.getPlayer(), event.getLevel(), event.getGameMode())) {
                            phobot.getAttackService().attack(event.getPlayer(), crystalEntity);
                            CrystalPosition surroundBlockingPos = findPositionToBlockSurround(positions, event.getPos(), level, event.getPlayer());
                            if (surroundBlockingPos != null) {
                                autoCrystal.placer().place(event.getPlayer(), level, surroundBlockingPos);
                            }
                        }
                    });

                    event.setCancelled(true);
                } else {
                    CrystalPosition blowsUpDrop = findBestPositionToBlowUpDrop(positions, event.getPos(), level, event.getPlayer());
                    CrystalPosition blocksSurround = findPositionToBlockSurround(positions, event.getPos(), level, event.getPlayer());
                    if (isValid(blowsUpDrop, blocksSurround, null) && blowsUpDrop != null) {
                        currentCrystalPos = blowsUpDrop.immutable();
                        currentSpeedminePos = event.getPos();
                        time = TimeUtil.getMillis();
                        autoCrystal.placer().place(event.getPlayer(), level, blowsUpDrop);
                        autoCrystal.blockBlackList().put(blowsUpDrop.immutable(), TimeUtil.getMillis());
                        event.setCancelled(true);
                    }
                }
            }
        });

        listen(new SafeListener<PacketEvent.Receive<ClientboundAddEntityPacket>>(mc) {
            @Override
            public void onEvent(PacketEvent.Receive<ClientboundAddEntityPacket> event, LocalPlayer player, ClientLevel clientLevel, MultiPlayerGameMode gameMode) {
                try {
                    ClientboundAddEntityPacket packet = event.getPacket();
                    if (!packet.getType().equals(EntityType.END_CRYSTAL)) {
                        return;
                    }

                    ClientLevel level = phobot.getThreadSafeLevelService().getLevel();
                    EndCrystal entity = new EndCrystal(level, event.getPacket().getX(), event.getPacket().getY(), event.getPacket().getZ());
                    if (Objects.equals(entity.blockPosition().below(), currentCrystalPos)) {
                        if (currentSpeedminePos == null || !Objects.equals(currentSpeedminePos, speedmine.getCurrentPos())) {
                            reset();
                            return;
                        }

                        if (entity.getBoundingBox().distanceToSqr(player.getEyePosition()) >= ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE) {
                            reset();
                            return;
                        }

                        entity.setId(event.getPacket().getId());
                        phobot.getInventoryService().use(context -> {
                            breakBlock(context, currentSpeedminePos, player, level, gameMode);
                            phobot.getAttackService().attack(player, entity);
                            var customBlockStateLevel = new BlockStateLevel.Delegating(level);
                            CrystalPosition surroundBlockingPos = findPositionToBlockSurround(positions, currentSpeedminePos, customBlockStateLevel, player);
                            if (surroundBlockingPos != null && surroundBlockingPos.getDamage() >= autoCrystal.minDamage().getValue()) {
                                autoCrystal.placer().place(player, level, surroundBlockingPos);
                            } else {
                                // TODO: could update AutoTrap here?
                                autoTrap.getBlackList().remove(currentSpeedminePos);
                            }
                        });

                        reset();
                    }
                } catch (Exception e) {
                    log.error("Exception occurred during ClientboundAddEntityPacket receive", e);
                }
            }
        });

        ResetUtil.onRespawnOrWorldChange(this, mc, this::reset);
    }

    @Override
    protected void updatePlacements(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (!autoCrystal.isEnabled() && autoCrystal.obbyPos() != null) {
            autoCrystal.updatePlacements(context, player, level, gameMode);
        }
    }

    public void reset() {
        currentSpeedminePos = null;
        currentCrystalPos = null;
        time = 0L;
    }

    private boolean breakBlock(InventoryContext context, BlockPos pos, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        autoTrap.getBlackList().put(pos, TimeUtil.getMillis());
        return speedmine.breakCurrentPos(context, level, player, gameMode, false);
    }

    public boolean isValid(@Nullable CrystalPosition blowsUpDrop, @Nullable CrystalPosition blocksSurround, @Nullable CrystalPosition crystal) {
        if (blocksSurround == null) {
            if (crystal == null) {
                if (blowsUpDrop == null) {
                    return false;
                }

                return blowsUpDrop.getDamage() >= autoCrystal.minDamage().getValue();
            }

            return crystal.getDamage() >= autoCrystal.minDamage().getValue() || blowsUpDrop != null && blowsUpDrop.getDamage() >= autoCrystal.minDamage().getValue();
        }

        // TODO: also check ratio
        return blocksSurround.getDamage() >= autoCrystal.minDamage().getValue() || blowsUpDrop != null && blowsUpDrop.getDamage() >= autoCrystal.minDamage().getValue();
    }

    public @Nullable CrystalPosition findCrystalThatBlowsUpDrop(PositionsThatBlowUpDrops positions, BlockPos pos, BlockStateLevel.Delegating level, LocalPlayer player) {
        level.getMap().clear();
        level.getMap().put(pos, Blocks.AIR.defaultBlockState());
        Result result = new Result();
        iterateOverBomberPositions(positions, pos, level, crystalPos -> {
            for (EndCrystal crystal : level.getEntities(EntityType.END_CRYSTAL, new AABB(crystalPos), endCrystal -> crystalPos.equals(endCrystal.blockPosition()))) {
                if (((IEntity) crystal).phobot$GetTimeSinceAttack() < 100L || !EntityUtil.isInAttackRange(player, crystal)) {
                    continue;
                }

                crystalPos.reset();
                crystalPos.setValid(true);
                crystalPos.getCrystals()[0] = crystal;
                crystalPos.incrementY(-1); // we calculate damage from below
                calculateDamage(crystalPos, result, player, level);
            }
        }, PositionUtil.DIRECTIONS.clone());
        return result.getBestPos();
    }

    public @Nullable CrystalPosition findPositionToBlockSurround(PositionsThatBlowUpDrops positions, BlockPos pos, BlockStateLevel.Delegating level, LocalPlayer player) {
        level.getMap().clear();
        level.getMap().put(pos, Blocks.AIR.defaultBlockState());
        Result result = new Result();
        for (CrystalPosition crystalPosition : positions.blockingPositions) {
            crystalPosition.computeValidity(phobot.getAntiCheat(), level, player, EntityUtil.RANGE, pos, 0);
            if (crystalPosition.isValid()) {
                calculateDamage(crystalPosition, result, player, level);
            }
        }

        return CrystalPosition.copy(result.getBestPos());
    }

    public @Nullable CrystalPosition findBestPositionToBlowUpDrop(PositionsThatBlowUpDrops positions, BlockPos pos, BlockStateLevel.Delegating level, LocalPlayer player) {
        Result result = new Result();
        level.getMap().clear();
        iterateOverBomberPositions(positions, pos, level, position -> {
            if (position != positions.center) {
                level.getMap().put(pos, Blocks.AIR.defaultBlockState());
            }

            position.computeValidity(phobot.getAntiCheat(), level, player, EntityUtil.RANGE, pos.getX(), pos.getY() - (position == positions.getCenter() ? 0 : 1), pos.getZ(), 0);
            if (position.isValid()) {
                if (position.isObsidian()) {
                    position.computeEntityValidity(level, phobot.getAntiCheat().isCC(), 0);
                    if (!position.isClearOfEntities()) {
                        return;
                    }
                }

                if (position == positions.center) {
                    level.getMap().put(position, Blocks.AIR.defaultBlockState());
                } else {
                    level.getMap().put(position, Blocks.OBSIDIAN.defaultBlockState());
                }

                calculateDamage(position, result, player, level);
                level.getMap().remove(position);
            }
        }, Direction.NORTH, Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.DOWN/*, no Direction.UP*/);
        return CrystalPosition.copy(result.getBestPos());
    }

    private void iterateOverBomberPositions(PositionsThatBlowUpDrops positions, BlockPos pos, ClientLevel level, Consumer<CrystalPosition> action, Direction... directions) {
        positions.center.setWithCrystalPositionOffset(pos);
        action.accept(positions.center);
        for (int i = 0; i < positions.directionPositions[0].length; i++) { // go out in "circles", so that we check the ones closest first
            for (int j = 0; j < directions.length; j++) {
                Direction direction = directions[j];
                if (direction == null) {
                    continue;
                }

                CrystalPosition crystalPos = positions.directionPositions[direction.ordinal()][i];
                crystalPos.setWithCrystalPositionOffset(pos);
                if (!level.isEmptyBlock(crystalPos)) {
                    directions[j] = null;
                    continue; // continue in another direction, further crystals will be blocked by this block
                }

                action.accept(crystalPos);
            }
        }
    }

    private void calculateDamage(CrystalPosition position, Result result, LocalPlayer player, BlockStateLevel.Delegating level) {
        MutableObject<CrystalPosition> reference = result.getPositionToCompareTo(position);
        float selfDamage = phobot.getAntiCheat().getDamageCalculator().getDamage(player, level, position);
        position.setSelfDamage(selfDamage);
        if (selfDamage < EntityUtil.getHealth(player)) {
            if (reference.getValue() == null) {
                reference.setValue(position);
            }

            for (Player enemy : level.players()) {
                if (EntityUtil.isEnemyInRange(getPingBypass(), player, enemy, EntityUtil.RANGE + CrystalPlacingModule.CRYSTAL_RADIUS)) {
                    float damage = phobot.getAntiCheat().getDamageCalculator().getDamage(enemy, level, position);
                    if (damage >= EntityUtil.getHealth(enemy)) {
                        position.setKilling(true);
                    }

                    position.setDamageIfHigher(damage);
                }
            }

            evaluate(reference, position);
        }
    }

    public void evaluate(MutableObject<CrystalPosition> reference, CrystalPosition position) {
        if (reference.getValue() == null
                || position.getSelfDamage() < reference.getValue().getSelfDamage() && position.getDamage() > reference.getValue().getDamage()
                || position.isKilling() && !reference.getValue().isKilling()
                || position.getDamage() >= autoCrystal.minDamage().getValue()
                    && position.getRatio(autoCrystal.balance().getValue()) > reference.getValue().getRatio(autoCrystal.balance().getValue())) {
            reference.setValue(position);
        }
    }

    public static class Result {
        private final MutableObject<CrystalPosition> position = new MutableObject<>();
        private final MutableObject<CrystalPosition> blockedByCrystalPosition = new MutableObject<>();
        private final MutableObject<CrystalPosition> obbyPosition = new MutableObject<>();
        private final MutableObject<CrystalPosition> blockedByCrystalObbyPosition = new MutableObject<>();

        public MutableObject<CrystalPosition> getPositionToCompareTo(CrystalPosition crystalPosition) {
            if (crystalPosition.isObsidian()) {
                if (crystalPosition.isBlockedByCrystal()) {
                    return blockedByCrystalObbyPosition;
                }

                return obbyPosition;
            } else if (crystalPosition.isBlockedByCrystal()) {
                return blockedByCrystalPosition;
            }

            return position;
        }

        public @Nullable CrystalPosition getBestPos() {
            return position.getValue() != null
                    ? position.getValue()
                    : (blockedByCrystalPosition.getValue() != null
                        ? blockedByCrystalPosition.getValue()
                        : (obbyPosition.getValue() != null
                            ? obbyPosition.getValue()
                            : (blockedByCrystalObbyPosition.getValue() != null
                                ? blockedByCrystalObbyPosition.getValue()
                                : null)));
        }
    }

    @Getter
    public static class PositionsThatBlowUpDrops {
        private static final BlockPos[] BLOCKING_POSITIONS = new BlockPos[]{new BlockPos(0, -1, 0), new BlockPos(1, -1, 0), new BlockPos(-1, -1, 0), new BlockPos(0, -1, -1), new BlockPos(0, -1, 1)};
        private final CrystalPosition[] blockingPositions = new CrystalPosition[BLOCKING_POSITIONS.length];
        private final CrystalPosition[][] directionPositions = new CrystalPosition[PositionUtil.DIRECTIONS.length][];
        private final CrystalPosition center = new CrystalPosition(BlockPos.ZERO);
        private final MutPos computePos = new MutPos();

        public PositionsThatBlowUpDrops() {
            // TODO: its also possible that a position that blows up the dropped item is on a diagonal,
            //  but its more random since we cant know where the item will spawn and we have to deal 5 hp damage to the item.
            for (Direction direction : PositionUtil.DIRECTIONS) {
                directionPositions[direction.ordinal()] = new CrystalPosition[11];
                for (int j = 1; j < 12; j++) {
                    directionPositions[direction.ordinal()][j - 1] = new CrystalPosition(new BlockPos(direction.getStepX() * j, direction.getStepY() * j, direction.getStepZ() * j));
                }
            }

            for (int i = 0; i < BLOCKING_POSITIONS.length; i++) {
                blockingPositions[i] = new CrystalPosition(BLOCKING_POSITIONS[i]);
            }
        }
    }

}
