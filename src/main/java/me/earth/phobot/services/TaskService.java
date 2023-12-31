package me.earth.phobot.services;

import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.module.Module;
import me.earth.pingbypass.commons.event.loop.GameloopEvent;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.PriorityQueue;
import java.util.Queue;

public class TaskService extends SubscriberImpl {
    private final Queue<Task> tasks = new PriorityQueue<>();
    private final Minecraft mc;

    public TaskService(Minecraft mc) {
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
        long time = TimeUtil.getMillis() + ms;
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
