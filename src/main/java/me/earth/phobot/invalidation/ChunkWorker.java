package me.earth.phobot.invalidation;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.util.CollectionUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ChunkWorker {
    private final Deque<Runnable> postWorkTasks = new ArrayDeque<>();
    private final AtomicInteger atomicVersion = new AtomicInteger();
    private final AtomicBoolean working = new AtomicBoolean();

    /**
     * Sets the working state of this chunk. If it is working any task added via {@link #addTask(Runnable)} will
     * not be executed until this method is called again for the working state {@code false}.
     * When set to {@code true} all tasks added before will be cleared.
     * This method is only to be called from Minecrafts main thread!
     *
     * @param working whether this chunk is currently being processed by a task or not.
     */
    public void setWorking(boolean working) {
        synchronized (this.working) {
            this.working.set(working);
            if (working) {
               postWorkTasks.clear();
            } else {
                CollectionUtil.emptyQueue(postWorkTasks);
            }
        }
    }

    public void addTask(Runnable task) {
        synchronized (this.working) {
            if (isWorking()) {
                postWorkTasks.add(task);
            } else {
                task.run();
            }
        }
    }

    public boolean isWorking() {
        return working.get();
    }

    public int getVersion() {
        return atomicVersion.get();
    }

    // TODO: for mass invalidation, a set of everything in this chunk would be just as good?
    public void incrementVersion() {
        atomicVersion.incrementAndGet();
    }

}
