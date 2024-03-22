package me.earth.phobot.modules.combat;

import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.phobot.modules.BlockPlacingModule;
import me.earth.phobot.modules.SwappingBlockPlacingModule;
import me.earth.phobot.pathfinder.blocks.BlockPathfinder;
import me.earth.phobot.pathfinder.blocks.BlockPathfinderWithBlacklist;
import me.earth.phobot.services.SurroundService;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class AutoTrap extends SwappingBlockPlacingModule implements TrapsPlayers {
    private final Setting<PacketRotationMode> packetRotations = constant("PacketRotations", PacketRotationMode.None, "Sends additional packets to rotate, might lag back," +
            "but allows you to place more blocks in one tick. With Mode Surrounded this will only happen when you are surrounded, so that you do not have to worry about lagbacks.");
    private final Setting<Integer> maxHelping = number("Helping", 3, 1, 10, "Amount of helping blocks to use.");
    private final Map<BlockPos, Long> blackList = new ConcurrentHashMap<>();
    private final BlockPathfinder blockPathfinder = new BlockPathfinderWithBlacklist(blackList);
    private final SurroundService surroundService;

    public AutoTrap(Phobot phobot, SurroundService surroundService) {
        super(phobot, phobot.getBlockPlacer(), "AutoTrap", Categories.COMBAT, "Traps other players.", 5);
        this.surroundService = surroundService;
        listen(getBlackListClearListener());
        ResetUtil.disableOnRespawnAndWorldChange(this, mc);
    }

    @Override
    protected void updatePlacements(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        Block block = context.findBlock(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN);
        if (block == null) {
            return;
        }

        Set<BlockPos> allCurrentPositions = new HashSet<>();
        for (Player enemy : level.players()) {
            if (EntityUtil.isEnemyInRange(getPingBypass(), player, enemy, 7.0)) {
                trap(player, level, enemy, block, allCurrentPositions);
            }
        }
    }

    @Override
    protected boolean isUsingPacketRotations() {
        return switch (packetRotations.getValue()) {
            case Always -> true;
            case Surrounded -> surroundService.isSurrounded();
            default -> false;
        };
    }

}
