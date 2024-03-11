package me.earth.phobot.modules.combat;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.ducks.IEntity;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.modules.BlockPlacingModule;
import me.earth.phobot.modules.ChecksBlockPlacingValidity;
import me.earth.phobot.pathfinder.blocks.BlockPathfinder;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.math.RotationUtil;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.phobot.util.time.StopWatch;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.network.PostListener;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

import static me.earth.phobot.util.math.MathUtil.distance2dSq;

// TODO: does not work when we are inside a block!!
@Slf4j
@Getter
public class Surround extends BlockPlacingModule implements FindsShortestPath, BlockPlacer.ActionListener {
    private final Setting<Boolean> packetRotations = bool("PacketRotations", false, "Sends additional packets to rotate, might lag back, but allows you to place more blocks in one tick.");
    private final Setting<Integer> maxHelping = number("Helping", 0, 0, 10, "Amount of additional helping blocks to place. Mainly for StrictDirection servers.");
    private final Setting<Boolean> disable = bool("Disable", true, "Disables this module when you leave the position you enabled it on.");
    private final Setting<Boolean> antiFacePlace = bool("AntiFacePlace", false, "Places another layer of blocks to prevent faceplace.");
    private final Setting<Integer> swap = number("Swap", -1, -1, 2000, "Delay between swap (packet) switches in ms. Off if -1.");
    private final Setting<Boolean> center = bool("Center", false, "Centers you in the middle of the block you are standing on if necessary.");
    private final StopWatch.ForMultipleThreads swapTimer = new StopWatch.ForMultipleThreads();
    private final BlockPathfinder blockPathfinder = new BlockPathfinder();
    private volatile Set<BlockPos> currentPositions = new HashSet<>();
    private BlockPos startPos = BlockPos.ZERO;
    private boolean centered;
    private double startY;

    public Surround(Phobot phobot) {
        super(phobot, phobot.getBlockPlacer(), "Surround", Categories.COMBAT, "Surrounds you with obsidian.", BlockPlacer.PRIORITY - 1);
        ResetUtil.disableOnRespawnAndWorldChange(this, mc);
        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc, 10_000) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (disable.getValue() && (!player.blockPosition().equals(startPos) || player.getY() > startY || player.getDeltaMovement().y > 0)) {
                    disable();
                    return;
                }

                if (!centered && center.getValue()) {
                    center(player, level);
                    centered = true;
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
    public void onActionExecutedSuccessfully(BlockPlacer.Action action) {
        if (!action.isUsingSetCarried()) {
            swapTimer.reset();
        }
    }

    @Override
    protected void onEnable() {
        centered = false;
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
    public boolean isUsingSetCarriedItem() {
        int swap = this.swap.getValue();
        return swap == -1 || swap != 0 && !swapTimer.passed(swap);
    }

    @Override
    public boolean isDamageAcceptable(Player player, float damage) {
        return super.isDamageAcceptable(player, damage) || player.isHolding(Items.TOTEM_OF_UNDYING); // we are going to pop anyways
    }

    @Override
    protected boolean isUsingPacketRotations() {
        return packetRotations.getValue();
    }

    public Set<BlockPos> getAllSurroundingPositions(Player surroundPlayer, Player player, ClientLevel level, Block block, boolean place) {
        return getAllSurroundingPositions(surroundPlayer, player, level, block, place, this::placePos, false);
    }

    private Set<BlockPos> getAllSurroundingPositions(Player surroundPlayer, Player player, ClientLevel level, Block block,
                                                     boolean place, PlaceFunction placeFunction, boolean returnAfterFirstPlace) {
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
                if (state.canBeReplaced()
                        && !isBlockedByEntity(above, block.defaultBlockState().getCollisionShape(level, above), player, level, e -> false, ((p,e) -> {/*do not set crystal*/}))) {
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
                if (!placeFunction.place(pos, block, player, level)) {
                    failed.add(pos);
                } else if (returnAfterFirstPlace) {
                    return allSurrounding;
                }
            }

            if (clearFailedPositions(failed, block, level, player, placeFunction, returnAfterFirstPlace)) {
                return allSurrounding;
            }

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
                        placeFunction.place(pos, block, player, level);
                    }

                    if (clearFailedPositions(failed, block, level, player, placeFunction, returnAfterFirstPlace)) {
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

    private boolean clearFailedPositions(List<BlockPos> failed, Block block, ClientLevel level, Player player, PlaceFunction placeFunction, boolean returnAfterFirstPlace) {
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

    protected void extendAround(BlockPos pos, Set<BlockPos> positions, Set<BlockPos> allSurrounding, Set<BlockPos> allChecked, Set<Entity> checkedEntities,
                                Player player, ClientLevel level, Block block, boolean addBelow) {
        if (!addSupportPosWithBedrockCheck(pos, level, allSurrounding, addBelow)) {
            for (Direction direction : PositionUtil.HORIZONTAL_DIRECTIONS) {
                placeSupporting(pos, positions, allSurrounding, allChecked, checkedEntities, player, level, block, addBelow, direction);
            }
        }
    }

    protected void placeSupporting(BlockPos pos, Set<BlockPos> positions, Set<BlockPos> allSurrounding, Set<BlockPos> allChecked, Set<Entity> checkedEntities,
                                   Player player, ClientLevel level, Block block, boolean addBelow, @Nullable Direction direction) {
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

        // TODO: this gets taken into account by SurroundService and should not be part of the Surround!!!!!!
        //  MAKE THIS A SEPARATE MODULE!
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
                                            Set<BlockPos> allChecked, Set<Entity> checkedEntities, Player player, ClientLevel level, Block block) {
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

    // TODO: also center smartly to place the minimum amount of blocks?
    private void center(LocalPlayer player, ClientLevel level) {
        Set<BlockPos> positions = PositionUtil.getPositionsUnderEntity(player, 0.5);
        if (positions.size() != 1) {
            MovementPlayer firstMove = centerOnce(player, level, positions);
            positions = PositionUtil.getPositionsUnderEntity(player, 0.5);
            if (positions.size() != 1) {
                // center twice, if we are in the middle of a 2x2 its possible that moving into the direction of the center once is not enough
                centerOnce(player, level, positions);
                // If we center twice we need to send a position packet, we can use that packet to do our first rotation
                if (phobot.getAntiCheat().getBlockRotations().getValue()) {
                    BlockPlacer.Action[] firstAction = new BlockPlacer.Action[1];
                    Block block = phobot.getInventoryService().supply(ctx -> ctx.findBlock(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ENDER_CHEST), true, Blocks.OBSIDIAN);
                    PlaceFunction placeFunction = (pos, b, p, l) -> {
                        if (firstAction[0] == null) {
                            firstAction[0] = createPlaceAction(pos, b, p, l, new CenteringBlockPlacingValidityCheck());
                        }

                        return firstAction[0] != null;
                    };
                    // call our placeFunction to find out which will be the first action
                    getAllSurroundingPositions(player, firstMove, level, block, true, placeFunction, true);
                    if (firstAction[0] != null) {
                        float[] rotations = RotationUtil.getRotations(firstMove, level, firstAction[0].getPlaceOn(), firstAction[0].getDirection());
                        // TODO: verify that we have actually send a new yaw and pitch to the server?
                        player.connection.send(new ServerboundMovePlayerPacket.PosRot(firstMove.getX(), firstMove.getY(), firstMove.getZ(), rotations[0], rotations[1], firstMove.onGround()));
                    } else {
                        player.connection.send(new ServerboundMovePlayerPacket.Pos(firstMove.getX(), firstMove.getY(), firstMove.getZ(), firstMove.onGround()));
                    }
                } else {
                    player.connection.send(new ServerboundMovePlayerPacket.Pos(firstMove.getX(), firstMove.getY(), firstMove.getZ(), firstMove.onGround()));
                }
            }
        }
    }

    private MovementPlayer centerOnce(LocalPlayer player, ClientLevel level, Collection<BlockPos> positions) {
        BlockPos best = positions.stream()
                .min(Comparator.comparingDouble(pos -> distance2dSq(pos.getX() + 0.5, pos.getZ() + 0.5, player.getX(), player.getZ())))
                .orElseThrow();
        Vec3 delta =
                new Vec3(best.getX() + 0.5 - player.getX(), 0.0, best.getZ() + 0.5 - player.getZ())
                        .normalize()
                        .scale(phobot.getMovementService().getMovement().getSpeed(player))
                        .add(0.0, player.getDeltaMovement().y, 0.0);
        // TODO: not perfect yet, what if we have already moved this tick?
        MovementPlayer movementPlayer = new MovementPlayer(level);
        movementPlayer.copyPosition(player);
        movementPlayer.setDeltaMovement(delta);
        movementPlayer.move(MoverType.SELF, delta);
        player.setPos(movementPlayer.position());
        player.setOnGround(movementPlayer.onGround());
        startPos = player.blockPosition();
        startY = player.getY();
        return movementPlayer;
    }

    @FunctionalInterface
    private interface PlaceFunction {
        boolean place(BlockPos pos, Block block, Player player, ClientLevel level);
    }

    private class CenteringBlockPlacingValidityCheck implements ChecksBlockPlacingValidity {
        @Override
        public boolean isBlockedByEntity(BlockPos pos, VoxelShape shapeToPlace, Player player, ClientLevel level, Predicate<Entity> consumesBlockingEntities) {
            return isBlockedByEntity(pos, shapeToPlace, player, level, consumesBlockingEntities, (blockPlacer, endCrystal) -> {/* do not set crystal*/});
        }

        @Override
        public BlockPlacer getBlockPlacer() {
            return Surround.this.getBlockPlacer();
        }
    }

}
