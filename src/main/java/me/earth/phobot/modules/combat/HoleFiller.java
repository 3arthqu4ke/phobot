package me.earth.phobot.modules.combat;

import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.phobot.holes.Hole;
import me.earth.phobot.modules.BlockPlacingModule;
import me.earth.phobot.services.SurroundService;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.player.PredictionPlayer;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

// TODO: FastFall!
@Getter
public class HoleFiller extends BlockPlacingModule implements DetectsPlayerApproaching {
    private final Setting<Double> distance = precise("Distance", 2.0, 0.1, 6.0, "Distance of the target player to the hole we want to fill.");
    private final Setting<Integer> prediction = number("Prediction", 2, 1, 5, "Distance of the target player to the hole we want to fill.");
    private final Setting<Boolean> safe = bool("Safe", false, "Only uses this module when you are safe.");
    private final SurroundService surroundService;

    public HoleFiller(Phobot phobot, SurroundService surroundService) {
        super(phobot, phobot.getBlockPlacer(), "HoleFiller", Categories.COMBAT, "Fills holes around enemy players.", 0);
        this.surroundService = surroundService;
    }

    // TODO: dont fill phobot target hole!
    @Override
    protected void updatePlacements(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        Block block = context.findBlock(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ENDER_CHEST, Blocks.ANVIL);
        if (block == null) {
            return;
        }

        Set<Hole> holes = new HashSet<>();
        for (Hole hole : phobot.getHoleManager().getMap().values()) {
            if (hole.getDistanceSqr(player) < ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE && hole.isValid()) {
                holes.add(hole);
            }
        }

        if (holes.isEmpty()) {
            return;
        }

        checkApproachingPlayers(player, block, level, holes);
    }

    @Override
    public boolean onPlayerApproachingHole(Hole hole, Block block, PredictionPlayer pp, LocalPlayer localPlayer, ClientLevel level) {
        if (hole.is2x2()) {
            // in a 2x2 its enough to just fill in one block
            for (BlockPos pos : hole.getAirParts().stream().sorted(Comparator.comparingDouble(p -> -p.distToCenterSqr(pp.position()))).toList()) {
                if (placePos(pos, block, localPlayer, level)) {
                    break;
                }
            }
        } else {
            for (BlockPos pos : hole.getAirParts()) {
                placePos(pos, block, localPlayer, level);
            }
        }

        return false;
    }

}
