package me.earth.phobot.invalidation;

public interface CanBeInvalidated {
    boolean isValid();

    void invalidate();

}
