package me.earth.phobot;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

@SuppressWarnings({"NullableProblems", "DataFlowIssue"})
public class DummyRegistry<T> implements Registry<T> {
    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return null;
    }

    @Nullable
    @Override
    public ResourceLocation getKey(T object) {
        return null;
    }

    @Override
    public Holder.Reference<T> getHolderOrThrow(ResourceKey<T> resourceKey) {
        return null;
    }

    @Override
    public Optional<ResourceKey<T>> getResourceKey(T object) {
        return Optional.empty();
    }

    @Override
    public int getId(@Nullable T object) {
        return 0;
    }

    @Nullable
    @Override
    public T byId(int i) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Nullable
    @Override
    public T get(@Nullable ResourceKey<T> resourceKey) {
        return null;
    }

    @Nullable
    @Override
    public T get(@Nullable ResourceLocation resourceLocation) {
        return null;
    }

    @Override
    public Lifecycle lifecycle(T object) {
        return null;
    }

    @Override
    public Lifecycle registryLifecycle() {
        return null;
    }

    @Override
    public Set<ResourceLocation> keySet() {
        return null;
    }

    @Override
    public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        return null;
    }

    @Override
    public Set<ResourceKey<T>> registryKeySet() {
        return null;
    }

    @Override
    public Optional<Holder.Reference<T>> getRandom(RandomSource randomSource) {
        return Optional.empty();
    }

    @Override
    public boolean containsKey(ResourceLocation resourceLocation) {
        return false;
    }

    @Override
    public boolean containsKey(ResourceKey<T> resourceKey) {
        return false;
    }

    @Override
    public Registry<T> freeze() {
        return null;
    }

    @Override
    public Holder.Reference<T> createIntrusiveHolder(T object) {
        return null;
    }

    @Override
    public Optional<Holder.Reference<T>> getHolder(int i) {
        return Optional.empty();
    }

    @Override
    public Optional<Holder.Reference<T>> getHolder(ResourceKey<T> resourceKey) {
        return Optional.empty();
    }

    @Override
    public Holder<T> wrapAsHolder(T object) {
        return null;
    }

    @Override
    public Stream<Holder.Reference<T>> holders() {
        return null;
    }

    @Override
    public Optional<HolderSet.Named<T>> getTag(TagKey<T> tagKey) {
        return Optional.empty();
    }

    @Override
    public HolderSet.Named<T> getOrCreateTag(TagKey<T> tagKey) {
        return null;
    }

    @Override
    public Stream<Pair<TagKey<T>, HolderSet.Named<T>>> getTags() {
        return null;
    }

    @Override
    public Stream<TagKey<T>> getTagNames() {
        return null;
    }

    @Override
    public void resetTags() {

    }

    @Override
    public void bindTags(Map<TagKey<T>, List<Holder<T>>> map) {

    }

    @Override
    public HolderOwner<T> holderOwner() {
        return null;
    }

    @Override
    public HolderLookup.RegistryLookup<T> asLookup() {
        return null;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return null;
    }
}
