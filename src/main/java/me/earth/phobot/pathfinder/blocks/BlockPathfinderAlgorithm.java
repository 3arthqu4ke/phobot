package me.earth.phobot.pathfinder.blocks;

import me.earth.phobot.pathfinder.algorithm.Dijkstra;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;

public class BlockPathfinderAlgorithm extends Dijkstra<BlockNode> {
    private final BiPredicate<BlockPos, BlockPos> validityCheck;
    private final BiPredicate<BlockPos, @Nullable BlockPos> goalCheck;
    private final BlockPos center;
    private final int maxCost;

    public BlockPathfinderAlgorithm(BlockNode start, BiPredicate<BlockPos, BlockPos> validityCheck, BiPredicate<BlockPos, @Nullable BlockPos> goalCheck, BlockPos center, int maxCost) {
        super(start, null);
        this.validityCheck = validityCheck;
        this.goalCheck = goalCheck;
        this.center = center;
        this.maxCost = maxCost;
    }

    @Override
    protected void evaluate(BlockNode current, BlockNode neighbour) {
        double tentative_gScore = gScore.getOrDefault(current, Double.POSITIVE_INFINITY) + getCost(current, neighbour);
        if (tentative_gScore < gScore.getOrDefault(neighbour, Double.POSITIVE_INFINITY) && tentative_gScore <= maxCost) {
            addDijkstra(current, neighbour, tentative_gScore);
        }
    }

    @Override
    protected boolean isValid(BlockNode current, BlockNode neighbour) {
        neighbour.setToOffsetFromCenter(center);
        return isGoal(neighbour, current) || validityCheck.test(current.getCurrent(), neighbour.getCurrent());
    }

    @Override
    protected boolean isGoal(BlockNode current, @Nullable BlockNode from) {
        return goalCheck.test(current.getCurrent(), from == null ? null : from.getCurrent());
    }

}
