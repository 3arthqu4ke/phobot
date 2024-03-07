package me.earth.phobot.pathfinder.movement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.event.LagbackEvent;
import me.earth.phobot.event.MoveEvent;
import me.earth.phobot.event.PathfinderUpdateEvent;
import me.earth.phobot.event.RenderEvent;
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
import me.earth.phobot.services.LagbackService;
import me.earth.phobot.util.NullabilityUtil;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.SafeListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

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
    protected State state;

    public MovementPathfinder(PingBypass pingBypass) {
        listen(new SafeListener<PathfinderUpdateEvent>(pingBypass.getMinecraft()) {
            @Override
            public void onEvent(PathfinderUpdateEvent event, LocalPlayer localPlayer, ClientLevel clientLevel, MultiPlayerGameMode multiPlayerGameMode) {
                State current = state;
                if (current == null) {
                    return;
                }

                if (!current.currentNode.positionEquals(localPlayer.position())) {
                    log.error("Failed to reach current node: " + localPlayer.position() + " vs " + current.currentNode);
                }

                if (current.currentNode.isGoal()) {
                    localPlayer.setDeltaMovement(new Vec3(0.0, localPlayer.getDeltaMovement().y, 0.0));
                    reset(Result.FINISHED);
                    return;
                }

                if (current.laggedThisTick) {
                    current.resetToPlayer(localPlayer);
                } else {
                    current.currentNode.apply(localPlayer);
                }
            }
        });

        listen(new SafeListener<MoveEvent>(pingBypass.getMinecraft(), Integer.MIN_VALUE + 1000) { // this should happen late
            @Override
            public void onEvent(MoveEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                State current = state;
                if (current == null) {
                    return;
                }

                if (current.cancellation.isCancelled()) {
                    reset(Result.CANCELLED);
                    return;
                }

                current.algorithm.updatePotionEffects(player.getActiveEffects());
                // TODO: also set FastFall!
                current.algorithm.setWalkOnly(!current.timeSinceLastLagBack.passed(250L));
                if (current.timeSinceLastLagBack.passed(5_000)) {
                    current.lagbacks = 0;
                } else if (current.lagbacks > 4) {
                    reset(Result.LAG);
                    return;
                }

                current.laggedThisTick = false;
                MovementNode next = current.currentNode.next();
                Vec3 delta;
                if (next != null) {
                    delta = new Vec3(next.getX() - player.getX(), next.getY() - player.getY(), next.getZ() - player.getZ());
                    // Verify that since we calculated this MovementNode the world has not changed
                    // It could be that a new block has been placed, blocking our way to the next MovementNode
                    MovementPlayer movementPlayer = current.algorithm.getPlayer();
                    // We do this by setting a player on the old node, then moving him to the next node
                    // If everything is ok, he will reach the target
                    current.currentNode.apply(movementPlayer);
                    movementPlayer.setMoveCallback(Function.identity());
                    movementPlayer.setDeltaMovement(delta);
                    movementPlayer.move(MoverType.PLAYER, movementPlayer.getDeltaMovement());
                    if (!next.positionEquals(movementPlayer.position())) {
                        // We were not able to get to the next MovementNode, reset next and calculate again
                        log.error("Failed to verify next position: " + movementPlayer.position() + " vs " + next);
                        current.currentNode.next(null);
                        next = null;
                    }
                }

                if (next == null) {
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
                        pingBypass.getChat().send(Component.literal("Failed to find path to " + PositionUtil.toSimpleString(current.algorithm.getGoal())).withStyle(RED));
                        reset(Result.FAILED);
                        return;
                    }
                }

                delta = new Vec3(next.getX() - player.getX(), next.getY() - player.getY(), next.getZ() - player.getZ());
                event.setVec(delta);
                player.setDeltaMovement(delta);
                current.setCurrentNode(next);
            }
        });

        listen(new SafeListener<LagbackEvent>(pingBypass.getMinecraft()) {
            @Override
            public void onEvent(LagbackEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                State current = state;
                if (current != null) {
                    current.resetToPlayer(player);
                    current.timeSinceLastLagBack.reset();
                    current.lagbacks++;
                }
            }
        });

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

        ResetUtil.onRespawnOrWorldChange(this, pingBypass.getMinecraft(), () -> reset(Result.WORLD_CHANGED));
    }

    /**
     * @return {@code true} if this pathfinder is currently following a path.
     */
    public boolean isFollowingPath() {
        return state != null;
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
        Cancellation cancellation = new Cancellation();
        CancellableFuture<Result> future = new CancellableFuture<>(cancellation);
        phobot.getMinecraft().submit(() -> NullabilityUtil.safeOr(phobot.getMinecraft(), ((localPlayer, level, gameMode) -> {
            State current = this.state;
            MovementNode start = null;
            if (current != null) {
                current.future.complete(Result.NEW_PATH);
                start = current.currentNode;
            }

            Path<MeshNode> path = new Path<>(
                    localPlayer.position(),
                    goal,
                    BlockPos.containing(localPlayer.position()),
                    BlockPos.containing(goal),
                    meshNodePath.order(Algorithm.Result.Order.START_TO_GOAL).getPath(),
                    MeshNode.class
            );

            MovementPathfindingAlgorithm algorithm = new MovementPathfindingAlgorithm(phobot, level, path, localPlayer, start, null);
            this.state = new State(cancellation, future, new AlgorithmRenderer<>(algorithm), algorithm, path, algorithm.getStart(), false, 0);
            this.state.resetToPlayer(localPlayer);
            algorithm.setStart(this.state.currentNode);
        }), () -> future.complete(Result.WORLD_CHANGED)));

        return future;
    }

    public void cancel() {
        reset(Result.CANCELLED);
    }

    private void reset(Result result) {
        State current = this.state;
        if (current != null) {
            current.future.complete(result);
        }

        this.state = null;
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
        NEW_PATH
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

        public void resetToPlayer(LocalPlayer player) {
            MovementNode current = currentNode;
            if (current == null) {
                return;
            }

            Movement.State state = new Movement.State();
            state.setDelta(player.getDeltaMovement());
            MovementNode node = new MovementNode(player, state, algorithm.getGoal(), currentNode.targetNodeIndex());
            currentNode = node;
            algorithm.setCurrentMove(node);
            algorithm.setCurrentRender(node);
        }
    }
    
}
