package me.earth.phobot.util.math;

import lombok.SneakyThrows;
import me.earth.phobot.TestUtil;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManagerTest;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PositionUtilTest {
    @Test
    @SneakyThrows
    public void testGetYAtPosition() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            BlockStateLevel.Delegating level = new BlockStateLevel.Delegating(clientLevel);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            pos.set(0, 10, 0);
            assertEquals(10.0, PositionUtil.getMaxYAtPosition(pos, level));

            pos.set(0, 5, 0);
            level.getMap().put(pos.immutable(), Blocks.OBSIDIAN.defaultBlockState());
            assertEquals(6.0, PositionUtil.getMaxYAtPosition(pos, level));

            pos.set(0, 1, 0);
            level.getMap().put(pos.immutable(), Blocks.BLACK_CARPET.defaultBlockState());
            assertEquals(1.0625, PositionUtil.getMaxYAtPosition(pos, level));

            pos.set(0, 0, 0);
            level.getMap().put(pos.immutable(), Blocks.ACACIA_FENCE.defaultBlockState());
            pos.set(0, 1, 0);
            assertEquals(1.5, PositionUtil.getMaxYAtPosition(pos, level));

            var meshManager = NavigationMeshManagerTest.createNavigationMeshManager();
            NavigationMeshManagerTest.setupMesh(level, meshManager);
            assertEquals(2.0, PositionUtil.getMaxYAtPosition(new BlockPos.MutableBlockPos(0, 2, 0), level));
            assertEquals(1.5, PositionUtil.getMaxYAtPosition(new BlockPos.MutableBlockPos(0, 1, 0), level));
        }
    }

}
