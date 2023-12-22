package me.earth.phobot.pathfinder.util;

import org.jetbrains.annotations.Nullable;

import java.util.SortedSet;

// TODO: I have not yet achieved good results with a binary heap but maybe I am not seeing something
public interface OpenSet<N> {
    @Nullable N removeFirst();

    void add(N n);

    void update(N n, Runnable actionThatUpdatesScore);

    int size();

    void clear();

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

            @Override
            public void clear() {
                sortedSet.clear();
            }
        };
    }

}
