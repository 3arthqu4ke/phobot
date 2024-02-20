package me.earth.phobot;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.internal.UnsafeAllocator;
import com.mojang.serialization.Lifecycle;
import lombok.Data;
import lombok.SneakyThrows;
import me.earth.phobot.util.world.BlockStateLevel;
import me.earth.phobot.util.world.DelegatingClientLevel;
import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

@SuppressWarnings("NullableProblems")
public class TestUtil {
    public static void bootstrap() {
        SharedConstants.setVersion(DetectedVersion.BUILT_IN);
        Bootstrap.bootStrap();
    }

    @SneakyThrows
    public static <T> T allocateInstance(Class<? extends T> tClass) {
        return UnsafeAllocator.INSTANCE.newInstance(tClass);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static ClientLevel createClientLevel() {
        bootstrap();
        Holder.Reference<DimensionType> holder = Holder.Reference.createStandAlone(new HolderOwner<>(){}, allocateInstance(ResourceKey.class));
        Field value = Holder.Reference.class.getDeclaredField("value");
        value.setAccessible(true);
        value.set(holder, getDimensionTypes().get(0));
        ResourceKey<Level> resourceKey = allocateInstance(ResourceKey.class);
        RegistryAccess registryAccess = new RegistryAccess() {
            @Override
            public <E> Optional<Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> resourceKey) {
                return Optional.of(new DummyRegistry<>());
            }

            @Override
            public Stream<RegistryEntry<?>> registries() {
                return Stream.empty();
            }
        };

        //noinspection DataFlowIssue
        return new ClientLevel(DelegatingClientLevel.DummyClientPacketListener.create(registryAccess),
                new ClientLevel.ClientLevelData(Difficulty.EASY, false, false),
                resourceKey, holder, 0, 0, () -> InactiveProfiler.INSTANCE, null, false, 0);
    }

    public static BlockStateLevel.Delegating setupLevel(ClientLevel clientLevel, Consumer<BlockStateLevel.Delegating> consumer) {
        BlockStateLevel.Delegating level = new BlockStateLevel.Delegating(clientLevel) {
            @Override
            public boolean hasChunkAt(BlockPos pos) {
                return true;
            }
        };

        consumer.accept(level);
        return level;
    }

    public static BlockStateLevel.Delegating setupLevelFromJson(ClientLevel clientLevel, String json) {
        bootstrap();
        return setupLevel(clientLevel, level -> {
            try (InputStream is = Objects.requireNonNull(TestUtil.class.getClassLoader().getResourceAsStream(json), json + " was null!")) {
                Type listType = new TypeToken<ArrayList<BlockWithPos>>(){}.getType();
                List<BlockWithPos> blocks = new Gson().fromJson(new InputStreamReader(is), listType);
                for (BlockWithPos blockWithPos : blocks) {
                    Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(blockWithPos.getBlock()));
                    level.getMap().put(new BlockPos(blockWithPos.getX(), blockWithPos.getY(), blockWithPos.getZ()), block.defaultBlockState());
                }
            } catch (IOException e) {
                throw new IOError(e);
            }
        });
    }

    private static List<DimensionType> getDimensionTypes() {
        List<DimensionType> types = new ArrayList<>(4);
        DimensionTypes.bootstrap(new BootstapContext<>() {
            @Override
            public Holder.Reference<DimensionType> register(ResourceKey<DimensionType> resourceKey, DimensionType object, Lifecycle lifecycle) {
                types.add(object);
                return null;
            }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> resourceKey) {
                return null;
            }
        });

        return types;
    }

    @Data
    @SuppressWarnings("ClassCanBeRecord")
    private static class BlockWithPos {
        private final String block;
        private final int x;
        private final int y;
        private final int z;
    }

}
