package me.earth.phobot.services;

import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.loop.GameloopEvent;
import me.earth.pingbypass.api.module.Module;
import net.minecraft.util.thread.BlockableEventLoop;
import org.jetbrains.annotations.NotNull;

import java.util.PriorityQueue;
import java.util.Queue;

public class TaskService extends SubscriberImpl {
    private final Queue<Task> tasks = new PriorityQueue<>();
    private final BlockableEventLoop<Runnable> mc;

    public TaskService(BlockableEventLoop<Runnable> mc) {
        this.mc = mc;
        listen(new Listener<GameloopEvent>() {
            @Override
            public void onEvent(GameloopEvent event) {
                while (!tasks.isEmpty()) {
                    Task first = tasks.peek();
                    if (first != null && TimeUtil.isTimeStampOlderThan(first.timeStamp, 0)) {
                        tasks.poll();
                        first.runnable.run();
                    } else {
                        return;
                    }
                }
            }
        });
    }

    public void addTaskToBeExecutedIn(long ms, Runnable runnable) {
        long time = TimeUtil.getMillis() + ms; // <- not protected against overflows but whatever
        mc.submit(() -> tasks.add(new Task(runnable, time)));
    }

    public void addTaskToBeExecutedIn(long ms, Module module, Runnable runnable) {
        addTaskToBeExecutedIn(ms, () -> {
            if (module.isEnabled()) {
                runnable.run();
            }
        });
    }

    private record Task(Runnable runnable, long timeStamp) implements Comparable<Task> {
        @Override
        public int compareTo(@NotNull TaskService.Task o) {
            return Long.compare(this.timeStamp, o.timeStamp);
        }
    }

}
