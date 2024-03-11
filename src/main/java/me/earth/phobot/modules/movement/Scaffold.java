package me.earth.phobot.modules.movement;

import me.earth.phobot.Phobot;
import me.earth.phobot.modules.BlockPlacingModule;
import me.earth.phobot.pathfinder.blocks.BlockPathfinder;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

public class Scaffold extends BlockPlacingModule {
    private final Setting<Boolean> down = bool("Down", true, "Allows you to go down when shifting.");
    private final Setting<Integer> clutch = number("Clutch", 0, 0, 10, "Places multiple blocks to place underneath you.");
    private final Setting<Integer> below = number("Below", 1, 0, 10, "Amount of blocks to place below when clutching.");
    private final StopWatch.ForMultipleThreads towerTimer = new StopWatch.ForMultipleThreads();
    private final BlockPathfinder pathfinder = new BlockPathfinder();

    public Scaffold(Phobot phobot) {
        super(phobot, phobot.getBlockPlacer(), "Scaffold", Categories.MOVEMENT, "Places blocks underneath you.", 0);
        this.unregister(getDelay());
        getDelay().setValue(0);
    }

    @Override
    protected void onEnable() {
        towerTimer.reset();
    }

    @Override
    public boolean placePos(BlockPos pos, Block block, Player player, ClientLevel level) {
        boolean placed = super.placePos(pos, block, player, level);
        if (placed) {
            if (player instanceof LocalPlayer localPlayer
                    && mc.options.keyJump.isDown()
                    && !mc.options.keyShift.isDown()
                    && localPlayer.input.forwardImpulse == 0.0
                    && localPlayer.input.leftImpulse == 0.0) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.42f + player.getJumpBoostPower(), player.getDeltaMovement().z);
                if (towerTimer.passed(1500L)) {
                    player.setDeltaMovement(player.getDeltaMovement().x, -0.28, player.getDeltaMovement().z);
                    towerTimer.reset();
                }
            } else {
                towerTimer.reset();
            }
        }

        return placed;
    }

    @Override
    protected void updatePlacements(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (!(mc.options.keyJump.isDown() && !mc.options.keyShift.isDown() && player.input.forwardImpulse == 0.0 && player.input.leftImpulse == 0.0)) {
            towerTimer.reset();
        }

        Block block = context.find(
                s -> s.getItem().getItem() instanceof BlockItem b
                        && !(b.getBlock() instanceof Fallable)
                        && b.getBlock().defaultBlockState().getCollisionShape(level, BlockPos.ZERO).equals(Shapes.block()) ? b.getBlock() : null,
                s -> s.getItem().getItem() instanceof BlockItem b
                        && !(b.getBlock() instanceof Fallable)
                        && !b.getBlock().defaultBlockState().canBeReplaced()
                        && !b.getBlock().defaultBlockState().getCollisionShape(level, BlockPos.ZERO).isEmpty() ? b.getBlock() : null);
        if (block == null) {
            return;
        }

        BlockPos initial = player.blockPosition().below();
        BlockPos pos = player.blockPosition().below();
        if (down.getValue() && !mc.options.keyJump.isDown() && mc.options.keyShift.isDown()) {
            pos = pos.below();
        }

        if (level.getBlockState(pos).canBeReplaced()) {
            int pathLength;
            if (!placePos(pos, block, player, level) && (pathLength = this.clutch.getValue()) > 0) {
                int b = below.getValue(); // TODO: make this based on the amount of actions we can perform per tick and check if we fall further during the amount of ticks needed to clutch
                for (int i = 1; i <= b; i++) {
                    pos = initial.below(i);
                    if (!level.getBlockState(pos).canBeReplaced()) {
                        return;
                    }
                }

                List<BlockPos> path = pathfinder.getShortestPath(pos, block, player, level, pathLength, this, getBlockPlacer());
                if (!path.isEmpty()) {
                    for (int i = 1/*skip first, it should be the goal*/; i < path.size() - 1; i++) {
                        placePos(path.get(i), block, player, level);
                    }
                }
            }

            return;
        }

        if (mc.options.keyUp.isDown() && !mc.options.keyDown.isDown()) {
            pos = pos.relative(player.getDirection());
        } else if (!mc.options.keyUp.isDown() && mc.options.keyDown.isDown()) {
            pos = pos.relative(player.getDirection().getOpposite());
        }

        if (level.getBlockState(pos).canBeReplaced()) {
            placePos(pos, block, player, level);
            return;
        }

        if (mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
            pos = pos.relative(player.getDirection().getCounterClockWise());
        } else if (!mc.options.keyLeft.isDown() && mc.options.keyRight.isDown()) {
            pos = pos.relative(player.getDirection().getClockWise());
        }

        if (level.getBlockState(pos).canBeReplaced()) {
            placePos(pos, block, player, level);
        }
    }

}
