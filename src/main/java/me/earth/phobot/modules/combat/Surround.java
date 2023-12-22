package me.earth.phobot.modules.combat;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.ducks.IEntity;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.modules.BlockPlacingModule;
import me.earth.phobot.pathfinder.blocks.BlockPathfinder;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.commons.event.SafeListener;
import me.earth.pingbypass.commons.event.network.PostListener;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Slf4j
@Getter
public class Surround extends BlockPlacingModule implements FindsShortestPath {
    private final Setting<Boolean> packetRotations = bool("PacketRotations", false, "Sends additional packets to rotate, might lag back, but allows you to place more blocks in one tick.");
    private final Setting<Boolean> disable = bool("Disable", true, "Disables this module when you leave the position you enabled it on.");
    private final Setting<Boolean> antiFacePlace = bool("AntiFacePlace", false, "Places another layer of blocks to prevent faceplace.");
    private final Setting<Integer> maxHelping = number("Helping", 0, 0, 10, "Amount of additional helping blocks to place. Mainly for StrictDirection servers.");
    private final Setting<Boolean> swap = bool("Swap", false, "Uses swap switch to work around switch cooldown.");
    private final BlockPathfinder blockPathfinder = new BlockPathfinder();
    private volatile Set<BlockPos> currentPositions = new HashSet<>();
    private BlockPos startPos = BlockPos.ZERO;
    private double startY;

    public Surround(Phobot phobot) {
        super(phobot, phobot.getBlockPlacer(), "Surround", Categories.COMBAT, "Surrounds you with obsidian.", BlockPlacer.PRIORITY - 1);
        ResetUtil.disableOnRespawnAndWorldChange(this, mc);
        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (disable.getValue() && (!player.blockPosition().equals(startPos) || player.getY() > startY || player.getDeltaMovement().y > 0)) {
                    disable();
                }
            }
        });

        // TODO: on lagback!
        // TODO: we have yet to model this correctly, for the first crystal the blockstate might still be obby, then they break and spawn a new crystal
        listen(new PostListener.Safe.Direct<ClientboundAddEntityPacket>(99/*after AutoCrystal attacks*/, mc) {
            @Override
            public void onSafePacket(ClientboundAddEntityPacket packet, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (packet.getType().equals(EntityType.END_CRYSTAL)) {
                    Entity entity = level.getEntity(packet.getId());
                    if (entity instanceof IEntity endCrystal
                            && TimeUtil.isTimeStampOlderThan(endCrystal.phobot$getAttackTime(), 100)
                            && entity.getBoundingBox().distanceToSqr(player.getEyePosition()) < ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE
                            && mc.gameMode != null) {
                        for (BlockPos pos : currentPositions) {
                            if (entity.getBoundingBox().intersects(new AABB(pos))) {
                                updateOutsideOfTick(player, level, mc.gameMode);
                                return;
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onEnable() {
        if (mc.player != null) {
            this.startPos = mc.player.blockPosition();
            this.startY = mc.player.getY();
        }
    }

    @Override
    protected void updatePlacements(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        Block block = context.findBlock(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ENDER_CHEST);
        if (block == null) {
            return;
        }

        getAllSurroundingPositions(player, player, level, block, true);
    }

    @Override
    public boolean isUsingSetCarriedItem() {
        return !swap.getValue();
    }

    @Override
    public boolean isDamageAcceptable(Player player, float damage) {
        return super.isDamageAcceptable(player, damage) || player.isHolding(Items.TOTEM_OF_UNDYING); // we are going to pop anyways
    }

    @Override
    protected boolean isUsingPacketRotations() {
        return packetRotations.getValue();
    }

    public Set<BlockPos> getAllSurroundingPositions(Player surroundPlayer, LocalPlayer player, ClientLevel level, Block block, boolean place) {
        Set<Entity> checkedEntities = new HashSet<>();
        checkedEntities.add(surroundPlayer);

        Set<BlockPos> underPlayer = PositionUtil.getPositionsUnderEntity(surroundPlayer, 0.5);
        Set<BlockPos> positions = new LinkedHashSet<>(underPlayer);
        Set<BlockPos> allSurrounding = new HashSet<>(underPlayer);
        Set<BlockPos> allChecked = new HashSet<>();

        List<BlockPos> blockedUnderPlayer = new ArrayList<>(4);
        List<BlockPos> unblockedUnderPlayer = new ArrayList<>(4);
        for (BlockPos pos : underPlayer) {
            BlockPos surroundPos = pos.above();
            if (level.getBlockState(surroundPos).canBeReplaced()) {
                unblockedUnderPlayer.add(surroundPos);
            } else {
                blockedUnderPlayer.add(surroundPos);
            }
        }

        for (BlockPos pos : underPlayer) {
            if (underPlayer.size() > 1) { // prevents unnecessary extensions, 3arth4ck used "createBlocked" for this with lots of code
                BlockPos above = pos.above();
                BlockState state = level.getBlockState(above);
                if (state.canBeReplaced() && !isBlockedByEntity(above, block.defaultBlockState().getCollisionShape(level, above), player, level, e -> false, ((p,e) -> {/*do not set crystal*/}))) {
                    continue;
                } else if (blockedUnderPlayer.size() == 3 && !state.canBeReplaced() && !unblockedUnderPlayer.contains(above)) {
                    continue;
                }
            }

            extendAround(pos, positions, allSurrounding, allChecked, checkedEntities, player, level, block, true);
        }

        if (place) {
            List<BlockPos> failed = new ArrayList<>();
            for (BlockPos pos : positions) {
                if (!placePos(pos, block, player, level)) {
                    failed.add(pos);
                }
            }

            clearFailedPositions(failed, block, level, player);
            int size = failed.size();
            // TODO: this eats FPS (number looks high but Ticks becoming very long making the game feel laggy)!
            //  But low priority since situations where we really have to do this are rare, as it only happens on top of single blocks if we are scaffolded up high
            // brute force best paths for helping blocks, this is usually needed on strict direction servers if we want to surround on a single block
            if (size > 0 && maxHelping.getValue() > 0) {
                for (int i = 0; i < size; i++) {
                    var shortest = findShortestPath(player, level, failed, block);
                    if (shortest == null || shortest.getValue().isEmpty()) {
                        break;
                    }

                    for (int j = 1/*skip first, it should be the goal*/; j < shortest.getValue().size() - 1; j++) {
                        BlockPos pos = shortest.getValue().get(j);
                        placePos(pos, block, player, level);
                    }

                    clearFailedPositions(failed, block, level, player);
                    failed.remove(shortest.getKey());
                    if (failed.isEmpty()) {
                        break;
                    }
                }
            }

            this.currentPositions = allSurrounding;
        }

        return allSurrounding;
    }

    private void clearFailedPositions(List<BlockPos> failed, Block block, ClientLevel level, LocalPlayer player) {
        int size = failed.size();
        for (int i = 0; i < size; i++) {
            // retry, maybe the blocks we have placed helped
            int sizeBefore = failed.size();
            failed.removeIf(pos -> placePos(pos, block, player, level));
            if (failed.isEmpty() || failed.size() == sizeBefore) {
                break;
            }
        }
    }

    protected void extendAround(BlockPos pos, Set<BlockPos> positions, Set<BlockPos> allSurrounding, Set<BlockPos> allChecked, Set<Entity> checkedEntities,
                                LocalPlayer player, ClientLevel level, Block block, boolean addBelow) {
        if (!addSupportPosWithBedrockCheck(pos, level, allSurrounding, addBelow)) {
            for (Direction direction : PositionUtil.HORIZONTAL_DIRECTIONS) {
                placeSupporting(pos, positions, allSurrounding, allChecked, checkedEntities, player, level, block, addBelow, direction);
            }
        }
    }

    protected void placeSupporting(BlockPos pos, Set<BlockPos> positions, Set<BlockPos> allSurrounding, Set<BlockPos> allChecked, Set<Entity> checkedEntities,
                                   LocalPlayer player, ClientLevel level, Block block, boolean addBelow, @Nullable Direction direction) {
        BlockPos supportingPos = direction == null ? pos : pos.relative(direction);
        if (!allChecked.add(supportingPos)) {
            return;
        }

        if (addSupportPosWithBedrockCheck(supportingPos, level, allSurrounding, addBelow)) {
            return;
        }

        BlockPos surroundPos = supportingPos.above();
        boolean blocked = extendAroundSurroundPos(pos, surroundPos, positions, allSurrounding, allChecked, checkedEntities, player, level, block);
        if (addBelow && !level.getBlockState(surroundPos).is(Blocks.BEDROCK)) {
            positions.add(supportingPos);
            allSurrounding.add(supportingPos);
        }

        if (!blocked) {
            allSurrounding.add(surroundPos);
            positions.add(surroundPos);
        }

        if (antiFacePlace.getValue() && EntityUtil.getHealth(player) < 8.0f) {
            BlockPos antiFacePlacePos = surroundPos.above();
            if (level.getBlockState(antiFacePlacePos).canBeReplaced()) {
                if (!extendAroundSurroundPos(pos, antiFacePlacePos, positions, allSurrounding, allChecked, checkedEntities, player, level, block)) {
                    positions.add(antiFacePlacePos);
                }

                allSurrounding.add(antiFacePlacePos);
            }
        }
    }

    private boolean addSupportPosWithBedrockCheck(BlockPos supportPos, Level level, Set<BlockPos> allSurrounding, boolean addBelow) {
        BlockPos above = supportPos.above();
        BlockState state = level.getBlockState(above);
        if (!state.canBeReplaced()) {
            allSurrounding.add(above);
            if (addBelow && !state.is(Blocks.BEDROCK)) {
                allSurrounding.add(supportPos);
            }

            return true;
        }

        return false;
    }

    private boolean extendAroundSurroundPos(BlockPos pos, BlockPos surroundPos, Set<BlockPos> positions, Set<BlockPos> allSurrounding,
                                            Set<BlockPos> allChecked, Set<Entity> checkedEntities, LocalPlayer player, ClientLevel level, Block block) {
        MutableObject<Boolean> blocked = new MutableObject<>(false);
        isBlockedByEntity(surroundPos, /*TODO: collision shape varies on place*/block.defaultBlockState().getCollisionShape(level, surroundPos), player, level, entity -> {
            if (!(entity instanceof EndCrystal)) {
                blocked.setValue(true);
                if (checkedEntities.add(entity)) {
                    for (BlockPos entityBlockedPos : PositionUtil.getPositionsBlockedByEntityAtY(entity, pos.getY())) {
                        extendAround(entityBlockedPos, positions, allSurrounding, allChecked, checkedEntities, player, level, block, false);
                    }
                }
            }

            return true;
        }, (blockPlacer, endCrystal) -> {/*crystal will be set when we place*/});
        return blocked.getValue();
    }

}
