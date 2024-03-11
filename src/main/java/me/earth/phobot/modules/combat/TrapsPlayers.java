package me.earth.phobot.modules.combat;

import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.modules.ChecksBlockPlacingValidity;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Traps players in a cage made up of blocks.
 *
 * @see AutoTrap
 * @see SelfTrap
 * @see AntiAnvil
 */
public interface TrapsPlayers extends ChecksBlockPlacingValidity, FindsShortestPath {
    Map<BlockPos, Long> getBlackList();

    boolean placePos(BlockPos pos, Block block, Player player, ClientLevel level);

    default double getY(Player player) {
        return player.getBoundingBox().maxY;
    }

    default boolean checkBelow() {
        return true;
    }

    default void trap(LocalPlayer player, ClientLevel level, Player enemy, Block block, Set<BlockPos> allCurrentPositions) {
        List<BlockPos> positions = PositionUtil.getPositionsBlockedByEntityAtY(enemy, getY(enemy)).stream().map(BlockPos::above).collect(Collectors.toList());
        positions.removeIf(pos -> getBlackList().containsKey(pos) || !level.getBlockState(pos.below()).isAir() || checkBelow() && !level.getBlockState(pos.below(2)).isAir());
        var shortest = findShortestPath(player, level, new ArrayList<>(positions), block);
        if (shortest != null && !shortest.getValue().isEmpty()) {
            for (int i = 1/*skip first, it should be the goal*/; i < shortest.getValue().size() - 1; i++) {
                BlockPos pos = shortest.getValue().get(i);
                if (!placePos(pos, block, player, level)) {
                    return;
                }

                allCurrentPositions.add(pos);
            }

            positions.sort(Comparator.comparingInt(pos -> pos.distManhattan(shortest.getValue().get(shortest.getValue().size() - 1))));
            for (BlockPos pos : positions) {
                placePos(pos, block, player, level);
                allCurrentPositions.add(pos);
            }
        }
    }

    default Listener<?> getBlackListClearListener() {
        return new Listener<PreMotionPlayerUpdateEvent>(Integer.MAX_VALUE) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event) {
                getBlackList().entrySet().removeIf(entry -> TimeUtil.isTimeStampOlderThan(entry.getValue(), 150L));
            }
        };
    }

}
