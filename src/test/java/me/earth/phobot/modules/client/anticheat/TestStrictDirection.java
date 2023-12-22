package me.earth.phobot.modules.client.anticheat;

import lombok.SneakyThrows;
import me.earth.phobot.TestUtil;
import me.earth.phobot.util.player.FakePlayer;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestStrictDirection {
    @Test
    @SneakyThrows
    public void testStrictDirectionCheck() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            BlockStateLevel.Delegating level = new BlockStateLevel.Delegating(clientLevel);
            level.getMap().put(BlockPos.ZERO, Blocks.OBSIDIAN.defaultBlockState());
            level.getMap().put(new BlockPos(1, 1, 0), Blocks.OBSIDIAN.defaultBlockState());
            level.getMap().put(new BlockPos(-1, 1, 0), Blocks.OBSIDIAN.defaultBlockState());
            level.getMap().put(new BlockPos(1, 2, 0), Blocks.OBSIDIAN.defaultBlockState());

            Player player = new FakePlayer(level);
            player.setPos(0.5, 1.0, 0.5);

            assertEquals(Direction.UP, NCP.INSTANCE.getStrictDirection(new BlockPos(1, 2, 0), player, level));
            assertTrue(NCP.INSTANCE.strictDirectionCheck(new BlockPos(1, 2, 0), Direction.UP, level, player));
            assertTrue(NCP.INSTANCE.strictDirectionCheck(new BlockPos(-1, 1, 0), Direction.UP, level, player));
            assertFalse(NCP.INSTANCE.strictDirectionCheck(new BlockPos(-1, 1, 0), Direction.DOWN, level, player));

            // down is first
            assertEquals(Direction.DOWN, Grim.INSTANCE.getStrictDirection(new BlockPos(1, 2, 0), player, level));
            assertEquals(Direction.UP, Grim.INSTANCE.getStrictDirection(new BlockPos(1, 1, 0), player, level));
            assertFalse(Grim.INSTANCE.strictDirectionCheck(new BlockPos(1, 2, 0), Direction.UP, level, player));
            assertTrue(Grim.INSTANCE.strictDirectionCheck(new BlockPos(-1, 1, 0), Direction.UP, level, player));
            assertFalse(Grim.INSTANCE.strictDirectionCheck(new BlockPos(-1, 1, 0), Direction.DOWN, level, player));
        }
    }

}
