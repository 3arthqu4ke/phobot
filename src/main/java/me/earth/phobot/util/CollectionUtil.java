package me.earth.phobot.util;

import lombok.experimental.UtilityClass;

import java.util.Queue;
import java.util.function.Consumer;

@UtilityClass
public class CollectionUtil {
    public static void emptyQueue(Queue<Runnable> queue) {
        emptyQueue(queue, Runnable::run);
    }

    public static <T> void emptyQueue(Queue<T> queue, Consumer<T> onPoll) {
        while (!queue.isEmpty()) {
            T polled = queue.poll();
            if (polled != null) {
                onPoll.accept(polled);
            }
        }
    }

}
