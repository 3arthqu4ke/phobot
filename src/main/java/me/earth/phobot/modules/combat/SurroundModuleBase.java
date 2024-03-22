package me.earth.phobot.modules.combat;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.ducks.IEntity;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.modules.SwappingBlockPlacingModule;
import me.earth.phobot.pathfinder.blocks.BlockPathfinder;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.services.SurroundService;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.network.PostListener;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.api.traits.Nameable;
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

// TODO: does not work when we are inside a block!!
/**
 * Base class for modules that surround you with blocks.
 *
 * @see Surround
 * @see AntiFacePlace
 */
@Slf4j
@Getter
public class SurroundModuleBase extends SwappingBlockPlacingModule implements FindsShortestPath {
    private final Setting<Boolean> packetRotations = bool("PacketRotations", false, "Sends additional packets to rotate, might lag back, but allows you to place more blocks in one tick.");
    private final Setting<Integer> maxHelping = number("Helping", 0, 0, 10, "Amount of additional helping blocks to place. Mainly for StrictDirection servers.");
    private final Setting<Boolean> disable = bool("Disable", true, "Disables this module when you leave the position you enabled it on.");
    private final BlockPathfinder blockPathfinder = new BlockPathfinder();

    protected volatile Set<BlockPos> currentPositions = new HashSet<>();
    protected BlockPos startPos = BlockPos.ZERO;
    protected double startY;

    public SurroundModuleBase(Phobot phobot, BlockPlacer blockPlacer, String name, Nameable category, String description, int priority) {
        super(phobot, blockPlacer, name, category, description, priority);
        ResetUtil.disableOnRespawnAndWorldChange(this, mc);
        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc, 10_000) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                onMotionPlayerUpdate(player, level);
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
        LocalPlayer player = mc.player;
        if (player != null) {
            this.startPos = player.blockPosition();
            this.startY = player.getY();
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
    public boolean isDamageAcceptable(Player player, float damage) {
        return super.isDamageAcceptable(player, damage) || player.isHolding(Items.TOTEM_OF_UNDYING); // we are going to pop anyway
    }

    @Override
    protected boolean isUsingPacketRotations() {
        return packetRotations.getValue();
    }

    /**
     * Returns all positions around the given player that are needed for Surround and places them if configured.
     * WARNING: this also returns all supporting positions.
     * If you want to check if a player is surrounded see how {@link SurroundService#isSurrounded(Entity)} does it.
     *
     * @param surroundPlayer the player to get the surrounding blocks for.
     * @param placePlayer the player to use for placing. (rotation checks etc.)
     * @param level the world the players are in.
     * @param block the block to use, e.g. {@link Blocks#ENDER_CHEST} or {@link Blocks#OBSIDIAN}.
     * @param place whether to place the missing blocks or not.
     * @return all positions around us that are needed to surround the given player + supporting blocks.
     * @see #getAllSurroundingPositions(Entity, Player, ClientLevel, Block, boolean, PlaceFunction, boolean)
     */
    public Set<BlockPos> getAllSurroundingPositions(Entity surroundPlayer, Player placePlayer, ClientLevel level, Block block, boolean place) {
        return getAllSurroundingPositions(surroundPlayer, placePlayer, level, block, place, this::placePos, false);
    }

    /**
     * Returns all positions around the given player that are needed for Surround and places them if configured.
     * WARNING: this also returns all supporting positions.
     * If you want to check if a player is surrounded see how {@link SurroundService#isSurrounded(Entity)} does it.
     *
     * @param surroundPlayer the player to get the surrounding blocks for.
     * @param placePlayer the player to use for placing. (rotation checks etc.)
     * @param level the world the players are in.
     * @param block the block to use, e.g. {@link Blocks#ENDER_CHEST} or {@link Blocks#OBSIDIAN}.
     * @param place whether to place the missing blocks or not.
     * @param placeFunction the place function to use for placing, can be configured to redirect all placing.
     * @param returnAfterFirstPlace this functions returns after the first successful call to the given placeFunction.
     * @return all positions around us that are needed to surround the given player + supporting blocks.
     */
    protected Set<BlockPos> getAllSurroundingPositions(Entity surroundPlayer, Player placePlayer, ClientLevel level, Block block,
                                                       boolean place, PlaceFunction placeFunction, boolean returnAfterFirstPlace) {
        // Entities that have blocked our surround and that we have extended around
        Set<Entity> checkedEntities = new HashSet<>();
        checkedEntities.add(surroundPlayer);
        // The positions the player is standing on.
        // Basically the Surrounds extension works by attempting to build a 1x1 surround around each of these Blocks
        // If the player blocks multiple blocks we obviously cannot place on those and 1x1 surrounds form a bigger 2x1 or 2x2 surround.
        Set<BlockPos> underPlayer = PositionUtil.getPositionsUnderEntity(surroundPlayer, 0.5);
        // positions to place
        Set<BlockPos> positions = new LinkedHashSet<>(underPlayer);
        // positions to place + all the other positions that would make a surround
        Set<BlockPos> allSurrounding = new HashSet<>(underPlayer);
        // all positions that have been checked, so we do not check the same position twice
        Set<BlockPos> allChecked = new HashSet<>();
        // This is for this situation:
        //         1  0
        //         p  1
        // Where p is the player which is standing against the corner between 1 and 1,
        // there is one direction in which the players hitbox will also block 0.
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
            // see above blockedUnderPlayer prevents unnecessary extensions, 3arth4ck used "createBlocked" for this with lots of code
            if (underPlayer.size() > 1) {
                BlockPos above = pos.above();
                BlockState state = level.getBlockState(above);
                if (state.canBeReplaced()
                        && !isBlockedByEntity(above, block.defaultBlockState().getCollisionShape(level, above), placePlayer, level, e -> false, ((p,e) -> {/*do not set crystal*/}))) {
                    continue;
                } else if (blockedUnderPlayer.size() == 3 && !state.canBeReplaced() && !unblockedUnderPlayer.contains(above)) {
                    continue;
                }
            }

            extendAround(pos, positions, allSurrounding, allChecked, checkedEntities, placePlayer, level, block, true);
        }

        if (place) {
            // place positions that we can first
            List<BlockPos> failed = new ArrayList<>();
            for (BlockPos pos : positions) {
                if (!placeFunction.place(pos, block, placePlayer, level)) {
                    failed.add(pos);
                } else if (returnAfterFirstPlace) {
                    return allSurrounding;
                }
            }
            // this will reattempt to place the positions that failed, because the positions that we have placed successfully can help us with placing the failed ones.
            if (clearFailedPositions(failed, block, level, placePlayer, placeFunction, returnAfterFirstPlace)) {
                return allSurrounding;
            }

            int size = failed.size();
            // TODO: this eats FPS (number looks high but Ticks becoming very long making the game feel laggy)!
            //  But low priority since situations where we really have to do this are rare, as it only happens on top of single blocks if we are scaffolded up high
            // brute force best paths for helping blocks, this is usually needed on strict direction servers if we want to surround on a single block
            if (size > 0 && maxHelping.getValue() > 0) {
                for (int i = 0; i < size; i++) {
                    var shortest = findShortestPath(placePlayer, level, failed, block);
                    if (shortest == null || shortest.getValue().isEmpty()) {
                        break;
                    }

                    for (int j = 1/*skip first, it should be the goal*/; j < shortest.getValue().size() - 1; j++) {
                        BlockPos pos = shortest.getValue().get(j);
                        placeFunction.place(pos, block, placePlayer, level);
                    }

                    if (clearFailedPositions(failed, block, level, placePlayer, placeFunction, returnAfterFirstPlace)) {
                        return allSurrounding;
                    }

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

    // this will reattempt to place the positions that failed, because the positions that we have placed successfully can help us with placing the failed ones.
    protected boolean clearFailedPositions(List<BlockPos> failed, Block block, ClientLevel level, Player player, PlaceFunction placeFunction, boolean returnAfterFirstPlace) {
        int size = failed.size();
        for (int i = 0; i < size; i++) {
            // retry, maybe the blocks we have placed helped
            int sizeBefore = failed.size();
            if (failed.removeIf(pos -> placeFunction.place(pos, block, player, level)) && returnAfterFirstPlace) {
                return true;
            }

            if (failed.isEmpty() || failed.size() == sizeBefore) {
                break;
            }
        }

        return false;
    }

    // makes a 1x1 surround around the given position
    protected void extendAround(BlockPos pos, Set<BlockPos> positions, Set<BlockPos> allSurrounding, Set<BlockPos> allChecked, Set<Entity> checkedEntities,
                                Player placePlayer, ClientLevel level, Block block, boolean addBelow) {
        if (!addSupportPosWithBedrockCheck(pos, level, allSurrounding, addBelow)) {
            for (Direction direction : PositionUtil.HORIZONTAL_DIRECTIONS) {
                placeSupporting(pos, positions, allSurrounding, allChecked, checkedEntities, placePlayer, level, block, addBelow, direction);
            }
        }
    }

    // makes a single pilar for the 1x1 surround
    protected void placeSupporting(BlockPos pos, Set<BlockPos> positions, Set<BlockPos> allSurrounding, Set<BlockPos> allChecked, Set<Entity> checkedEntities,
                                   Player placePlayer, ClientLevel level, Block block, boolean addBelow, @Nullable Direction direction) {
        BlockPos supportingPos = direction == null ? pos : pos.relative(direction);
        if (!allChecked.add(supportingPos)) {
            return;
        }

        if (addSupportPosWithBedrockCheck(supportingPos, level, allSurrounding, addBelow)) {
            return;
        }

        BlockPos surroundPos = supportingPos.above();
        addSurroundPos(pos, positions, allSurrounding, allChecked, checkedEntities, placePlayer, level, block, surroundPos, addBelow, supportingPos);
    }

    // computes the actual surround pos at the top of the pillar next to our legs
    // also computes extending positions
    protected void addSurroundPos(BlockPos pos, Set<BlockPos> positions, Set<BlockPos> allSurrounding, Set<BlockPos> allChecked, Set<Entity> checkedEntities,
                                  Player placePlayer, ClientLevel level, Block block, BlockPos surroundPos, boolean addBelow, BlockPos supportingPos) {
        boolean blocked = extendAroundSurroundPos(pos, surroundPos, positions, allSurrounding, allChecked, checkedEntities, placePlayer, level, block);
        if (addBelow && !level.getBlockState(surroundPos).is(Blocks.BEDROCK)) {
            allSurrounding.add(supportingPos);
            addToPositions(positions, supportingPos);
        }

        if (!blocked) {
            allSurrounding.add(surroundPos);
            addToPositions(positions, surroundPos);
        }
    }

    // can be overridden by subclasses, like AntiFacePlace to regulate which positions we actually want to place
    protected void addToPositions(Set<BlockPos> positions, BlockPos pos) {
        positions.add(pos);
    }

    // checks if a position next to our legs is bedrock, in that case we do not need to place the supporting position underneath it
    // otherwise adds both positions. We always want the supportPos, as it prevents players from mining it, them mining our surround pos and slipping right in, blocking us.
    protected boolean addSupportPosWithBedrockCheck(BlockPos supportPos, Level level, Set<BlockPos> allSurrounding, boolean addBelow) {
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

    protected boolean extendAroundSurroundPos(BlockPos pos, BlockPos surroundPos, Set<BlockPos> positions, Set<BlockPos> allSurrounding,
                                              Set<BlockPos> allChecked, Set<Entity> checkedEntities, Player placePlayer, ClientLevel level, Block block) {
        MutableObject<Boolean> blocked = new MutableObject<>(false);
        isBlockedByEntity(surroundPos, /*TODO: collision shape varies on place*/block.defaultBlockState().getCollisionShape(level, surroundPos), placePlayer, level, entity -> {
            if (!(entity instanceof EndCrystal)) {
                blocked.setValue(true);
                if (checkedEntities.add(entity)) {
                    for (BlockPos entityBlockedPos : PositionUtil.getPositionsBlockedByEntityAtY(entity, pos.getY())) {
                        extendAround(entityBlockedPos, positions, allSurrounding, allChecked, checkedEntities, placePlayer, level, block, false);
                    }
                }
            }

            return true;
        }, (blockPlacer, endCrystal) -> {/*crystal will be set when we place*/});
        return blocked.getValue();
    }

    /**
     * Called on {@link PreMotionPlayerUpdateEvent} with priority 10000.
     * Used for the disable check, so if you override this method and want to do something after calling this, consider checking {@link #isEnabled()} before.
     *
     * @param player the player to check.
     * @param level the level to check.
     */
    protected void onMotionPlayerUpdate(LocalPlayer player, ClientLevel level) {
        if (disable.getValue() && (!player.blockPosition().equals(startPos) || player.getY() > startY || player.getDeltaMovement().y > 0)) {
            disable();
        }
    }

    @FunctionalInterface
    protected interface PlaceFunction {
        boolean place(BlockPos pos, Block block, Player player, ClientLevel level);
    }

}
