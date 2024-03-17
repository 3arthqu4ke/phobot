package me.earth.phobot.pathfinder.algorithm.pooling;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class PooledCameFromMap<N extends PoolNode<N>> implements Map<N, @Nullable N> {
    private final int index;

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable N get(Object key) {
        return ((PoolNode<N>) key).getCameFrom(index);
    }

    @Override
    public @Nullable N put(N key, @Nullable N value) {
        key.setCameFrom(index, value);
        return null;
    }

    // Unsupported

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public @Nullable N remove(Object key) {
        return null;
    }

    @Override
    public void putAll(@NotNull Map<? extends N, ? extends @Nullable N> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        // NOP
    }

    @NotNull
    @Override
    public Set<N> keySet() {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public Collection<@Nullable N> values() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Set<Entry<N, @Nullable N>> entrySet() {
        return Collections.emptySet();
    }

}
