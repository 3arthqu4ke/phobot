package me.earth.phobot.util.world;

import lombok.SneakyThrows;
import me.earth.phobot.TestUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import net.minecraft.world.level.entity.Visibility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests that the constructors of {@link EntityCopyingLevel} etc. run without Exceptions.
 */
public class DelegatingLevelTests {
    @Test
    @SneakyThrows
    public void testNewDelegatingClientLevel() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            DelegatingClientLevel level = new DelegatingClientLevel(clientLevel) {};
            assertSame(InactiveProfiler.INSTANCE, level.getProfiler());
        }
    }

    @Test
    @SneakyThrows
    public void testNewSafeLevel() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            EntityCopyingLevel level = new EntityCopyingLevel(clientLevel, new LevelEntityGetterAdapter<>(new EntityLookup<>(), new EntitySectionStorage<>(Entity.class, l -> Visibility.HIDDEN)));
            assertSame(InactiveProfiler.INSTANCE, level.getProfiler());
        }
    }

    @Test
    @SneakyThrows
    public void testCustomBlockStateLevelDelegating() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            BlockStateLevel.Delegating level = new BlockStateLevel.Delegating(clientLevel);
            assertSame(InactiveProfiler.INSTANCE, level.getProfiler());
            BlockState blockState = TestUtil.allocateInstance(BlockState.class);
            level.getMap().put(BlockPos.ZERO, blockState);
            assertSame(blockState, level.getBlockState(BlockPos.ZERO));
        }
    }

}
