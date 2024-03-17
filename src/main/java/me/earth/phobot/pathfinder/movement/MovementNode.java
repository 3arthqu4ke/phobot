package me.earth.phobot.pathfinder.movement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import me.earth.phobot.event.MoveEvent;
import me.earth.phobot.movement.Movement;
import me.earth.phobot.pathfinder.Path;
import me.earth.phobot.pathfinder.algorithm.Abstract3dNode;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.util.player.MovementPlayer;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import static me.earth.phobot.pathfinder.movement.EntityMovementSnapshot.dummy;

/**
 * Essentially a snapshots of an {@link Player}s position and movement related states after a call to {@link Player#travel(Vec3)}.
 */
@Setter
@Getter
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class MovementNode extends Abstract3dNode<MovementNode> {
    private final Movement.State state;
    private final EntityMovementSnapshot snapshot;
    /**
     * The {@link Player#getDeltaMovement()} the player has during the {@link MoveEvent}, after movement hack modules have been applied.
     * @see #deltaReturnedForMoveEvent
     * @see MovementPlayer#setMoveCallback(Function)
     */
    private final Vec3 deltaDuringMoveEvent;
    /**
     * The {@link Vec3} that was returned via {@link MoveEvent#setVec(Vec3)}.
     * @see #deltaDuringMoveEvent
     * @see MovementPlayer#setMoveCallback(Function)
     */
    private final Vec3 deltaReturnedForMoveEvent;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private @Nullable MovementNode goal;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private @Nullable MovementNode previous;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private @Nullable MovementNode next;

    private int targetNodeIndex;

    public MovementNode(Player player, Movement.State state, @Nullable MovementNode goal, int targetNodeIndex) {
        this(player, state, goal, targetNodeIndex, player.getDeltaMovement(), player.getDeltaMovement());
    }

    public MovementNode(Player player, Movement.State state, @Nullable MovementNode goal, int targetNodeIndex, Vec3 deltaDuringMoveEvent, Vec3 deltaReturnedForMoveEvent) {
        this(player.position(), state, goal, new EntityMovementSnapshot(player), targetNodeIndex, deltaDuringMoveEvent, deltaReturnedForMoveEvent);
    }

    public MovementNode(MovementNode node, @Nullable MovementNode goal, int targetNodeIndex) {
        this(node.snapshot.getPosition(), node.state, goal, node.snapshot, targetNodeIndex, node.deltaDuringMoveEvent, node.deltaReturnedForMoveEvent);
    }

    public MovementNode(Position pos, Movement.State state, @Nullable MovementNode goal, EntityMovementSnapshot snapshot, int targetNodeIndex,
                        Vec3 deltaDuringMoveEvent, Vec3 deltaReturnedForMoveEvent) {
        super(pos.x(), pos.y(), pos.z());
        this.state = state;
        this.goal = goal;
        this.snapshot = snapshot;
        this.targetNodeIndex = targetNodeIndex;
        this.deltaDuringMoveEvent = deltaDuringMoveEvent;
        this.deltaReturnedForMoveEvent = deltaReturnedForMoveEvent;
    }

    @Override
    @Deprecated
    public MovementNode[] getAdjacent() {
        return new MovementNode[0];
    }

    public void apply(Entity entity) {
        snapshot.apply(entity);
    }

    public boolean isGoal() {
        return goal == null || super.equals(goal);
    }

    public boolean isStart() {
        return targetNodeIndex == 0;
    }

    public boolean onGround() {
        return snapshot.isOnGround();
    }

    public UnaryOperator<Vec3> getMoveEventDeltaFunction(Player player) {
        return delta -> {
            player.setDeltaMovement(deltaDuringMoveEvent);
            return deltaReturnedForMoveEvent;
        };
    }

    public static MovementNode createGoalNode(Path<MeshNode> path, Vec3 gravity) {
        return new MovementNode(path.getExactGoal(), new Movement.State(), null, dummy(path.getExactGoal(), gravity), path.getPath().size() - 1, gravity, gravity);
    }

}
