package me.earth.phobot.modules.combat;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.modules.SwappingBlockPlacingModule;
import me.earth.phobot.services.BlockDestructionService;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// TODO: use bomber to check if they can place?
// TODO: bypass strictdirection, by sending packets for a jump we can jump and then place (probably blocked by grim)
@Slf4j
public class Blocker extends SwappingBlockPlacingModule {
    private final Setting<Integer> ticks = number("Ticks", 3, 0, 10, "We predict when a player will break a block around us and block it this many ticks before.");
    private final Surround surround;

    public Blocker(Phobot phobot, Surround surround) {
        super(phobot, phobot.getBlockPlacer(), "Blocker", Categories.COMBAT, "Protects you against crystal bombing.", 0);
        this.surround = surround;
    }

    @Override
    protected void updatePlacements(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        Block block = context.findBlock(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ENDER_CHEST);
        if (block == null) {
            return;
        }

        for (BlockPos pos : surround.getAllSurroundingPositions(player, player, level, Blocks.OBSIDIAN, false)) {
            checkPos(pos, block, player, level, PositionUtil.DIRECTIONS);
            checkPos(pos.above(), block, player, level, Direction.UP);
        }

        for (BlockPos pos : PositionUtil.getPositionsBlockedByEntityAtY(player, player.getBoundingBox().minY + 2.5)) {
            checkPos(pos, block, player, level, Direction.UP);
        }
    }

    private void checkPos(BlockPos pos, Block block, LocalPlayer player, ClientLevel level, Direction... directions) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.BEDROCK)) {
            BlockDestructionService.Progress progress = phobot.getBlockDestructionService().getPositions().get(pos);
            float damageDelta;
            if (progress != null && (damageDelta = getMaxDigSpeed(pos, state, level)) * ((float) TimeUtil.getPassedTimeSince(progress.timeStamp()) / 50L) + ticks.getValue() * damageDelta >= 1.0f) {
                for (Direction direction : directions) {
                    placePos(pos.relative(direction), block, player, level);
                }
            }
        }
    }

    public static float getMaxDigSpeed(BlockPos pos, BlockState state, ClientLevel level) {
        float destroySpeed = state.getDestroySpeed(level, pos);
        if (destroySpeed == -1.0f || state.isAir()) {
            destroySpeed = Blocks.OBSIDIAN.defaultBlockState().getDestroySpeed(level, pos);
        }

        return getFastestPlayerDestroySpeed() / destroySpeed / 30.0f;
    }

    private static float getFastestPlayerDestroySpeed() {
        float digSpeed = Tiers.NETHERITE.getSpeed();
        if (digSpeed > 1.0F) {
            int i = Enchantments.BLOCK_EFFICIENCY.getMaxLevel();
            if (i > 0) {
                digSpeed += (float)(i * i + 1);
            }
        }

        return Math.max(digSpeed, 0.0f);
    }

}
