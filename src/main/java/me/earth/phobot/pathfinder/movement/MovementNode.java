package me.earth.phobot.pathfinder.movement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import me.earth.phobot.movement.Movement;
import me.earth.phobot.pathfinder.algorithm.Abstract3dNode;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.player.MovementPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class MovementNode extends Abstract3dNode<MovementNode> {
    private final Movement.State state;
    private final boolean horizontalCollision;
    private final boolean verticalCollisionBelow;
    private final boolean verticalCollision;
    private final boolean onGround;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final @Nullable MovementNode goal;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private @Nullable MovementNode previous;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private @Nullable MovementNode next;

    private int targetNodeIndex;

    public MovementNode(MovementNode node, @Nullable MovementNode goal, int targetNodeIndex) {
        this(new Vec3(node.getX(), node.getY(), node.getZ()), node.state, goal, node.horizontalCollision, node.verticalCollisionBelow, node.verticalCollision, node.onGround, targetNodeIndex);
    }

    public MovementNode(MovementPlayer player, Movement.State state, @Nullable MovementNode goal, int targetNodeIndex) {
        this(player.position(), state, goal, player.horizontalCollision, player.verticalCollisionBelow, player.verticalCollision, player.onGround(), targetNodeIndex);
    }

    public MovementNode(Position pos, Movement.State state, @Nullable MovementNode goal,
                        boolean horizontalCollision, boolean verticalCollisionBelow, boolean verticalCollision, boolean onGround, int targetNodeIndex) {
        super(pos.x(), pos.y(), pos.z());
        this.state = state;
        this.horizontalCollision = horizontalCollision;
        this.verticalCollisionBelow = verticalCollisionBelow;
        this.verticalCollision = verticalCollision;
        this.onGround = onGround;
        this.goal = goal;
        this.targetNodeIndex = targetNodeIndex;
    }

    @Override
    @Deprecated
    public MovementNode[] getAdjacent() {
        return new MovementNode[0];
    }

    public void apply(Entity entity) {
        entity.setPos(getX(), getY(), getZ());
        entity.setDeltaMovement(state.getDelta());
        entity.setOnGround(onGround);
        entity.horizontalCollision = horizontalCollision;
        entity.verticalCollision = verticalCollision;
        entity.verticalCollisionBelow = verticalCollisionBelow;
    }

    public boolean isGoal() {
        return goal == null || super.equals(goal);
    }

    public boolean isStart() {
        return targetNodeIndex == 0;
    }

    public boolean positionEquals(@Nullable MovementNode other) {
        return super.equals(other);
    }

    public Set<BlockPos> getBlockedPositions(MovementPlayer player) {
        player.setPos(getX(), getY(), getZ());
        var result = new HashSet<BlockPos>();
        for (double y = this.getY(); y <= player.getBoundingBox().maxY; y++) {
            PositionUtil.getPositionsBlockedByEntityAtY(result, player, y);
        }

        return result;
    }

}
