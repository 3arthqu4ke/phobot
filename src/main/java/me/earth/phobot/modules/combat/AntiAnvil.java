package me.earth.phobot.modules.combat;

import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.phobot.modules.BlockPlacingModule;
import me.earth.phobot.pathfinder.blocks.BlockPathfinder;
import me.earth.phobot.pathfinder.blocks.BlockPathfinderWithBlacklist;
import me.earth.phobot.services.SurroundService;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class AntiAnvil extends BlockPlacingModule implements TrapsPlayers {
    private final Setting<Integer> maxHelping = number("Helping", 3, 1, 10, "Amount of helping blocks to use.");
    private final Map<BlockPos, Long> blackList = new ConcurrentHashMap<>();
    private final BlockPathfinder blockPathfinder = new BlockPathfinderWithBlacklist(blackList);
    private final SurroundService surroundService;

    public AntiAnvil(Phobot phobot, SurroundService surroundService) {
        super(phobot, phobot.getBlockPlacer(), "AntiAnvil", Categories.COMBAT, "Places blocks above you if anvils fall down on you.", 0);
        this.surroundService = surroundService;
    }

    @Override
    protected void updatePlacements(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (!surroundService.isSurrounded(player)) {
            return;
        }

        Block block = context.findBlock(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ENDER_CHEST);
        if (block == null) {
            return;
        }

        AABB bb = player.getBoundingBox();
        if (bb.maxY > level.getMaxBuildHeight() + 1) {
            return;
        }

        bb = new AABB(bb.minX, bb.minY, bb.minZ, bb.maxX, level.getMaxBuildHeight() + 1, bb.maxZ);
        if (!level.getEntities(EntityType.FALLING_BLOCK, bb, entity -> entity.getBlockState().getBlock() instanceof AnvilBlock).isEmpty()) {
            trap(player, level, player, block, new HashSet<>());
        }
    }

    @Override
    public double getY(Player player) {
        return TrapsPlayers.super.getY(player) + 1;
    }

    @Override
    public boolean checkBelow() {
        return false;
    }

}
