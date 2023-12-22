package me.earth.phobot;

import com.mojang.serialization.Lifecycle;
import lombok.SneakyThrows;
import me.earth.phobot.util.world.DelegatingClientLevel;
import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.*;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("NullableProblems")
public class TestUtil {
    public static void bootstrap() {
        SharedConstants.setVersion(DetectedVersion.BUILT_IN);
        Bootstrap.bootStrap();
    }

    @SneakyThrows
    public static Unsafe getUnsafe() {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static ClientLevel createClientLevel() {
        bootstrap();
        Holder.Reference<DimensionType> holder = Holder.Reference.createStandAlone(new HolderOwner<>(){}, (ResourceKey<DimensionType>) TestUtil.getUnsafe().allocateInstance(ResourceKey.class));
        Field value = Holder.Reference.class.getDeclaredField("value");
        value.setAccessible(true);
        value.set(holder, getDimensionTypes().get(0));
        ResourceKey<Level> resourceKey = (ResourceKey<Level>) TestUtil.getUnsafe().allocateInstance(ResourceKey.class);
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

        return new ClientLevel(new DelegatingClientLevel.DummyClientPacketListener(registryAccess),
                new ClientLevel.ClientLevelData(Difficulty.EASY, false, false),
                resourceKey, holder, 0, 0, () -> InactiveProfiler.INSTANCE, null, false, 0);
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

}
