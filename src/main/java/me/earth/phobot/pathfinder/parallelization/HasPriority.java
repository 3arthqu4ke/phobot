package me.earth.phobot.pathfinder.parallelization;

import org.jetbrains.annotations.NotNull;

/**
 * An object with a priority. The higher the priority the value, the more important the object.
 */
@FunctionalInterface
public interface HasPriority extends Comparable<HasPriority> {
    /**
     * @return the priority of this object. A higher value means that this object is more important.
     */
    int getPriority();

    @Override
    default int compareTo(@NotNull HasPriority o) {
        // higher priority is first
        return Integer.compare(o.getPriority(), this.getPriority());
    }

    /**
     * @param o the object to check.
     * @return {@code true} if this object has a higher priority than the given one.
     */
    default boolean isMoreImportantThan(@NotNull HasPriority o) {
        return compareTo(o) < 0;
    }

}
