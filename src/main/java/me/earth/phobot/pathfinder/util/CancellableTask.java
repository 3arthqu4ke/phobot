package me.earth.phobot.pathfinder.util;

/**
 * A task that can be cancelled through a given {@link Cancellation}.
 *
 * @param <T> the type of result returned by {@link #run(Cancellation)}
 */
@FunctionalInterface
public interface CancellableTask<T> {
    T run(Cancellation cancellation);

}
