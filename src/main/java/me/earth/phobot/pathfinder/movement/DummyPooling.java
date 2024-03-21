package me.earth.phobot.pathfinder.movement;

import me.earth.phobot.pathfinder.algorithm.pooling.NodeParallelizationPooling;
import me.earth.phobot.pathfinder.util.Cancellation;

public class DummyPooling extends NodeParallelizationPooling {
    public static final DummyPooling INSTANCE = new DummyPooling();
    private final DummyReference dummyReference = new DummyReference();

    public DummyPooling() {
        super(1);
    }

    @Override
    public NodeParallelizationPooling.PoolReference requestIndex(Cancellation cancellation) {
        return INSTANCE.dummyReference;
    }

    public final class DummyReference extends NodeParallelizationPooling.PoolReference {
        public DummyReference() {
            super(0, 0);
        }

        @Override
        public void close() {

        }
    }

}
