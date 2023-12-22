package me.earth.phobot.modules;

import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.api.traits.Nameable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for modules that place blocks.
 */
@Getter
public abstract class BlockPlacingModule extends PhobotModule implements ChecksBlockPlacingValidity, BlockPlacer.PlacesBlocks {
    private final Setting<Integer> delay = number("Delay", 30, 0, 500, "Delay between placements.");
    private final Setting<Boolean> noGlitchBlocks = bool("NoGlitchBlocks", false, "Does not place a block on the clientside.");
    private final StopWatch.ForSingleThread timer = new StopWatch.ForSingleThread();
    private final BlockPlacer blockPlacer;
    private final int priority;

    public BlockPlacingModule(Phobot phobot, BlockPlacer blockPlacer, String name, Nameable category, String description, int priority) {
        super(phobot, name, category, description);
        this.blockPlacer = blockPlacer;
        this.priority = priority;
        // TODO: clear blockplacer when unregistered etc!
        blockPlacer.getModules().add(this);
    }

    @Override
    public boolean isActive() {
        return isEnabled();
    }

    @Override
    public void update(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (!timer.passed(delay.getValue())) {
            return;
        }

        updatePlacements(context, player, level, gameMode);
    }

    protected abstract void updatePlacements(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode);

    public boolean isUsingSetCarriedItem() {
        return true;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean placePos(BlockPos pos, Block block, LocalPlayer player, ClientLevel level) {
        for (BlockPlacer.Action action : blockPlacer.getActions()) {
            if (action.getPos().equals(pos)) {
                return true;
            }
        }

        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (!state.canBeReplaced()) {
            return false;
        }

        Set<BlockPlacer.Action> dependencies = new HashSet<>();
        Direction direction = getDirection(level, player, pos, dependencies);
        if (direction == null) {
            return false;
        }

        // TODO: not necessary for crystalpvp.cc, but trace the proper checks in BlockItem, they are much more accurate
        BlockPos placeOn = pos.relative(direction);
        BlockState futureState = block.defaultBlockState(); // TODO: use this!!! block.getStateForPlacement(blockPlaceContext) though for the simple blocks on cc this should be fine for now
        VoxelShape shape = futureState.getCollisionShape(level, pos, blockPlacer.getCollisionContext());
        if (!shape.isEmpty() && isBlockedByEntity(pos, shape, player, level, entity -> false)) {
            return false;
        }

        BlockPlacer.Action action = new BlockPlacer.Action(this, placeOn.immutable(), pos.immutable(), direction.getOpposite(), block.asItem(), !noGlitchBlocks.getValue(), isUsingSetCarriedItem(),
                isUsingPacketRotations(), getBlockPlacer(), requiresExactDirection());
        action.getDependencies().addAll(dependencies);
        addAction(blockPlacer, action);
        return true;
    }

    protected boolean isUsingPacketRotations() {
        return false;
    }

    protected boolean requiresExactDirection() { // for special stuff like piston aura, bedaura etc. where where we place matters.
        return false;
    }

    protected void addAction(BlockPlacer placer, BlockPlacer.Action action) {
        placer.addAction(action);
        timer.reset();
    }

    protected void updateOutsideOfTick(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        phobot.getInventoryService().use(context -> {
            getBlockPlacer().startTick(player, level);
            update(context, player, level, gameMode);
            getBlockPlacer().endTick(context, player, level);
        });
    }

    public enum Rotate {
        None,
        Rotate,
        Packets
    }

}
