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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
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
import java.util.function.BiConsumer;
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
        return setupLevel(clientLevel, level -> setupBlockStateLevelFromJson(level, json, BlockPos.ZERO));
    }

    public static void setupBlockStateLevelFromJson(BlockStateLevel.Delegating level, String json, BlockPos off) {
        setupBlockStateLevelFromJson(json, (pos, block) ->
                level.getMap().put(new BlockPos(pos.getX() + off.getX(), pos.getY() + off.getY(), pos.getZ() + off.getZ()), block.defaultBlockState()));
    }

    public static void setupBlockStateLevelFromJson(String json, BiConsumer<BlockWithPos, Block> action) {
        try (InputStream is = Objects.requireNonNull(TestUtil.class.getClassLoader().getResourceAsStream(json), json + " was null!")) {
            Type listType = new TypeToken<ArrayList<BlockWithPos>>(){}.getType();
            List<BlockWithPos> blocks = new Gson().fromJson(new InputStreamReader(is), listType);
            for (BlockWithPos pos : blocks) {
                Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(pos.getBlock()));
                action.accept(pos, block);
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static Minecraft createMinecraft(ClientLevel level) {
        return createMinecraft(allocateInstance(LocalPlayer.class), level, allocateInstance(MultiPlayerGameMode.class));
    }

    public static Minecraft createMinecraft(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        Minecraft mc = allocateInstance(Minecraft.class);
        mc.player = player;
        mc.level = level;
        mc.gameMode = gameMode;
        return mc;
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
    public static class BlockWithPos {
        private final String block;
        private final int x;
        private final int y;
        private final int z;
    }

}
