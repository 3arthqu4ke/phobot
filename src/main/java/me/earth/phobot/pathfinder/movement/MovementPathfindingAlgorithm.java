package me.earth.phobot.pathfinder.movement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.modules.movement.FastFall;
import me.earth.phobot.movement.Movement;
import me.earth.phobot.movement.MovementParable;
import me.earth.phobot.pathfinder.Path;
import me.earth.phobot.pathfinder.algorithm.RenderableAlgorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.util.CancellableTask;
import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.mutables.MutPos;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.phobot.util.time.TimeUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor_={@Deprecated})
public class MovementPathfindingAlgorithm implements RenderableAlgorithm<MovementNode>, CancellableTask<@Nullable List<MovementNode>> {
    private final MutPos mutPos = new MutPos();
    private final Phobot phobot;
    private final ClientLevel level;
    private final Path<MeshNode> path;
    private final MovementPlayer player;
    private final FastFall fastFall;

    private Collection<MobEffectInstance> effects;
    private MovementParable parable; // TODO: update parable with Movement.State, we can jump further if we have bunnyhopped already!
    private MovementNode start;
    private MovementNode goal;
    private MovementNode currentMove;
    private MovementNode currentRender;

    @SuppressWarnings({"deprecation", "RedundantSuppression"}) // this calls our private constructor that should not be called by anyone else
    public MovementPathfindingAlgorithm(Phobot phobot, ClientLevel level, Path<MeshNode> path, @Nullable Player player, @Nullable MovementNode start, @Nullable MovementNode goal) {
        this(phobot, level, path, new MovementPlayer(level), phobot.getPingBypass().getModuleManager().getByClass(FastFall.class).orElseGet(() -> new FastFall(phobot, null)),
             player == null ? Collections.emptyList() : new ArrayList<>(player.getActiveEffects()), null, start, goal, null, null);
        init();
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"}) // this calls our private constructor that should not be called by anyone else
    public MovementPathfindingAlgorithm(MovementPathfindingAlgorithm copy) {
        this(copy.phobot, copy.level, copy.path, new MovementPlayer(copy.level), copy.fastFall, copy.effects, copy.parable, copy.start, copy.goal, copy.currentMove, copy.currentMove);
        init();
    }

    // TODO: if close to goal, prefer walking
    public boolean update() {
        assert currentMove != null;
        currentMove.apply(player);
        for (int i = path.getPath().size() - 1; i > (currentMove.isStart() ? -1/* start node could be before the start mesh node*/ : Math.max(0, currentMove.targetNodeIndex())); i--) {
            MeshNode meshNode = path.getPath().get(i);
            boolean isGoal = i == path.getPath().size() - 1;
            double x = isGoal ? goal.getX() : meshNode.getX() + 0.5;
            double y = isGoal ? goal.getY() : PositionUtil.getMaxYAtPosition(mutPos.set(meshNode.getX(), meshNode.getY() - 1, meshNode.getZ()), level);
            double z = isGoal ? goal.getZ() : meshNode.getZ() + 0.5;
            // TODO: optimize, do not bunnyhop for long falls?
            // TODO: take into account that we move on ground for one tick before jumping because we start with stage 0? Or start with stage 2?
            // TODO: make this better? Instead of canReach check if we are on the parable?
            if (parable.isOnParable(player.getX(), player.getY(), player.getZ(), x, y, z, 0.5, 0.125) && (currentMove.isStart() || i > currentMove.targetNodeIndex())) {
                log.info("Can reach " + i + " at " + (meshNode.getX() + 0.5) + " " + (meshNode.getY() + " " +  (meshNode.getZ() + 0.5) + " from " + player.position()));
                if (moveTowards(x, y, z, isGoal, i, false)) {
                    log.info("Successfully bunnyhopped towards " + meshNode);
                    return true;
                } else {
                    currentMove.apply(player);
                }
            }
        }

        currentMove.apply(player); // unnecessary, no?
        int targetNodeIndex = currentMove.isStart() ? 0 : currentMove.targetNodeIndex() + 1 < path.getPath().size() ? currentMove.targetNodeIndex() + 1 : path.getPath().size() - 1;
        MeshNode meshNode = path.getPath().get(targetNodeIndex);
        if (targetNodeIndex + 1 < path.getPath().size()) {
            MeshNode next = path.getPath().get(targetNodeIndex + 1);
            MeshNode actualCurrentMeshNode = path.getPath().get(currentMove.targetNodeIndex());          // next is diagonal to actualCurrent
            if (Math.abs(actualCurrentMeshNode.getX() - next.getX()) == 1 && Math.abs(actualCurrentMeshNode.getZ() - next.getZ()) == 1 && strafeTowards(next, targetNodeIndex + 1)) {
                return true;
            }
        }

        currentMove.apply(player);
        log.info("Strafing towards next: " + (targetNodeIndex) + " at " + meshNode);
        if (!strafeTowards(meshNode, targetNodeIndex)) {
            if (targetNodeIndex == 0 && path.getPath().size() > 1 && strafeTowards(path.getPath().get(1), 1)) {
                return true;
            }

            log.error("Failed to move towards next " + meshNode);
            return false;
        }

        return true;
    }

    private boolean strafeTowards(MeshNode meshNode, int targetNodeIndex) {
        boolean isGoal = targetNodeIndex == path.getPath().size() - 1;
        return moveTowards(
                isGoal ? goal.getX() : meshNode.getX() + 0.5,
                isGoal ? goal.getY() : PositionUtil.getMaxYAtPosition(mutPos.set(meshNode.getX(), meshNode.getY() - 1, meshNode.getZ()), level),
                isGoal ? goal.getZ() : meshNode.getZ() + 0.5,
                isGoal, targetNodeIndex, true);
    }

    private boolean moveTowards(double x, double y, double z, boolean isGoal, int targetNodeIndex, boolean strafe) {
        Vec3 initialDelta = new Vec3(x - currentMove.getX(), 0.0, z - currentMove.getZ()).normalize();
        MovementNode firstJump = null;
        MovementNode currentJump = currentMove;
        MovementNode lastJump = null;
        MovementNode tenthLast = null;
        double prevDistance = currentJump.distanceSq(x, y, z);
        int movedAwayCount = 0;
        int sameCount = 0;
        while (currentJump != null) {
            if (currentJump.positionEquals(lastJump)) {
                log.info("Position equals!");
                sameCount++;
                // TODO: check if this makes sense?
                if (sameCount > 1) { // allow one tick of position being the same, this could be because we are waiting for collision etc.?
                    log.info(x + ", " + y + ", " + z + " from " + currentJump + " the current " + currentJump + " is the same as " + lastJump);
                    currentJump = null;
                    break;
                }
            } else {
                sameCount = 0;
            }

            if (currentJump.isGoal()) {
                break;
            }

            if (!isGoal && currentJump.onGround()) {
                if (Math.abs(currentJump.getX() - x) <= 0.5 && Math.abs(currentJump.getZ() - z) <= 0.5 && Math.abs(currentJump.getY() - y) <= 0.5) {
                    if (currentJump.isStart() && targetNodeIndex == 0) {
                        return false;
                    }

                    log.info("Reached node " + targetNodeIndex + " at " + x + ", " + y + ", " + z);
                    break;
                } else {
                    boolean found = false;
                    for (int i = targetNodeIndex + 1; i < Math.min(path.getPath().size() - 1, targetNodeIndex + 4); i++) {
                        MeshNode otherMeshNode = path.getPath().get(i);
                        double otherX = otherMeshNode.getX() + 0.5;
                        double otherY = PositionUtil.getMaxYAtPosition(mutPos.set(otherMeshNode.getX(), otherMeshNode.getY() - 1, otherMeshNode.getZ()), level);
                        double otherZ = otherMeshNode.getZ() + 0.5;
                        if (Math.abs(currentJump.getX() - otherX) <= 0.5 && Math.abs(currentJump.getZ() - otherZ) <= 0.5 && Math.abs(currentJump.getY() - otherY) <= 0.5 && i > currentMove.targetNodeIndex()) {
                            log.info("Failed to reach " + targetNodeIndex + " but reached " + i + " instead!");
                            currentJump.targetNodeIndex(i);
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        break;
                    }
                }

                // TODO: this does not prevent trying to break through ceiling to reach node underneath!
                // TODO: optimize, check horizontalcollisions and try to optimize them away!
            }

            double distance = currentJump.distanceSq(x, y, z);
            if (distance > prevDistance) {
                movedAwayCount++;
                if (movedAwayCount > 10) {
                    log.info("Moved away from " + x + ", " + y + ", " + z + " ten times!");
                    currentJump = null;
                    break;
                }
            } else {
                movedAwayCount = 0;
            }

            prevDistance = distance;
            tenthLast = tenthLast == null ? getIthLastNodeExceptCurrentMove(currentJump, 10) : tenthLast.next();
            if (tenthLast != null && tenthLast.distanceSq(currentJump) <= 1.0) { // we have not moved away properly from last node
                log.info("Got stuck on " + currentJump);
                currentJump = null;
                break;
            }

            // Vector we need to move towards the targeted MeshNode
            Vec3 delta = new Vec3(x - currentJump.getX(), 0.0, z - currentJump.getZ());
            boolean normalize = true;
            boolean strafeFall = false;
            if (delta.lengthSqr() <= Mth.square(phobot.getMovementService().getMovement().getSpeed(player))) {
                if (isGoal || strafe && !currentJump.onGround() && Math.abs(currentJump.getY() - y) > 1.0) { // about to hit goal, or falling down
                    normalize = false;
                } else if (strafe && currentJump.onGround() && currentJump.getY() > y) { // avoids getting stuck on cc spawn
                    strafeFall = true;
                }
            }

            delta = normalize ? (strafe && !strafeFall ? delta.normalize() : initialDelta) : delta;
            lastJump = currentJump;
            currentJump = move(currentJump, targetNodeIndex, new Vec3(delta.x, player.getDeltaMovement().y, delta.z), strafe, normalize);
            if (firstJump == null) {
                firstJump = currentJump;
            }
        }

        if (currentJump != null) {
            currentMove.next(firstJump);
            currentMove = currentJump;
            log.info("Success! " + currentJump.targetNodeIndex() + " reached: " + currentJump);
            currentRender = currentMove;
            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<MovementNode> run(Cancellation cancellation) {
        if (start.isGoal()) {
            log.warn("MovementPathfinder ran for start " + start + " == " + goal);
            return null;
        }

        String message = "Cancelled: ";
        log.info("Looking for path from " + start + " to " + goal);
        long time = TimeUtil.getMillis();
        while (!cancellation.isCancelled()) {
            if (isFinished()) {
                time = TimeUtil.getMillis() - time;
                log.info("Found path from " + start + " to " + goal + ", took " + time + "ms.");
                return buildPath();
            }

            if (!update()) {
                message = "Failed Update: ";
                break;
            }
        }

        time = TimeUtil.getMillis() - time;
        log.warn(message + "Failed to find path from " + start + " to " + goal + ", took " + time + "ms.");
        return null;
    }

    @Override
    public @Nullable MovementNode getCurrent() { // for rendering purposes
        return currentRender;
    }

    @Override
    public @Nullable MovementNode getCameFrom(MovementNode node) {
        return node.previous();
    }

    public boolean isFinished() {
        return currentMove.isGoal();
    }

    public void updatePotionEffects(Collection<MobEffectInstance> effects) {
        this.effects = effects;
        player.getActiveEffectsMap().clear();
        effects.forEach(player::addEffect);
        this.parable = MovementParable.calculate(player, level);
    }

    private @Nullable MovementNode move(MovementNode node, int targetNodeIndex, Vec3 directionAndYMovement, boolean strafe, boolean normalized) {
        Movement.State[] stateRef = new Movement.State[1];
        double xBefore = player.getX();
        double zBefore = player.getZ();
        player.setMoveCallback(delta -> {
            Movement.State state = !normalized
                ? new Movement.State(phobot.getMovementService().getMovement().getSpeed(player), 0.0, directionAndYMovement, 0, false, false)
                : strafe
                    ? phobot.getMovementService().getMovement().strafe(player, level, node.state(), directionAndYMovement)
                    : phobot.getMovementService().getMovement().move(player, level, node.state(), directionAndYMovement);

            stateRef[0] = state;
            if (state.isReset()) {
                state.setDelta(new Vec3(delta.x, delta.y, delta.z));
            } else {
                player.setDeltaMovement(delta.x, state.getDelta().y, delta.z);
            }

            if (strafe && fastFall.canFastFall(player, state.getDelta(), level, false)) {
                state.setDelta(fastFall.getFastFallVec(state.getDelta()));
            }

            return state.getDelta();
        });

        try {
            Thread.sleep(50L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        player.travel();
        Movement.State state = stateRef[0];
        if (state == null) {
            log.error("Something went wrong while moving from " + node + " in direction " + directionAndYMovement);
            return null;
        }

        log.info("Moved towards " + targetNodeIndex + " bunnyHop: " + !strafe + " " + state.getStage());
        state.setDistance(Math.sqrt((player.getX() - xBefore) * (player.getX() - xBefore) + (player.getZ() - zBefore) * (player.getZ() - zBefore)));
        state.setDelta(new Vec3(state.getDelta().x, player.getDeltaMovement().y, state.getDelta().z));
        MovementNode movementNode = new MovementNode(player, state, goal, targetNodeIndex);
        movementNode.previous(node);
        if (node != currentMove) { // the new nodes we compute will only be connected to the actualCurrent path after we have landed successfully
            node.next(movementNode);
        }

        currentRender = movementNode;
        return movementNode;
    }

    private @Nullable List<MovementNode> buildPath() {
        List<MovementNode> result = new ArrayList<>();
        MovementNode node = start;
        while (node != null) {
            result.add(node);
            node = node.next();
        }

        if (!result.get(result.size() - 1).isGoal()) {
            log.error("Last entry in path was " + result.get(result.size() - 1) + " but not the goal " + goal + "!");
            return null;
        }

        return result;
    }

    private @Nullable MovementNode getIthLastNodeExceptCurrentMove(@Nullable MovementNode node, int ith) {
        for (int i = 0; i < ith; i++) {
            if (node == null || node == currentMove) {
                return null;
            }

            node = node.previous();
        }

        return node;
    }

    private void init() {
        player.setMovement(phobot.getMovementService().getMovement());
        updatePotionEffects(effects);

        if (goal == null) {
            goal = new MovementNode(path.getExactGoal(), new Movement.State(), null, false, false, false, false, path.getPath().size() - 1);
        } else {
            goal = new MovementNode(goal, goal, path.getPath().size() - 1);
        }

        if (start == null) {
            player.setPos(path.getExactStart());
            player.verticalCollision = true;
            player.verticalCollisionBelow = true;
            player.horizontalCollision = false; // TODO: ?
            player.setOnGround(true);
            Movement.State state = new Movement.State();
            state.setDelta(new Vec3(0.0, -phobot.getMovementService().getMovement().getDeltaYOnGround(), 0.0));
            start = new MovementNode(player, state, goal, 0);
        } else { // start could have been used in a previous pathing process, copy and set new goal and targetNodeIndex
            start = new MovementNode(start, goal, 0);
        }

        currentMove = start;
        currentRender = start;
    }

}
