package me.earth.phobot.pathfinder.blocks;

import lombok.SneakyThrows;
import me.earth.phobot.Phobot;
import me.earth.phobot.TestPhobot;
import me.earth.phobot.TestUtil;
import me.earth.phobot.modules.ChecksBlockPlacingValidity;
import me.earth.phobot.modules.client.anticheat.StrictDirection;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.util.player.FakePlayer;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BlockPathfinderTest {
    @Test
    @SneakyThrows
    public void testBlockPathfinder() {
        Phobot phobot = TestPhobot.createNewTestPhobot();
        phobot.getAntiCheat().getStrictDirection().setValue(StrictDirection.Type.Vanilla);
        BlockPathfinder blockPathfinder = new BlockPathfinder();
        BlockPlacer blockPlacer = phobot.getBlockPlacer();
        ChecksBlockPlacingValidity validity = () -> blockPlacer;
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            BlockStateLevel.Delegating level = new BlockStateLevel.Delegating(clientLevel);
            level.getMap().put(BlockPos.ZERO, Blocks.OBSIDIAN.defaultBlockState());
            var player = new FakePlayer(level);
            Field playerInfo = AbstractClientPlayer.class.getDeclaredField("playerInfo");
            playerInfo.setAccessible(true);
            playerInfo.set(player, new PlayerInfo(player.getGameProfile(), false));
            player.setPos(new Vec3(1.5, -1, 1.5));
            player.setId(1337);
            level.addEntity(player);

            List<BlockPos> path = blockPathfinder.getShortestPath(new BlockPos(0, 2, 0), Blocks.OBSIDIAN, player, level, 2, validity, blockPlacer);
            assertEquals(3, path.size());
            assertEquals(BlockPos.ZERO, path.get(0));
            assertEquals(new BlockPos(0, 1, 0), path.get(1));
            assertEquals(new BlockPos(0, 2, 0), path.get(2));
            // strict direction prevents us from placing above blocks from underneath
            phobot.getAntiCheat().getStrictDirection().setValue(StrictDirection.Type.NCP);
            path = blockPathfinder.getShortestPath(new BlockPos(0, 2, 0), Blocks.OBSIDIAN, player, level, 2, validity, blockPlacer);
            assertTrue(path.isEmpty());
            // player is above so should pass all strict direction checks
            player.setPos(new Vec3(0, 3, 0));
            path = blockPathfinder.getShortestPath(new BlockPos(0, 2, 0), Blocks.OBSIDIAN, player, level, 2, validity, blockPlacer);
            assertEquals(3, path.size());
            assertEquals(BlockPos.ZERO, path.get(0));
            assertEquals(new BlockPos(0, 1, 0), path.get(1));
            assertEquals(new BlockPos(0, 2, 0), path.get(2));
            // maxCost is 1
            path = blockPathfinder.getShortestPath(new BlockPos(0, 2, 0), Blocks.OBSIDIAN, player, level, 1, validity, blockPlacer);
            assertTrue(path.isEmpty());
            // with maxCost 1 we should still find a path to a block that can be placed without helping blocks
            path = blockPathfinder.getShortestPath(new BlockPos(0, 1, 0), Blocks.OBSIDIAN, player, level, 1, validity, blockPlacer);
            assertEquals(2, path.size());
            assertEquals(BlockPos.ZERO, path.get(0));
            assertEquals(new BlockPos(0, 1, 0), path.get(1));
            // with maxCost 0 we should never find a path.
            path = blockPathfinder.getShortestPath(new BlockPos(0, 1, 0), Blocks.OBSIDIAN, player, level, 0, validity, blockPlacer);
            assertTrue(path.isEmpty());
        }
    }

}
