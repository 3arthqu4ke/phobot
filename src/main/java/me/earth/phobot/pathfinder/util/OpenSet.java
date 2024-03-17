package me.earth.phobot.pathfinder.util;

import me.earth.phobot.pathfinder.algorithm.Algorithm;
import org.jetbrains.annotations.Nullable;

import java.util.SortedSet;

// TODO: main issue with a binary heap is that to make updating fast, each element needs to hold a reference of the index its stored at.
//  That makes parallel pathfinding on the same MeshNodes problematic.
/**
 * The OpenSet used by {@link Algorithm}s.
 * @param <N> the type of elements in this set.
 */
public interface OpenSet<N> {
    @Nullable N removeFirst();

    void add(N n);

    void update(N n, Runnable actionThatUpdatesScore);

    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    static <T> OpenSet<T> wrap(SortedSet<T> sortedSet) {
        return new OpenSet<>() {
            @Override
            public @Nullable T removeFirst() {
                T first = sortedSet.first();
                sortedSet.remove(first);
                return first;
            }

            @Override
            public void add(T t) {
                sortedSet.add(t);
            }

            @Override
            public void update(T t, Runnable actionThatUpdatesScore) {
                sortedSet.remove(t);
                actionThatUpdatesScore.run();
                sortedSet.add(t);
            }

            @Override
            public int size() {
                return sortedSet.size();
            }
        };
    }

}
