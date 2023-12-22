package me.earth.phobot.util.world;

import lombok.SneakyThrows;
import me.earth.phobot.TestUtil;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManagerTest;
import me.earth.phobot.util.player.MovementPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BlockStateLevelTest {
    @Test
    @SneakyThrows
    public void testDelegatingBlockStateLevelCollisionAndBlockGetterProperties() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            BlockStateLevel.Delegating delegating = new BlockStateLevel.Delegating(clientLevel);
            NavigationMeshManagerTest.setupBlockStateLevel(delegating);
            // clientLevel only has air, clip should go right through
            assertEquals(new BlockPos(10, -1, 10), clientLevel.clip(new ClipContext(new Vec3(10.0, 3.0, 10.0), new Vec3(10.0, -1, 10.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, new MovementPlayer(clientLevel))).getBlockPos());
            // delegating has obsidian at 10, 0, 10, clip should end there
            assertEquals(new BlockPos(10, 0, 10), delegating.clip(new ClipContext(new Vec3(10.0, 3.0, 10.0), new Vec3(10.0, -1, 10.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, new MovementPlayer(delegating))).getBlockPos());

            assertFalse(clientLevel.getBlockCollisions(null, new AABB(9, -1, 9, 10, 1, 10)).iterator().hasNext());
            assertTrue(delegating.getBlockCollisions(null, new AABB(9, -1, 9, 10, 1, 10)).iterator().hasNext());

            assertTrue(clientLevel.noCollision(new MovementPlayer(clientLevel), new AABB(9, -1, 9, 10, 1, 10)));
            assertFalse(delegating.noCollision(new MovementPlayer(delegating), new AABB(9, -1, 9, 10, 1, 10)));
        }
    }

}
