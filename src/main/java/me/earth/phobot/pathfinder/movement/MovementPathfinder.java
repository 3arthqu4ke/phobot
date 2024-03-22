package me.earth.phobot.pathfinder.movement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.event.*;
import me.earth.phobot.movement.BunnyHop;
import me.earth.phobot.movement.Movement;
import me.earth.phobot.pathfinder.Path;
import me.earth.phobot.pathfinder.algorithm.AStar;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManager;
import me.earth.phobot.pathfinder.render.AlgorithmRenderer;
import me.earth.phobot.pathfinder.render.PathRenderer;
import me.earth.phobot.pathfinder.util.CancellableFuture;
import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.util.NullabilityUtil;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static net.minecraft.ChatFormatting.RED;

/**
 * Pathfinding in Phobot happens in two phases.
 * First a raw path of block positions, ({@link MeshNode}s) is found, using an {@link Algorithm}, such as {@link AStar} on the mesh created by the {@link NavigationMeshManager}.
 * Then we can use a {@link Movement}, such as {@link BunnyHop} to get the exact moves for the player, using a {@link MovementPathfindingAlgorithm}.
 * This class does that and immediately makes the player follow the path of {@link MovementNode}s that gets created.
 */
@Slf4j
public class MovementPathfinder extends SubscriberImpl {
    private final PathRenderer pathRenderer = new PathRenderer();
    private final StopWatch.ForSingleThread timer = new StopWatch.ForSingleThread();
    private final PingBypass pingBypass;
    protected State state;

    public MovementPathfinder(PingBypass pingBypass) {
        this(pingBypass, true, true);
    }

    public MovementPathfinder(PingBypass pingBypass, boolean lag, boolean render) {
        this.pingBypass = pingBypass;
        listen(new SafeListener<PathfinderUpdateEvent>(pingBypass.getMinecraft()) {
            @Override
            public void onEvent(PathfinderUpdateEvent event, LocalPlayer localPlayer, ClientLevel clientLevel, MultiPlayerGameMode multiPlayerGameMode) {
                State current = state;
                if (current == null) {
                    return;
                }

                Player player = getPlayer(localPlayer);
                if (player == null) {
                    reset(Result.GET_PLAYER_FAILURE);
                    return;
                }
                // check if player has reached the MovementNode we set during the MoveEvent
                handlePlayerDidNotReachPosition(current, player);
                if (current.currentNode.isGoal()) { // done!
                    player.setDeltaMovement(new Vec3(0.0, player.getDeltaMovement().y, 0.0));
                    reset(Result.FINISHED);
                    return;
                }

                if (current.laggedThisTick) {
                    current.resetToPlayer(player);
                } else {
                    applyMovementNode(player, current.currentNode);
                    timer.reset();
                }
            }
        });

        listen(new SafeListener<MoveEvent>(pingBypass.getMinecraft(), Integer.MIN_VALUE + 1000) { // this should happen late
            @Override
            public void onEvent(MoveEvent event, LocalPlayer localPlayer, ClientLevel level, MultiPlayerGameMode gameMode) {
                State current = state;
                if (current == null) {
                    return;
                }

                Player player = getPlayer(localPlayer);
                if (player == null) {
                    reset(Result.GET_PLAYER_FAILURE);
                    return;
                }

                if (current.currentNode.isGoal()) {
                    player.setDeltaMovement(new Vec3(0.0, player.getDeltaMovement().y, 0.0));
                    reset(Result.FINISHED);
                    return;
                }

                if (current.cancellation.isCancelled()) {
                    reset(Result.CANCELLED);
                    return;
                }

                if (!current.getTimeSinceLastLagBack().passed(getLagTime())) {
                    current.laggedThisTick = true;
                    return;
                }

                current.algorithm.updatePlayer(player);
                // TODO: also set FastFall!
                // current.algorithm.setWalkOnly(!current.timeSinceLastLagBack.passed(1_000L) ? 3.0 : null);
                if (current.timeSinceLastLagBack.passed(5_000)) {
                    current.lagbacks = 0;
                } else if (current.lagbacks > 4) {
                    reset(Result.LAG);
                    return;
                }

                current.laggedThisTick = false;
                MovementNode next = current.currentNode.next();
                if (next != null) {
                    // Verify that since we calculated this MovementNode the world has not changed
                    // It could be that a new block has been placed, blocking our way to the next MovementNode
                    MovementPlayer movementPlayer = current.algorithm.getPlayer();
                    movementPlayer.setSpeed((float) player.getAttributeValue(Attributes.MOVEMENT_SPEED));
                    // We do this by setting a player on the old node, then moving him to the next node
                    // If everything is ok, he will reach the target
                    current.currentNode.apply(movementPlayer);
                    movementPlayer.setMoveCallback(next.getMoveEventDeltaFunction(movementPlayer));
                    movementPlayer.aiTravel();
                    if (!next.positionEquals(movementPlayer.position())) {
                        // We were not able to get to the next MovementNode, reset next and calculate again
                        log.error("Failed to verify next position: " + movementPlayer.position() + ", delta: " + movementPlayer.getDeltaMovement() + " vs " + next);
                        // current.currentNode.next(null);
                        current.resetToPlayer(player);
                        next = null;
                    }
                }

                if (next == null) {
                    current.currentNode.next(null);
                    current.algorithm.setCurrentMove(current.currentNode);
                    for (int i = 0; i < MovementPathfindingAlgorithm.NO_MOVE_THRESHOLD; i++) {
                        if (current.algorithm.update()) {
                            next = current.currentNode.next();
                            if (next != null) {
                                break;
                            }
                        } else {
                            log.info("Failed update from " + current.currentNode + " to " + current.algorithm.getGoal());
                            break;
                        }
                    }

                    if (next == null) {
                        logFailedPath("Failed to find path to " + PositionUtil.toSimpleString(current.algorithm.getGoal()));
                        reset(Result.FAILED);
                        return;
                    }
                }

                Vec3 deltaDuringMoveEvent = next.deltaDuringMoveEvent();
                Vec3 deltaReturnedForMoveEvent = next.deltaReturnedForMoveEvent();
                if (player instanceof LocalPlayer lp && lp.input.shiftKeyDown) {
                    float sneakSpeedModifier = Mth.clamp(0.3f + EnchantmentHelper.getSneakingSpeedBonus(lp), 0.0f, 1.0f);
                    deltaDuringMoveEvent = deltaDuringMoveEvent.multiply(sneakSpeedModifier, 1.0, sneakSpeedModifier);
                    deltaReturnedForMoveEvent = deltaReturnedForMoveEvent.multiply(sneakSpeedModifier, 1.0, sneakSpeedModifier);
                }

                player.setDeltaMovement(deltaDuringMoveEvent);
                handleMovement(event, player, deltaReturnedForMoveEvent);
                current.setCurrentNode(next);
            }
        });

        if (render) {
            listen(new Listener<RenderEvent>() {
                @Override
                public void onEvent(RenderEvent event) {
                    State currentState = state;
                    if (currentState != null) {
                        currentState.algorithmRenderer.render(event);
                        pathRenderer.renderPath(event, currentState.path);
                    }
                }
            });
        }

        if (lag) {
            listen(new SafeListener<LagbackEvent>(pingBypass.getMinecraft()) {
                @Override
                public void onEvent(LagbackEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                    State current = state;
                    if (current != null) {
                        current.resetToPlayer(player, true);
                        current.timeSinceLastLagBack.reset();
                        current.algorithm.setWalkTicks(5);
                        current.lagbacks++;
                    }
                }
            });
        }

        listen(new Listener<StepHeightEvent>() {
            @Override
            public void onEvent(StepHeightEvent event) {
                State current = state;
                if (current != null && getPlayer(event.getPlayer()) == event.getPlayer() && current.algorithm.getPlayer().getMovement().canStep(event.getPlayer())) {
                    event.setHeight(current.algorithm.getPlayer().getMovement().getStepHeight());
                }
            }
        });

        ResetUtil.onRespawnOrWorldChange(this, pingBypass.getMinecraft(), () -> reset(Result.WORLD_CHANGED));
    }

    /**
     * @return time to wait with moving after lagging.
     */
    public int getLagTime() {
        return 250;
    }

    /**
     * @return {@code true} if this pathfinder is currently following a path.
     */
    public boolean isFollowingPath() {
        return state != null;
    }

    /**
     * @return the future representing the completion of the current pathfinding process or {@code null} if not following a path.
     */
    public @Nullable CompletableFuture<Result> getCurrentFuture() {
        State current = this.state;
        return current == null ? null : current.future;
    }

    /**
     * Follows the given path of {@link MeshNode}s.
     *
     * @param phobot       the phobot instance of which the {@link Movement} will be used.
     * @param meshNodePath the path of {@link MeshNode}s to follow.
     * @param goal         the exact goal position to reach.
     * @return a {@link CancellableFuture} representing the state of this task, yielding a {@link Result}.
     */
    public CancellableFuture<Result> follow(Phobot phobot, Algorithm.Result<MeshNode> meshNodePath, Vec3 goal) {
        return followScheduled(phobot, phobot.getMinecraft(), (action, orElse) -> NullabilityUtil.safeOr(phobot.getMinecraft(), action, orElse), meshNodePath, goal);
    }

    public void cancel() {
        reset(Result.CANCELLED);
    }

    @VisibleForTesting
    protected CancellableFuture<Result> followScheduled(Phobot phobot,
                                                        BlockableEventLoop<Runnable> eventLoop,
                                                        BiConsumer<NullabilityUtil.PlayerLevelAndGameModeConsumer, Runnable> nullabilityAction,
                                                        Algorithm.Result<MeshNode> meshNodePath,
                                                        Vec3 goal) {
        // TODO: before we follow a new path, run a MovementPathfindingAlgorithm really quick and check if that Algorithm actually reaches a MeshNode on the path?
        //  could be necessary in case we have calculated the MeshNode path on an older player position but have since then moved and now cannot follow the new path.
        Cancellation cancellation = new Cancellation();
        CancellableFuture<Result> future = new CancellableFuture<>(cancellation);
        eventLoop.submit(() -> nullabilityAction.accept(((localPlayer, level, gameMode) -> {
            Player player = getPlayer(localPlayer);
            if (player == null) {
                future.complete(Result.GET_PLAYER_FAILURE);
                return;
            }
            // Step Movement is not handled if this is not a LocalPlayer, so we need to give the player movement
            if (player instanceof MovementPlayer movementPlayer) {
                movementPlayer.setMovement(phobot.getMovementService().getMovement());
            }

            State current = this.state;
            MovementNode start = null;
            if (current != null) {
                current.future.complete(Result.NEW_PATH);
                start = current.currentNode;
            }

            Path<MeshNode> path = new Path<>(
                    player.position(),
                    goal,
                    BlockPos.containing(player.position()),
                    BlockPos.containing(goal),
                    meshNodePath.order(Algorithm.Result.Order.START_TO_GOAL).getPath(),
                    MeshNode.class
            );

            MovementPathfindingAlgorithm algorithm = new MovementPathfindingAlgorithm(phobot, level, path, player, start, null);
            log.info("Starting MovementPathfinder on " + player.position() + ", start: " + start + ", algorithm start: " + algorithm.getStart() + ", algo pos: " + algorithm.getPlayer().position());
            // TODO: keep lagbacks and laggedThisTick?
            this.state = new State(cancellation, future, new AlgorithmRenderer<>(algorithm), algorithm, path, algorithm.getStart(), false, 0);
        }), () -> future.complete(Result.WORLD_CHANGED)));

        return future;
    }

    private void reset(Result result) {
        State current = this.state;
        if (current != null) {
            current.future.complete(result);
        }

        this.state = null;
    }

    /**
     * By overriding this method, this Pathfinder can work against other players.
     *
     * @param localPlayer the {@link LocalPlayer}.
     * @return the player to move.
     */
    protected @Nullable Player getPlayer(Player localPlayer) {
        return localPlayer;
    }

    protected void applyMovementNode(Player player, MovementNode movementNode) {
        movementNode.apply(player);
    }

    protected void handlePlayerDidNotReachPosition(State current, Player player) {
        // TODO: investigate reasons for this?!
        if (!current.currentNode.positionEquals(player.position())) {
            Movement.State movementState = new Movement.State();
            movementState.setDelta(player.getDeltaMovement());
            log.error("Failed to reach current node: \n    " + current.currentNode + "\n    vs actually reached:\n    " +
                    new MovementNode(player, current.currentNode.state(), current.currentNode.goal(), current.currentNode.targetNodeIndex(),
                            current.currentNode.deltaDuringMoveEvent(), player.getDeltaMovement()));
            current.resetToPlayer(player);
        }
    }

    protected void handleMovement(MoveEvent event, Player player, Vec3 delta) {
        event.setVec(delta);
    }

    protected void logFailedPath(String message) {
        pingBypass.getChat().send(Component.literal(message).withStyle(RED), "MovementPathfinder");
    }

    /**
     * The result of following a path.
     */
    public enum Result {
        /**
         * Successfully reached the goal.
         */
        FINISHED,
        /**
         * Cancelled via the {@link CancellableFuture}.
         */
        CANCELLED,
        /**
         * Cancelled because the world changed or we respawned.
         */
        WORLD_CHANGED,
        /**
         * Cancelled because we could not stop lagging.
         */
        LAG,
        /**
         * Failed to find a path, reasons for this could be that the world changed, e.g. we could have gotten trapped.
         */
        FAILED,
        /**
         * A new path was set to follow, cancelling the old one.
         */
        NEW_PATH,
        /**
         * An unexpected error when {@link #getPlayer(Player)} returns {@code null}.
         */
        GET_PLAYER_FAILURE,
    }

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor(access = AccessLevel.NONE)
    protected static class State {
        private final StopWatch.ForSingleThread timeSinceLastLagBack = new StopWatch.ForSingleThread();
        private final Cancellation cancellation;
        private final CancellableFuture<Result> future;
        private final AlgorithmRenderer<MovementNode> algorithmRenderer;
        private final MovementPathfindingAlgorithm algorithm;
        private final Path<MeshNode> path;

        private MovementNode currentNode;
        private boolean laggedThisTick;
        private int lagbacks;

        public void resetToPlayer(Player player) {
            resetToPlayer(player, false);
        }

        public void resetToPlayer(Player player, boolean resetState) {
            MovementNode current = currentNode;
            if (current == null) {
                return;
            }

            Movement.State state = new Movement.State();
            state.setDelta(player.getDeltaMovement());
            if (resetState) {
                state.reset();
            }

            // TODO: we need to be careful about the targetNodeIndex!!! Its possible that we cant reach this targetNodeIndex from the spot we got lagged back to?
            MovementNode node = new MovementNode(player, state, algorithm.getGoal(), currentNode.targetNodeIndex());
            currentNode = node;
            algorithm.setCurrentMove(node);
            algorithm.setCurrentRender(node);
        }
    }
    
}
