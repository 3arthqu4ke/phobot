package me.earth.phobot.services;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.earth.phobot.damagecalc.CrystalPosition;
import me.earth.phobot.damagecalc.DamageCalculator;
import me.earth.phobot.event.PostMotionPlayerUpdateEvent;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.modules.ChecksBlockPlacingValidity;
import me.earth.phobot.modules.client.anticheat.AntiCheat;
import me.earth.phobot.modules.combat.autocrystal.CrystalPlacer;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.services.inventory.InventoryService;
import me.earth.phobot.util.math.RaytraceUtil;
import me.earth.phobot.util.math.RotationUtil;
import me.earth.phobot.util.world.BlockStateLevel;
import me.earth.phobot.util.world.PredictionUtil;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.commons.event.SafeListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static me.earth.phobot.services.inventory.InventoryContext.*;

@Getter
public class BlockPlacer extends SubscriberImpl {
    public static final int PRIORITY = 10_000;
    private final Queue<PlacesBlocks> modules = new PriorityQueue<>();
    private final List<Action> actions = new ArrayList<>();
    private final LocalPlayerPositionService localPlayerPositionService;
    private final MotionUpdateService motionUpdateService;
    private final AntiCheat antiCheat;
    private final Minecraft minecraft;

    private CollisionContext collisionContext = CollisionContext.empty();
    private BlockStateLevel.Delegating customBlockStateLevel = null;
    @Setter
    private EndCrystal crystal;
    private boolean attacked = false;
    private boolean completed = false;
    private volatile boolean inTick = false;

    public BlockPlacer(LocalPlayerPositionService localPlayerPositionService, MotionUpdateService motionUpdateService, InventoryService inventoryService, Minecraft minecraft, AntiCheat antiCheat) {
        this.localPlayerPositionService = localPlayerPositionService;
        this.motionUpdateService = motionUpdateService;
        this.antiCheat = antiCheat;
        this.minecraft = minecraft;
        listen(new SafeListener<PreMotionPlayerUpdateEvent>(minecraft, -PRIORITY) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                inventoryService.use(context -> {
                    startTick(player, level);
                    for (PlacesBlocks module : modules) {
                        if (module.isActive()) {
                            module.update(context, player, level, gameMode);
                        }
                    }

                    if (antiCheat.getBlockRotations().getValue() && !actions.isEmpty()) {
                        boolean spoofing = motionUpdateService.spoofing;
                        motionUpdateService.spoofing = false;
                        // end the tick before we sent our rotation and position to the server, so we can place blocks that rely on the last rotation sent
                        endTick(context, player, level, false);
                        motionUpdateService.spoofing = spoofing || motionUpdateService.spoofing;
                    }
                });
            }
        });

        listen(new SafeListener<PostMotionPlayerUpdateEvent>(minecraft, PRIORITY) {
            @Override
            public void onEvent(PostMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                inventoryService.use(context -> endTick(context, player, level));
            }
        });
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public void startTick(LocalPlayer player, ClientLevel level) {
        customBlockStateLevel = new BlockStateLevel.Delegating(level);
        collisionContext = CollisionContext.of(player);
        actions.clear();
        crystal = null;
        attacked = false;
        completed = false;
        inTick = true;
    }

    public void breakCrystal(LocalPlayer player) {
        if (crystal != null && !attacked) {
            // TODO: use attacking service with InventoryContext!!!!
            // TODO: only break crystal if the action is actually going to be executed
            player.connection.send(ServerboundInteractPacket.createAttackPacket(crystal, false));
            player.swing(InteractionHand.MAIN_HAND);
            attacked = true;
        }
    }

    public void endTick(InventoryContext context, LocalPlayer player, ClientLevel level) {
        endTick(context, player, level, true);
    }

    private void endTick(InventoryContext context, LocalPlayer player, ClientLevel level, boolean clear) {
        if (!actions.isEmpty()) {
            breakCrystal(player);
            boolean shiftKey = player.isShiftKeyDown(); // TODO: better way to detect if the server knows this?!
            if (!shiftKey) {
                // the players pose only gets updated every server tick, so this does not matter for rotations!
                player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
            }

            List<Action> failedActions = new ArrayList<>(actions.size());
            Set<Action> completedActions = new HashSet<>(actions.size());
            for (int i = 0; i < actions.size() && i < antiCheat.getActions().getValue(); i++) { // TODO: If we sent rotations send less packets!
                Action action = actions.get(i);
                if (failedActions.stream().anyMatch(action.dependencies::contains) || !action.execute(completedActions, context, player, level)) {
                    failedActions.add(action);
                } else {
                    completedActions.add(action);
                }
            }

            actions.removeIf(completedActions::contains);
            if (!shiftKey) {
                player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
            }

            if (failedActions.isEmpty()) {
                completed = true;
            }
        }

        if (clear) {
            customBlockStateLevel = null;
            collisionContext = null;
            actions.clear();
            crystal = null;
            inTick = false;
        }
    }

    public DamageCalculator getDamageCalculator() {
        return antiCheat.getDamageCalculator();
    }

    public interface PlacesBlocks extends Comparable<PlacesBlocks> {
        int getPriority();

        void update(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode);

        default boolean isActive() {
            return true;
        }

        @Override
        default int compareTo(@NotNull BlockPlacer.PlacesBlocks o) {
            return Integer.compare(o.getPriority(), this.getPriority());
        }
    }

    @Data
    public static class Action implements ChecksBlockPlacingValidity {
        private final Object module;
        @EqualsAndHashCode.Exclude
        private final Set<Action> dependencies = new HashSet<>();
        private final BlockPos placeOn;
        private final BlockPos pos;
        private final Direction direction;
        private final Item item;
        private final boolean shouldSetBlock;
        private final boolean usingSetCarried;
        private final boolean packetRotations;
        private final BlockPlacer blockPlacer;
        private final boolean requiresExactDirection;

        public boolean execute(Set<Action> completedActions, InventoryContext context, LocalPlayer player, ClientLevel level) {
            if (blockPlacer.getAntiCheat().isAboveBuildHeight(pos.getY())) {
                return false;
            }

            BlockStateLevel.Delegating stateLevel = Objects.requireNonNull(blockPlacer.getCustomBlockStateLevel(), "BlockPlace.stateLevel was null!");
            Player rotationPlayer = blockPlacer.getLocalPlayerPositionService().getPlayerOnLastPosition(player);
            if (isOutsideRange(placeOn, rotationPlayer)) {
                return false;
            }

            if (!stateLevel.getBlockState(pos).canBeReplaced()) {
                return false;
            }

            BlockHitResult hitResult = null;
            if (blockPlacer.getAntiCheat().getBlockRotations().getValue()) {
                boolean invert = blockPlacer.getAntiCheat().getOpposite().getValue();
                Player finalRotationPlayer = rotationPlayer;
                hitResult = requiresExactDirection
                        ? (strictDirectionCheck(placeOn, direction, stateLevel, rotationPlayer) ? RaytraceUtil.raytrace(rotationPlayer, stateLevel, placeOn, direction, invert) : null)
                        : RaytraceUtil.raytraceToPlaceTarget(rotationPlayer, stateLevel, pos, (p,d) -> strictDirectionCheck(p, d, stateLevel, finalRotationPlayer), invert);
                if (hitResult == null || !hitResult.getBlockPos().relative(hitResult.getDirection()).equals(pos)) {
                    if (!blockPlacer.getMinecraft().isSameThread()) {
                        return false;
                    }

                    boolean fail = true;
                    MotionUpdateService motionUpdateService = blockPlacer.motionUpdateService;
                    if (packetRotations && !motionUpdateService.isInPreUpdate()) {
                        float[] rotations = RotationUtil.getRotations(rotationPlayer, stateLevel, placeOn, direction);
                        player.connection.send(new ServerboundMovePlayerPacket.Rot(rotations[0], rotations[1], rotationPlayer.onGround()));
                        rotationPlayer = blockPlacer.getLocalPlayerPositionService().getPlayerOnLastPosition(player); // update, player has new rotation
                        hitResult = new BlockHitResult(RotationUtil.getHitVec(placeOn, stateLevel, direction), direction, placeOn, false);
                        fail = false;
                    } else if ((dependencies.isEmpty() || completedActions.containsAll(dependencies)) && !motionUpdateService.isSpoofing() && motionUpdateService.isInPreUpdate()) {
                        float[] rotations = RotationUtil.getRotations(player, stateLevel, placeOn, direction);
                        motionUpdateService.rotate(player, rotations[0], rotations[1]);
                    }

                    if (fail) {
                        return false;
                    }
                }
            } else if (!strictDirectionCheck(placeOn, direction, stateLevel, rotationPlayer)) {
                return false;
            }

            InventoryContext.SwitchResult switchResult = context.switchTo(item, PREFER_MAINHAND | SWITCH_BACK | (usingSetCarried ? SET_CARRIED_ITEM : 0));
            if (switchResult == null) {
                return false;
            }

            if (hitResult == null) {
                VoxelShape shape = stateLevel.getBlockState(placeOn).getCollisionShape(level, placeOn);
                Vec3 hitVec = new Vec3(direction.getStepX() + pos.getX(), direction.getStepY() + pos.getY(), direction.getStepZ() + pos.getZ());
                for (AABB bb : shape.toAabbs()) { // not really optimal...
                    double x = direction.getAxis() == Direction.Axis.X ? (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? bb.maxX : bb.minX) : bb.minX + (bb.maxX - bb.minX) / 2.0;
                    double y = direction.getAxis() == Direction.Axis.Y ? (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? bb.maxY : bb.minY) : bb.minY + (bb.maxY - bb.minY) / 2.0;
                    double z = direction.getAxis() == Direction.Axis.Z ? (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? bb.maxZ : bb.minZ) : bb.minZ + (bb.maxZ - bb.minZ) / 2.0;
                    hitVec = new Vec3(x + placeOn.getX(), y + placeOn.getY(), z + placeOn.getZ());
                    break;
                }

                hitResult = new BlockHitResult(hitVec, direction, placeOn, false);
            }

            BlockHitResult finalHitResult = hitResult;
            Player finalRotationPlayer = rotationPlayer;
            PredictionUtil.predict(level, seq -> {
                BlockPlaceContext blockPlaceContext = new BlockPlaceContext(finalRotationPlayer, switchResult.hand(), switchResult.slot().getItem(), finalHitResult);
                BlockState computedState = null;
                if (item instanceof BlockItem blockItem) {
                    computedState = blockItem.getBlock().defaultBlockState();
                    var newBlockPlaceContext = blockItem.updatePlacementContext(blockPlaceContext);
                    blockPlaceContext = newBlockPlaceContext == null ? blockPlaceContext : newBlockPlaceContext;
                    BlockState blockState = blockItem.getBlock().getStateForPlacement(blockPlaceContext);
                    computedState = blockState == null ? computedState : blockState;
                }

                if (computedState != null) {
                    if (shouldSetBlock) {
                        level.setBlock(pos, computedState, Block.UPDATE_ALL_IMMEDIATE);
                    }

                    stateLevel.getMap().put(pos, computedState);
                }

                if (!player.isCreative()) {
                    player.getItemInHand(switchResult.hand()).shrink(1); // TODO: not perfect yet, since this is handled by use item on.
                }

                player.connection.send(new ServerboundUseItemOnPacket(switchResult.hand(), finalHitResult, seq));
            });

            player.swing(switchResult.hand());
            return true;
        }

        public static Action crystalPlacingAction(CrystalPosition crystalPosition, CrystalPlacer crystalPlacer) {
            BlockPos immutable = crystalPosition.immutable();
            CrystalPosition copy = crystalPosition.copy();
            return new Action(crystalPlacer.module(), immutable, immutable, Direction.UP, Items.END_CRYSTAL, false, false, false, crystalPlacer.module().getBlockPlacer(), false) {
                @Override
                public boolean execute(Set<Action> completedActions, InventoryContext context, LocalPlayer player, ClientLevel clientLevel) {
                    BlockStateLevel.Delegating level = Objects.requireNonNull(getBlockPlacer().getCustomBlockStateLevel(), "BlockPlace.stateLevel was null!");
                    crystalPlacer.place(context, player, level, copy);
                    return true;
                }
            };
        }
    }

}
