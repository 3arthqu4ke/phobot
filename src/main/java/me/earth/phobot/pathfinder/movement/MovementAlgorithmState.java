package me.earth.phobot.pathfinder.movement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.world.phys.Vec3;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MovementAlgorithmState {
    double x;
    double y;
    double z;
    boolean isGoal;
    int targetNodeIndex;
    boolean strafe;

    int walkTicksLeft;
    Vec3 initialDelta;
    MovementNode firstJump;
    MovementNode currentJump;
    MovementNode lastJump;
    MovementNode tenthLast;
    double prevDistance;
    int movedAwayCount;
    int sameCount;

    // --------- properties that do not belong to the moveTowards method, but are more global within the algorithm ------ //

    MovementNode currentRender;
    MovementNode currentMove;
    /**
     * It can happen that during an update we hit the next MeshNode without even having to produce a new MovementNode.
     * Especially at the start, where we want to get to MeshNode 0, this is a problem. This variable tracks the
     * number of such updates and then updates the targetNodeIndex accordingly, so we target further MeshNodes.
     */
    int noMoveUpdates;
    /**
     * If > 0, we will walk for this amount of ticks and not bunnyhop.
     */
    int walkTicks;

    public void initializeMove(double x, double y, double z, boolean isGoal, int targetNodeIndex, boolean strafe) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.isGoal = isGoal;
        this.targetNodeIndex = targetNodeIndex;
        this.strafe = strafe;
        this.walkTicksLeft = walkTicks;

        this.initialDelta = new Vec3(x - currentMove.getX(), 0.0, z - currentMove.getZ()).normalize();
        this.firstJump = null;
        this.currentJump = currentMove;
        this.lastJump = null;
        this.tenthLast = null;
        this.prevDistance = currentJump.distanceSq(x, y, z);
        this.movedAwayCount = 0;
        this.sameCount = 0;
    }

    public void copyInto(MovementAlgorithmState state) {
        state.currentMove = currentMove;
        state.currentRender = currentRender;
        state.noMoveUpdates = noMoveUpdates;
        state.walkTicks = walkTicks;

        state.x = x;
        state.y = y;
        state.z = z;
        state.isGoal = isGoal;
        state.targetNodeIndex = targetNodeIndex;
        state.strafe = strafe;
        state.walkTicksLeft = walkTicksLeft;

        state.initialDelta = initialDelta;
        state.firstJump = firstJump;
        state.currentJump = currentJump;
        state.lastJump = lastJump;
        state.tenthLast = tenthLast;
        state.prevDistance = prevDistance;
        state.movedAwayCount = movedAwayCount;
        state.sameCount = sameCount;
    }

    public MovementAlgorithmState copyState() {
        return toBuilder().build();
    }

}
