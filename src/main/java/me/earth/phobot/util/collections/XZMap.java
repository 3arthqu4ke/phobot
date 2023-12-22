package me.earth.phobot.util.collections;

import lombok.experimental.Delegate;
import net.minecraft.core.Vec3i;

import java.util.Map;
import java.util.function.Function;

/**
 * A map with 2-dimensional keys based on the x and z values of {@link Vec3i}.
 *
 * @param <T> the type of value stored in this map.
 */
public class XZMap<T> implements Map<Long, T> {
    @Delegate
    private final Map<Long, T> map;

    public XZMap(Map<Long, T> map) {
        this.map = map;
    }

    public T get(int x, int z) {
        return map.get(getKey(x, z));
    }

    public T getOrDefault(int x, int z, T defaultValue) {
        return map.getOrDefault(getKey(x, z), defaultValue);
    }

    public T getOrDefault(Vec3i key, T defaultValue) {
        return getOrDefault(key.getX(), key.getZ(), defaultValue);
    }

    public T get(Vec3i key) {
        return get(key.getX(), key.getZ());
    }

    public T put(int x, int z, T value) {
        return map.put(getKey(x, z), value);
    }

    public T put(Vec3i key, T value) {
        return put(key.getX(), key.getZ(), value);
    }

    public T computeIfAbsent(int x, int z, Function<? super Long, ? extends T> mappingFunction) {
        return map.computeIfAbsent(getKey(x, z), mappingFunction);
    }

    public T computeIfAbsent(Vec3i key, Function<? super Long, ? extends T> mappingFunction) {
        return computeIfAbsent(key.getX(), key.getZ(), mappingFunction);
    }

    public T remove(int x, int z) {
        return map.remove(getKey(x, z));
    }

    public T remove(Vec3i key) {
        return remove(key.getX(), key.getZ());
    }

    public long getKey(int x, int z) {
        return (((long) x) << 32) | (z & 0xffffffffL);
    }

}
