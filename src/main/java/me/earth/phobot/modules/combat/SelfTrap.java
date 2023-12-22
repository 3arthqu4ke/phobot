package me.earth.phobot.modules.combat;

import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.phobot.modules.BlockPlacingModule;
import me.earth.phobot.pathfinder.blocks.BlockPathfinder;
import me.earth.phobot.pathfinder.blocks.BlockPathfinderWithBlacklist;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.ResetUtil;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class SelfTrap extends BlockPlacingModule implements TrapsPlayers {
    private final Setting<Integer> maxHelping = number("Helping", 3, 1, 10, "Amount of helping blocks to use.");
    private final Map<BlockPos, Long> blackList = new ConcurrentHashMap<>();
    private final BlockPathfinder blockPathfinder = new BlockPathfinderWithBlacklist(blackList);

    public SelfTrap(Phobot phobot) {
        super(phobot, phobot.getBlockPlacer(), "SelfTrap", Categories.COMBAT, "Traps you.", 0);
        listen(getBlackListClearListener());
        ResetUtil.disableOnRespawnAndWorldChange(this, mc);
    }

    @Override
    protected void updatePlacements(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        Block block = context.findBlock(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ENDER_CHEST);
        if (block == null) {
            return;
        }

        trap(player, level, player, block, new HashSet<>());
    }

}
