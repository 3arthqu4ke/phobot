package me.earth.phobot.pathfinder.util;

import lombok.Setter;
import lombok.ToString;

/**
 * Represents the cancellation of some ongoing task.
 */
@Setter
@ToString
@SuppressWarnings("LombokGetterMayBeUsed")
public class Cancellation {
    public static final Cancellation UNCANCELLABLE = new Cancellation() {
        @Override
        public boolean isCancelled() {
            return false;
        }
    };

    protected boolean cancelled;

    public void init() {
        // To initialize the current Thread if we are working with interrupts
    }

    public boolean isCancelled() {
        return cancelled;
    }

    @ToString(callSuper = true)
    public static class ThreadInterrupted extends Cancellation {
        private Thread thread = Thread.currentThread();

        @Override
        public void init() {
            thread = Thread.currentThread();
        }

        @Override
        public boolean isCancelled() {
            return super.isCancelled() || thread.isInterrupted();
        }
    }

}
