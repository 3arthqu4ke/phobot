package me.earth.phobot.pathfinder.movement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.modules.movement.FastFall;
import me.earth.phobot.movement.Movement;
import me.earth.phobot.pathfinder.Path;
import me.earth.phobot.pathfinder.Pathfinder;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.mutables.MutPos;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.phobot.util.time.TimeUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// TODO: see MovementPathfinderTest.testFallDownNarrowHoleVeryCloseToEdge, we run over the hole then back into it!!!
// TODO: optimize, check horizontal collisions and try to optimize them away!
/**
 * We have a path of {@link MeshNode}s. Now we want to know where to actually move, so we need a path of {@link MovementNode}s.
 *
 * @see MovementPathfinder
 * @see Pathfinder
 */
@Slf4j
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MovementPathfindingAlgorithm implements Algorithm<MovementNode> {
    /**
     * @see MovementPathfindingAlgorithm#noMoveUpdates
     */
    public static final int NO_MOVE_THRESHOLD = 8;

    private final MutPos mutPos = new MutPos();
    private final Phobot phobot;
    private final ClientLevel level;
    private final Path<MeshNode> path;
    private final MovementPlayer player;
    private final FastFall fastFall;

    private Collection<MobEffectInstance> effects;
    private MovementNode start;
    private MovementNode goal;
    private MovementNode currentMove;
    private MovementNode currentRender;
    /**
     * It can happen that during an update we hit the next MeshNode without even having to produce a new MovementNode.
     * Especially at the start, where we want to get to MeshNode 0, this is a problem. This variable tracks the
     * number of such updates and then updates the targetNodeIndex accordingly, so we target further MeshNodes.
     */
    private int noMoveUpdates;
    /**
     * If > 0, we will walk for this amount of ticks and not bunnyhop.
     */
    private int walkTicks; // TODO: investigate, saw it walking for a large distance after rubber-banding a bit?!

    /**
     * Constructs a new MovementPathfindingAlgorithm.
     *
     * @param phobot the phobot instance of which {@link Movement} and {@link FastFall} will be used.
     * @param level the level to path find in.
     * @param path the path of {@link MeshNode}s to follow.
     * @param player optionally, a player to supply potion effects.
     * @param start optionally, a movement node to start from.
     * @param goal optionally, a goal node to finish on.
     */
    public MovementPathfindingAlgorithm(Phobot phobot, ClientLevel level, Path<MeshNode> path, @Nullable Player player, @Nullable MovementNode start, @Nullable MovementNode goal) {
        this(phobot, level, path, new MovementPlayer(level), phobot.getPingBypass().getModuleManager().getByClass(FastFall.class).orElseGet(() -> new FastFall(phobot, null)),
             player == null ? Collections.emptyList() : new ArrayList<>(player.getActiveEffects()), start, goal, null, null, 0, 0);
        init(player);
    }

    @Override
    public @Nullable Algorithm.Result<MovementNode> run(Cancellation cancellation) {
        if (start.isGoal()) {
            log.warn("MovementPathfinder ran for start " + start + " == " + goal);
            return null;
        }

        String message = "Cancelled: ";
        log.debug("Looking for path from " + start + " to " + goal);
        long time = TimeUtil.getMillis();
        while (!cancellation.isCancelled()) {
            if (isFinished()) {
                time = TimeUtil.getMillis() - time;
                log.debug("Found path from " + start + " to " + goal + ", took " + time + "ms.");
                return buildResult();
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

    /**
     * @return {@code true} if the {@link #currentMove} is the goal.
     */
    public boolean isFinished() {
        return currentMove.isGoal();
    }

    /**
     * Updates the effects the player is currently under. The pathfinder will update based on these.
     *
     * @param localPlayer the local player to get the effects from.
     */
    public void updatePlayer(Player localPlayer) {
        this.effects = localPlayer.getActiveEffects();
        player.setSpeed((float) localPlayer.getAttributeValue(Attributes.MOVEMENT_SPEED));
        player.getActiveEffectsMap().clear();
        effects.forEach(player::addEffect);
    }

    /**
     * Updates the pathfinder. Concretely this means that we take {@link #currentMove} and
     * look down the path of {@link MeshNode}s. We then target these MeshNodes and
     * attempt to bunny hop/strafe towards them. If successful {@link MovementNode#next()}
     * will yield the next MovementNodes for the {@link #currentMove}.
     *
     * @return {@code true} if we have successfully found new nodes.
     */
    public boolean update() {
        assert currentMove != null;
        currentMove.apply(player);
        log.info("Walkticks: " + walkTicks);
        // we prefer walking towards the goal if we are close to it
        boolean preferWalking = currentMove.distanceSq(goal) < 6.25/* 2.5 ^ 2 */;
        if (!preferWalking && bunnyHopTowardsTarget()) { // attempt to bunny hop towards the target
            return true;
        }

        log.warn("Strafing to goal instead?! " + walkTicks);
        // We did not manage to reach a new node by bunny hopping, now we use normal walking (strafing).
        // For strafing we only check the next MeshNode in the path, because it is closest and strafing is just a direct line.

        // When strafing we also check the MeshNode after the next one, in case it is diagonal to the current one.
        // In that case we can try to optimize and move diagonally to it.
        currentMove.apply(player); // unnecessary, no?
        int noMoveUpdateIndex = Math.min(noMoveUpdates, path.getPath().size() - 1);
        int targetNodeIndex = currentMove.isStart()
                ? noMoveUpdateIndex
                : currentMove.targetNodeIndex() + 1 < path.getPath().size()
                    ? Math.max(currentMove.targetNodeIndex() + 1, noMoveUpdateIndex)
                    : path.getPath().size() - 1;

        MeshNode meshNode = path.getPath().get(targetNodeIndex);
        if (targetNodeIndex + 1 < path.getPath().size()) {
            MeshNode next = path.getPath().get(targetNodeIndex + 1);
            MeshNode actualCurrentMeshNode = path.getPath().get(currentMove.targetNodeIndex());
            // check if the next node is diagonal to actualCurrent and try to move
            if (Math.abs(actualCurrentMeshNode.getX() - next.getX()) == 1
                    && Math.abs(actualCurrentMeshNode.getZ() - next.getZ()) == 1 && strafeTowards(next, targetNodeIndex + 1)) {
                return true;
            }
        }

        currentMove.apply(player);
        if (!strafeTowards(meshNode, targetNodeIndex)) {
            if (targetNodeIndex == 0 && path.getPath().size() > 1 && strafeTowards(path.getPath().get(1), 1)) {
                return true;
            }

            if (preferWalking /*&& walkOnly == null*/) { // we have not yet tried to bunny hop towards the target, make last effort
                currentMove.apply(player);
                if (bunnyHopTowardsTarget()) {
                    return true;
                }
            }

            log.error("Failed to move towards next " + meshNode);
            return false;
        }

        return true;
    }

    private boolean bunnyHopTowardsTarget() {
        log.debug("Current state: " + currentMove.state());
        // we go through all MeshNodes and check if we can reach them using a bunny hop jump.
        //                                                                    v start node could be before the start mesh node
        for (int i = path.getPath().size() - 1; i > (currentMove.isStart() ? -1 : Math.max(0, currentMove.targetNodeIndex())); i--) {
            MeshNode meshNode = path.getPath().get(i);
            boolean isGoal = i == path.getPath().size() - 1;
            double x = isGoal ? goal.getX() : meshNode.getX() + 0.5;
            double y = isGoal ? goal.getY() : PositionUtil.getMaxYAtPosition(mutPos.set(meshNode.getX(), meshNode.getY() - 1, meshNode.getZ()), level);
            double z = isGoal ? goal.getZ() : meshNode.getZ() + 0.5;
            // TODO: here we need at least a distance threshold, because right now this might find a straight path to any meshnode, even ones that are 100 blocks away
            //  which is actually not that bad of a thing? But will cost lots of computational power
            // TODO: optimize, do not bunnyhop for long falls?
            if (currentMove.isStart() || i > currentMove.targetNodeIndex()) {
                // we can reach this MeshNode from our current position, attempt to move towards it
                if (moveTowards(x, y, z, isGoal, i, false)) {
                    log.debug("Successfully bunny hopped towards " + meshNode);
                    return true;
                } else {
                    currentMove.apply(player);
                }
            }
        }

        return false;
    }

    private boolean strafeTowards(MeshNode meshNode, int targetNodeIndex) {
        boolean isGoal = targetNodeIndex == path.getPath().size() - 1;
        return moveTowards(
                isGoal ? goal.getX() : meshNode.getX() + 0.5,
                isGoal ? goal.getY() : PositionUtil.getMaxYAtPosition(mutPos.set(meshNode.getX(), meshNode.getY() - 1, meshNode.getZ()), level),
                isGoal ? goal.getZ() : meshNode.getZ() + 0.5,
                isGoal, targetNodeIndex, true);
    }

    private boolean moveTowards(double x, double y, double z, boolean isGoal, int targetNodeIndex, boolean strafeIn) {
        int walkTicksLeft = walkTicks;
        // The horizontal direction to move towards the target x and z from our current position.
        Vec3 initialDelta = new Vec3(x - currentMove.getX(), 0.0, z - currentMove.getZ()).normalize();

        MovementNode firstJump = null;
        MovementNode currentJump = currentMove;
        MovementNode lastJump = null;
        MovementNode tenthLast = null;

        double prevDistance = currentJump.distanceSq(x, y, z);
        int movedAwayCount = 0;
        int sameCount = 0;
        while (currentJump != null) {
            boolean strafe = strafeIn || walkTicksLeft > 0;

            // We have not moved, might have gotten stuck somewhere
            if (lastJump != null && currentJump.positionEquals(lastJump)) {
                log.debug("Position equals! " + initialDelta + ", horizontal collision: " + player.horizontalCollision);
                sameCount++;
                // TODO: check if this makes sense?
                if (sameCount > 1) { // allow one tick of position being the same, this could be because we are waiting for collision etc.?
                    log.debug("SameCount: " + x + ", " + y + ", " + z + " from " + currentJump + " the current " + currentJump + " is the same as " + lastJump);
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
                // We have reached the current MeshNode
                if (Math.abs(currentJump.getX() - x) <= 0.5 && Math.abs(currentJump.getZ() - z) <= 0.5 && Math.abs(currentJump.getY() - y) <= 0.5) {
                    if (currentJump.isStart() && targetNodeIndex == 0) {
                        // TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        // TODO: does this make sense? Why would we do this?
                        //  especially with the new noMoveUpdates????
                        return false;
                    }

                    log.debug("Reached node " + targetNodeIndex + " at " + x + ", " + y + ", " + z);
                    break;
                } else {
                    // Check if we have reached another MeshNode instead
                    boolean found = false;
                    for (int i = targetNodeIndex + 1; i < Math.min(path.getPath().size() - 1, targetNodeIndex + 4); i++) {
                        MeshNode otherMeshNode = path.getPath().get(i);
                        double otherX = otherMeshNode.getX() + 0.5;
                        double otherY = PositionUtil.getMaxYAtPosition(mutPos.set(otherMeshNode.getX(), otherMeshNode.getY() - 1, otherMeshNode.getZ()), level);
                        double otherZ = otherMeshNode.getZ() + 0.5;
                        if (Math.abs(currentJump.getX() - otherX) <= 0.5 && Math.abs(currentJump.getZ() - otherZ) <= 0.5 && Math.abs(currentJump.getY() - otherY) <= 0.5 && i > currentMove.targetNodeIndex()) {
                            log.debug("Failed to reach " + targetNodeIndex + " but reached " + i + " instead!");
                            currentJump.targetNodeIndex(i);
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        break;
                    }
                }
            }

            // If we have moved away from our target node too many times it means we passed it and are running off in the wrong direction
            double distance = currentJump.distanceSq(x, y, z);
            if (distance > prevDistance) {
                movedAwayCount++;
                if (movedAwayCount > 10) {
                    log.debug("Moved away from " + x + ", " + y + ", " + z + " ten times!");
                    currentJump = null;
                    break;
                }
            } else {
                movedAwayCount = 0;
            }

            prevDistance = distance;
            int ith = 10;
            tenthLast = tenthLast == null ? getIthLastNodeExceptCurrentMove(currentJump, ith) : tenthLast.next();
            // If we have not moved away more than 1 block from the position we were in 10 movements ago, we are stuck
            if (tenthLast != null && tenthLast.distanceSq(currentJump) <= 1.0) {
                log.debug("Got stuck on " + currentJump);
                currentJump = null;
                break;
            }

            // Vector we need to move towards the targeted MeshNode
            Vec3 delta = new Vec3(x - currentJump.getX(), 0.0, z - currentJump.getZ());
            log.debug("Delta: " + delta);
            boolean normalize = true;
            boolean strafeFall = false;
            // We can move towards the target in just one tick
            if (delta.lengthSqr() <= Mth.square(phobot.getMovementService().getMovement().getSpeed(player))) {
                if (isGoal || strafe && !currentJump.onGround() && Math.abs(currentJump.getY() - y) > 1.0) {// about to hit goal, or falling down
                    normalize = false; // just center us when falling down
                } else if (strafe && currentJump.onGround() && currentJump.getY() > y) {// avoids getting stuck on cc spawn
                    strafeFall = true; // TODO: kinda forgot what this was for... I assume is has something to do with falling down to the side of the spawn?
                }
            }

            delta = normalize ? (strafe && !strafeFall ? delta.normalize() : initialDelta) : delta;
            log.debug("Delta after transform: " + delta + ", " + isGoal + ", distance to goal: " + currentJump.distance(goal) + ", " + currentJump.getY() + " vs " + goal.getY());
            lastJump = currentJump;
            currentJump = move(currentJump, targetNodeIndex, new Vec3(delta.x, player.getDeltaMovement().y, delta.z), strafe, normalize);
            if (currentJump != lastJump) {
                walkTicksLeft = Math.max(0, walkTicksLeft - 1);
            }

            if (firstJump == null) {
                firstJump = currentJump;
            }
        }

        if (currentJump != null) {
            if (currentJump == currentMove) {
                noMoveUpdates++;
                if (noMoveUpdates > NO_MOVE_THRESHOLD) {
                    log.error("9 updates without moving!!!" + currentMove);
                    return false;
                }
            } else {
                noMoveUpdates = 0;
            }

            currentMove.next(firstJump);
            currentMove = currentJump;
            walkTicks = walkTicksLeft;
            log.debug("Success! " + currentJump.targetNodeIndex() + " reached: " + currentJump);
            currentRender = currentMove;
            return true;
        }

        log.debug("Current Jump == null");
        return false;
    }

    /**
     * Moves the {@link #player} one tick into the given direction.
     *
     * @param node the node we were starting on, which also supplies the previous {@link Movement.State} to use for our speed hacks.
     * @param targetNodeIndex the index of the {@link MeshNode} we plan to move towards with this move.
     * @param directionAndYMovement the direction we are moving in, the y component is the players Y-Delta.
     * @param strafe if we should strafe instead of bunny hop.
     * @param normalized when not normalized it means that we are doing one last move to hit the goal, in that case we always strafe.
     * @return a new MovementNode that represents the next position the player will be on, or {@code null} if failed.
     */
    private @Nullable MovementNode move(MovementNode node, int targetNodeIndex, Vec3 directionAndYMovement, boolean strafe, boolean normalized) {
        Movement.State[] stateRef = new Movement.State[1];
        Vec3[] deltaDuringMoveEventRef = new Vec3[1];
        Vec3[] deltaReturnedForMoveEventRef = new Vec3[1];
        double xBefore = player.getX();
        double zBefore = player.getZ();
        // The move callback essentially simulates a MoveEvent. We modify the delta vector the same way Movement modules would do during a MoveEvent
        player.setMoveCallback(delta -> {
            Vec3 dir = new Vec3(directionAndYMovement.x, player.getDeltaMovement().y, directionAndYMovement.z);
            Movement.State state;
            boolean hasStrafed = false;
            boolean hasBunnyHopped = false;
            if (phobot.getMovementService().getMovement().shouldNotUseMovementHacks(player)) {
                state = new Movement.State(0.0, 0.0, delta, 0, false, true);
            } else if (!normalized) {
                state = new Movement.State(phobot.getMovementService().getMovement().getSpeed(player), 0.0, dir, 0, false, false);
            } else if (strafe) {
                hasStrafed = true;
                state = phobot.getMovementService().getMovement().strafe(player, level, node.state(), dir);
            } else {
                hasBunnyHopped = true;
                state = phobot.getMovementService().getMovement().move(player, level, node.state(), dir);
            }

            // TODO: !!! y-Movement in our Movement system is the root of all evil!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            stateRef[0] = state;
            Vec3 moveEventDelta = state.getDelta();
            if (state.isReset()) {
                moveEventDelta = delta;
            } else if (hasBunnyHopped) {
                player.setDeltaMovement(delta.x, state.getDelta().y, delta.z);
            } else if (hasStrafed) {
                player.setDeltaMovement(state.getDelta().x, state.getDelta().y, state.getDelta().z);
            }

            if (!hasBunnyHopped && fastFall.isEnabled() && fastFall.canFastFall(player, state.getDelta(), level, false)) {
                moveEventDelta = fastFall.getFastFallVec(moveEventDelta);
            }

            // copy because there are clients that make Vec3 mutable :(
            deltaDuringMoveEventRef[0] = new Vec3(player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z);
            deltaReturnedForMoveEventRef[0] = new Vec3(moveEventDelta.x, moveEventDelta.y, moveEventDelta.z);
            return moveEventDelta;
        });

        /*
        Allows you to visualize what is happening

        try {
            Thread.sleep(50L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        */

        // TODO: THIS IS NOT PERFECT YET!
        Vec3 xxVec = new Vec3(directionAndYMovement.x, 0.0, directionAndYMovement.z).normalize();
        player.xxa = (float) xxVec.x;
        player.yya = 0.0f;
        player.zza = (float) xxVec.z;
        player.aiTravel();
        Movement.State state = stateRef[0];
        Vec3 deltaDuringMoveEvent = deltaDuringMoveEventRef[0];
        Vec3 deltaReturnedForMoveEvent = deltaReturnedForMoveEventRef[0];
        if (state == null || deltaDuringMoveEvent == null || deltaReturnedForMoveEvent == null) {
            log.error("Something went wrong while moving from " + node + " in direction " + directionAndYMovement);
            return null;
        }

        state.setDistance(Math.sqrt((player.getX() - xBefore) * (player.getX() - xBefore) + (player.getZ() - zBefore) * (player.getZ() - zBefore)));
        MovementNode movementNode = new MovementNode(player, state, goal, targetNodeIndex, deltaDuringMoveEvent, deltaReturnedForMoveEvent);
        movementNode.previous(node);
        if (node != currentMove) { // the new nodes we compute will only be connected to the actualCurrent path after we have landed successfully
            node.next(movementNode);
        }

        currentRender = movementNode;
        return movementNode;
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

    private void init(@Nullable Player initialPlayer) {
        player.setMovement(phobot.getMovementService().getMovement());
        if (initialPlayer != null) {
            updatePlayer(initialPlayer);
        }

        if (goal == null) {
            Vec3 gravity = new Vec3(0.0, phobot.getMovementService().getMovement().getDeltaYOnGround(), 0.0);
            goal = MovementNode.createGoalNode(path, gravity);
        } else {
            goal = new MovementNode(goal, goal, path.getPath().size() - 1);
        }

        if (start == null) {
            player.setPos(path.getExactStart());
            Movement.State state = new Movement.State();
            if (initialPlayer != null) {
                EntityMovementSnapshot snapshot = new EntityMovementSnapshot(initialPlayer);
                snapshot.apply(player);
                state.setDelta(initialPlayer.getDeltaMovement());
            } else {
                player.verticalCollision = true;
                player.verticalCollisionBelow = true;
                player.horizontalCollision = false;
                player.setSpeed((float) player.getAttributeValue(Attributes.MOVEMENT_SPEED));
                player.setOnGround(true);
                Vec3 gravity = new Vec3(0.0, phobot.getMovementService().getMovement().getDeltaYOnGround(), 0.0);
                state.setDelta(gravity);
                player.setDeltaMovement(gravity);
            }

            start = new MovementNode(player, state, goal, 0);
        } else { // start could have been used in a previous pathing process, copy and set new goal and targetNodeIndex
            start = new MovementNode(start, goal, 0);
            start.apply(player);
        }

        currentMove = start;
        currentRender = start;
    }

    private @Nullable Algorithm.Result<MovementNode> buildResult() {
        List<MovementNode> path = new ArrayList<>();
        MovementNode node = start;
        while (node != null) {
            path.add(node);
            node = node.next();
        }

        if (!path.get(path.size() - 1).isGoal()) {
            log.error("Last entry in path was " + path.get(path.size() - 1) + " but not the goal " + goal + "!");
            return null;
        }

        return new Algorithm.Result<>(path, Algorithm.Result.Order.START_TO_GOAL);
    }

}
