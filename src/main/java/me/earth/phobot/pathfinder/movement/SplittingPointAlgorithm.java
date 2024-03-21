package me.earth.phobot.pathfinder.movement;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

@Slf4j
@Getter
public class SplittingPointAlgorithm extends MovementPathfindingAlgorithm {
    Set<MoveInfo> angles = new HashSet<>();
    BiConsumer<MovementNode, Boolean> moveConsumer = (node, reachedMeshNode) -> {};
    Runnable preMoveTick = () -> {};

    protected SplittingPointAlgorithm(MovementPathfindingAlgorithm alg) {
        super(alg.getPhobot(), alg.getLevel(), alg.getPath(), alg.getPlayer(), alg.getStart(), alg.getGoal());
    }

    @Override
    protected boolean moveTowards(double x, double y, double z, boolean isGoal, int targetNodeIndex, boolean strafe) {
        int yaw = (int) ((Math.atan2(z - currentMove.z(), x - currentMove.x()) * 180.0 / Math.PI) - 90.0f);
        MoveInfo moveInfo = new MoveInfo(yaw, strafe);
        if (angles.add(moveInfo)) {
            return super.moveTowards(x, y, z, isGoal, targetNodeIndex, strafe);
        }

        return false;
    }

    /* Would have thought this is faster, but it isn't!

    @Override
    protected boolean bunnyHopTowardsTarget(int start, int end, boolean increment) {
        return super.bunnyHopTowardsTarget(end + 1, start + 1, !increment);
    }*/

    @Override
    protected @Nullable Boolean moveTick() {
        preMoveTick.run();
        return moveTickWithoutPreMoveTick();
    }

    public @Nullable Boolean moveTickWithoutPreMoveTick() {
        MovementNode current = currentJump;
        Boolean result = false;
        while (current == currentJump) {
            result = super.moveTick();
            if (result != null) {
                break;
            }
        }

        if (result == null) {
            moveConsumer.accept(currentJump, false);
        } else if (result) {
            moveConsumer.accept(currentJump, true);
        }

        return false;
    }

    public record MoveInfo(int angle, boolean strafe) {

    }

}
