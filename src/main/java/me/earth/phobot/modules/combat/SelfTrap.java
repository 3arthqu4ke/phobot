package me.earth.phobot.modules.combat;

import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.phobot.holes.Hole;
import me.earth.phobot.modules.SwappingBlockPlacingModule;
import me.earth.phobot.pathfinder.blocks.BlockPathfinder;
import me.earth.phobot.pathfinder.blocks.BlockPathfinderWithBlacklist;
import me.earth.phobot.services.SurroundService;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.player.PredictionPlayer;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class SelfTrap extends SwappingBlockPlacingModule implements TrapsPlayers, DetectsPlayerApproaching {
    private final Setting<Integer> maxHelping = number("Helping", 3, 1, 10, "Amount of helping blocks to use.");
    private final Setting<Boolean> auto = bool("Auto", true, "Waits for a player to approach you and only then traps you.");
    private final Setting<Double> distance = precise("Distance", 2.0, 0.1, 6.0, "Distance of the target player to our position when using Auto.");
    private final Setting<Integer> prediction = number("Prediction", 2, 1, 5, "Distance of the target player our position when using Auto.");

    private final Map<BlockPos, Long> blackList = new ConcurrentHashMap<>();
    private final BlockPathfinder blockPathfinder = new BlockPathfinderWithBlacklist(blackList);
    private final SurroundService surroundService;

    public SelfTrap(Phobot phobot, SurroundService surroundService) {
        super(phobot, phobot.getBlockPlacer(), "SelfTrap", Categories.COMBAT, "Traps you.", 0);
        this.surroundService = surroundService;
        listen(getBlackListClearListener());
        ResetUtil.onRespawnOrWorldChange(this, mc, () -> {
            if (!auto.getValue()) {
                this.disable();
            }
        });
    }

    @Override
    protected void updatePlacements(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        Block block = context.findBlock(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ENDER_CHEST);
        if (block == null) {
            return;
        }

        if (auto.getValue()) {
            Hole hole;
            if (surroundService.isSurrounded() && (hole = phobot.getHoleManager().getHolePlayerIsIn(player)) != null) {
                checkApproachingPlayers(player, block, level, Set.of(hole));
            }
        } else {
            trap(player, level, player, block, new HashSet<>());
        }
    }

    @Override
    public boolean onPlayerApproachingHole(Hole hole, Block block, PredictionPlayer pp, LocalPlayer localPlayer, ClientLevel level) {
        if (pp.getPlayer().getBoundingBox().intersects(localPlayer.getBoundingBox())) {
            return false;
        }

        trap(localPlayer, level, localPlayer, block, new HashSet<>());
        return true;
    }

}
