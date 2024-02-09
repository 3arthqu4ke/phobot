package me.earth.phobot.util.math;

import lombok.SneakyThrows;
import me.earth.phobot.TestUtil;
import me.earth.phobot.util.player.FakePlayer;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RaytraceUtilTest {
    @Test
    @SneakyThrows
    public void testRaytrace() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            BlockStateLevel.Delegating level = new BlockStateLevel.Delegating(clientLevel);
            level.getMap().put(BlockPos.ZERO, Blocks.OBSIDIAN.defaultBlockState());
            level.getMap().put(new BlockPos(0, 1, 0), Blocks.OBSIDIAN.defaultBlockState());
            level.getMap().put(new BlockPos(0, 2, 0), Blocks.OBSIDIAN.defaultBlockState());
            level.getMap().put(new BlockPos(0, 3, 0), Blocks.AIR.defaultBlockState());
            level.getMap().put(new BlockPos(0, 4, 0), Blocks.AIR.defaultBlockState());
            level.getMap().put(new BlockPos(0, 5, 0), Blocks.AIR.defaultBlockState());
            level.getMap().put(new BlockPos(0, 6, 0), Blocks.AIR.defaultBlockState());

            Entity entity = new FakePlayer(level);
            entity.setYRot(0.0f);
            entity.setXRot(RotationUtil.X_ROT_DOWN);
            entity.setPos(0.5, 4.0, 0.5);

            assertNotNull(RaytraceUtil.raytrace(entity, level, new BlockPos(0, 1, 0), null, true));
            assertNotNull(RaytraceUtil.raytrace(entity, level, new BlockPos(0, 1, 0), Direction.UP, true));
            assertNotNull(RaytraceUtil.raytrace(entity, level, new BlockPos(0, 1, 0), Direction.DOWN, true));
            assertNull(RaytraceUtil.raytrace(entity, level, new BlockPos(0, 1, 0), Direction.DOWN, false));
            for (Direction direction : PositionUtil.HORIZONTAL_DIRECTIONS) {
                assertNull(RaytraceUtil.raytrace(entity, level, new BlockPos(0, 1, 0), direction, true));
            }

            var placeTargetResult = RaytraceUtil.raytraceToPlaceTarget(entity, level, new BlockPos(0, 3, 0), (p,d) -> true, true);
            assertNotNull(placeTargetResult);
            assertEquals(new BlockPos(0, 2, 0), placeTargetResult.getBlockPos());
            assertEquals(Direction.UP, placeTargetResult.getDirection());

            FakePlayer otherPlayer = new FakePlayer(level);
            otherPlayer.setId(1337);
            level.addEntity(otherPlayer);
            otherPlayer.setPos(new Vec3(0.5, 0.0, 0.5));
            assertNotNull(RaytraceUtil.raytraceEntities(entity, 8.0, e -> e.equals(otherPlayer)));
            assertNotNull(RaytraceUtil.raytraceEntity(entity, otherPlayer));
            otherPlayer.setPos(new Vec3(2.0, 0.0, 2.0));
            assertNull(RaytraceUtil.raytraceEntities(entity, 10.0, e -> e.equals(otherPlayer)));
            assertNull(RaytraceUtil.raytraceEntity(entity, otherPlayer));
        }
    }

}
