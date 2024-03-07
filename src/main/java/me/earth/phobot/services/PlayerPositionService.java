package me.earth.phobot.services;

import lombok.Getter;
import lombok.Synchronized;
import me.earth.pingbypass.api.event.SubscriberImpl;

import java.util.ArrayDeque;
import java.util.Deque;

@Getter
public class PlayerPositionService extends SubscriberImpl {
    protected final Deque<PlayerPosition> positions;
    protected final int threshold;

    protected volatile PlayerPosition position = new PlayerPosition();

    public PlayerPositionService(int threshold) {
        this.positions = new ArrayDeque<>(threshold);
        this.threshold = threshold;
    }

    @Synchronized
    protected synchronized void addToPositions(PlayerPosition position, Deque<PlayerPosition> positions) {
        if (positions.size() >= getThreshold()) {
            positions.removeLast();
        }

        positions.addFirst(position);
    }

}
