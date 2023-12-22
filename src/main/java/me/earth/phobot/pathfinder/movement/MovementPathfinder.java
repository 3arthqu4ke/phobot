package me.earth.phobot.pathfinder.movement;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.earth.phobot.Phobot;
import me.earth.phobot.event.MoveEvent;
import me.earth.phobot.event.PathfinderUpdateEvent;
import me.earth.phobot.event.RenderEvent;
import me.earth.phobot.pathfinder.Path;
import me.earth.phobot.pathfinder.algorithm.AlgorithmRenderer;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.util.NullabilityUtil;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.commons.event.SafeListener;
import me.earth.pingbypass.commons.event.network.PacketEvent;
import me.earth.pingbypass.commons.event.network.ReceiveListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

import static net.minecraft.ChatFormatting.RED;

public class MovementPathfinder extends SubscriberImpl {
    protected State state;

    public MovementPathfinder(PingBypass pingBypass) {
        listen(new SafeListener<PathfinderUpdateEvent>(pingBypass.getMinecraft()) {
            @Override
            public void onEvent(PathfinderUpdateEvent event, LocalPlayer localPlayer, ClientLevel clientLevel, MultiPlayerGameMode multiPlayerGameMode) {
                State currentState = state;
                if (currentState == null) {
                    return;
                }

                currentState.currentNode.apply(localPlayer);
            }
        });

        listen(new Listener<MoveEvent>() {
            @Override
            public void onEvent(MoveEvent event) {
                State current = state;
                if (current == null) {
                    return;
                }

                if (current.currentNode.isGoal()) {
                    state = null;
                    return;
                }

                MovementNode next = current.currentNode.next();
                if (next == null) {
                    if (current.algorithm.update()) {
                        next = current.currentNode.next();
                        if (next == null) {
                            pingBypass.getChat().send(Component.literal("Failed to update Pathfinder at "
                                    + current.algorithm.getCurrent() + "! (Next was null)").withStyle(RED), getClass().getName());
                            state = null;
                            return;
                        }
                    } else {
                        pingBypass.getChat().send(Component.literal("Failed to update Pathfinder at "
                                + current.algorithm.getCurrent() + "!").withStyle(RED), getClass().getName());
                        state = null;
                        return;
                    }
                }

                event.setVec(next.state().getDelta());
                current.setCurrentNode(next);
            }
        });

        listen(new ReceiveListener.Scheduled.Safe<ClientboundPlayerPositionPacket>(pingBypass.getMinecraft()) {
            @Override
            public void onSafeEvent(PacketEvent.Receive<ClientboundPlayerPositionPacket> event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                state = null;
                pingBypass.getChat().send(Component.literal("Aborting pathfinding process because of lagback!").withStyle(RED), getClass().getName());
                // TODO: attempt recovery?!
                // TODO: Surround?
            }
        });

        listen(new Listener<RenderEvent>() {
            @Override
            public void onEvent(RenderEvent event) {
                State currentState = state;
                if (currentState != null) {
                    currentState.algorithmRenderer.render(event);
                }
            }
        });
    }

    public void follow(Phobot phobot, Path<MeshNode> path) {
        phobot.getMinecraft().submit(() -> NullabilityUtil.safe(phobot.getMinecraft(), ((localPlayer, level, gameMode) -> {
            MovementPathfindingAlgorithm algorithm = new MovementPathfindingAlgorithm(phobot, level, path, localPlayer, null, null);
            this.state = new State(new AlgorithmRenderer<>(algorithm), algorithm, path, algorithm.getStart());
        })));
    }

    @Data
    @AllArgsConstructor
    protected static class State {
        protected final AlgorithmRenderer<MovementNode> algorithmRenderer;
        protected final MovementPathfindingAlgorithm algorithm;
        protected final Path<MeshNode> path;
        protected MovementNode currentNode;
    }

}
