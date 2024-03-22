package me.earth.phobot.modules.misc;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.modules.BlockPlacingModule;
import me.earth.phobot.modules.combat.AutoMine;
import me.earth.phobot.services.SurroundService;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

// TODO: make this more robust!
// TODO: check for enemies that could pick up
@Slf4j
public class AutoEchest extends BlockPlacingModule {
    public static final Direction[] DIRECTIONS_NO_DOWN = new Direction[] { Direction.UP, Direction.NORTH, Direction.WEST, Direction.EAST, Direction.SOUTH };
    private final Setting<BlockMode> block = constant("Block", BlockMode.Full, "Places a block so that the item will drop towards us.");
    private final SurroundService surroundService;
    private final Speedmine speedmine;
    private final AutoMine autoMine;

    public AutoEchest(Phobot phobot, SurroundService surroundService, AutoMine autoMine) {
        super(phobot, phobot.getBlockPlacer(), "AutoEchest", Categories.MISC, "Places echests and mines them.", 0);
        this.surroundService = surroundService;
        this.speedmine = autoMine.getSpeedmine();
        this.autoMine = autoMine;
    }

    @Override
    protected void updatePlacements(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (!speedmine.isEnabled()) {
            getPingBypass().getChat().sendWithoutLogging(Component.literal("Speedmine needs to be enabled for AutoEchest!").withStyle(ChatFormatting.RED), "SpeedmineCheck");
            return;
        }

        if (!surroundService.isSurrounded() || !context.has(Blocks.ENDER_CHEST.asItem())) {
            return;
        }

        int count = context.getCount(stack -> stack.is(Items.OBSIDIAN));
        if (count >= 128) {
            return;
        }

        Set<BlockPos> positions = surroundService.getSurround().getAllSurroundingPositions(player, player, level, Blocks.OBSIDIAN, false);
        positions.removeIf(pos -> pos.getY() < player.getY() - 0.5);
        BlockPos speedminePos = speedmine.getCurrentPos();
        if (speedminePos != null && positions.contains(speedminePos.below())) {
            if (!level.getBlockState(speedminePos).canBeReplaced()) {
                return;
            }

            blockAndPlace(speedminePos, player, level);
            return;
        } else if (speedminePos != null && level.getBlockState(speedminePos).is(Blocks.ENDER_CHEST)) {
            return;
        }

        List<BlockPos> sortedPositions = positions.stream().map(BlockPos::above).sorted(Comparator.comparingDouble(pos -> pos.distToCenterSqr(player.position()))).toList();
        for (BlockPos pos : sortedPositions) {
            if (level.getBlockState(pos).is(Blocks.ENDER_CHEST)) {
                speedmine.startDestroy(pos, level, player);
                return;
            }
        }

        for (BlockPos pos : sortedPositions) {
            if (level.getBlockState(pos).canBeReplaced()) {
                if (blockAndPlace(pos, player, level)) {
                    return;
                }
            }
        }
    }

    private boolean blockAndPlace(BlockPos pos, LocalPlayer player, ClientLevel level) {
        if (placePos(pos, Blocks.ENDER_CHEST, player, level)) {
            switch (block.getValue()) {
                case Single -> placeSingle(pos, player, level);
                case Above -> {
                    placeSingle(pos.above(), player, level);
                    placeSingle(pos, player, level);
                }
                case Full -> {
                    for (Direction direction : DIRECTIONS_NO_DOWN) {
                        placePos(pos.relative(direction), Blocks.OBSIDIAN, player, level);
                    }
                }
                default -> { }
            }

            return true;
        }

        return false;
    }

    private void placeSingle(BlockPos pos, LocalPlayer player, ClientLevel level) {
        BlockPos furthest = pos.relative(Direction.UP);
        double furthestDistance = 0.0;
        for (Direction direction : PositionUtil.HORIZONTAL_DIRECTIONS) {
            BlockPos offset = pos.relative(direction);
            double dist = offset.distToCenterSqr(player.position());
            if (dist > furthestDistance) {
                furthestDistance = dist;
                furthest = offset;
            }
        }

        placePos(furthest, Blocks.OBSIDIAN, player, level);
    }

    public enum BlockMode {
        None,
        Single,
        Above,
        Full
    }

}
