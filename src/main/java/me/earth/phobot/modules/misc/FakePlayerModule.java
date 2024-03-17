package me.earth.phobot.modules.misc;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.earth.phobot.Phobot;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.event.MoveEvent;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.modules.AbstractFakePlayerModule;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.movement.MovementNode;
import me.earth.phobot.pathfinder.movement.MovementPathfinder;
import me.earth.phobot.pathfinder.parallelization.ParallelPathSearch;
import me.earth.phobot.pathfinder.parallelization.ParallelSearchManager;
import me.earth.phobot.services.PlayerPosition;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.math.RotationUtil;
import me.earth.phobot.util.mutables.MutVec3;
import me.earth.phobot.util.player.FakePlayer;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.pingbypass.api.command.CommandSource;
import me.earth.pingbypass.api.command.impl.module.HasCustomModuleCommand;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static me.earth.pingbypass.api.command.CommandSource.argument;
import static me.earth.pingbypass.api.command.CommandSource.literal;

/**
 * Spawns a {@link FakePlayer}.
 */
public class FakePlayerModule extends AbstractFakePlayerModule implements HasCustomModuleCommand {
    private static final int ID = -2352352;

    private final Setting<Boolean> pathfinder = bool("Pathfinder", false, "Uses the pathfinder to move around you.");
    private final Setting<Boolean> record = bool("Record", false, "Records your movements.");
    private final Setting<Boolean> play = bool("Play", false, "Plays recorded movements.");
    private final List<PlayerPosition> positions = new ArrayList<>();
    private final ParallelSearchManager search = new ParallelSearchManager();
    private final MovementPathfinder movementPathfinder;

    private boolean wasRecording;
    private int playIndex;

    private MovementPlayer movementPathfinderPlayer;
    private volatile MeshNode currentMeshNode;

    public FakePlayerModule(Phobot phobot) {
        super(phobot, "FakePlayer", Categories.MISC, "Creates a FakePlayer to test stuff with.", ID);
        this.movementPathfinder = new FakePlayerMovementPathfinder(this);
        this.movementPathfinder.getListeners().forEach(this::listen);
        ResetUtil.disableOnRespawnAndWorldChange(this, mc);
        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (pathfinder.getValue()) {
                    record.setValue(false);
                    wasRecording = false;
                    doPathfinding(player, level);
                    return;
                }

                cancelPathfinding();
                if (record.getValue()) {
                    if (!wasRecording) {
                        positions.clear();
                    }

                    wasRecording = true;
                    positions.add(new PlayerPosition(player));
                    playIndex = 0;
                } else if (play.getValue() && !positions.isEmpty()) {
                    wasRecording = false;
                    if (playIndex >= positions.size()) {
                        playIndex = 0;
                    }

                    PlayerPosition position = positions.get(playIndex);
                    Entity entity = getPlayer(level);
                    if (entity != null) {
                        entity.lerpTo(position.getX(), position.getY(), position.getZ(), position.getYRot(), position.getXRot(), 3);
                    }

                    playIndex++;
                } else {
                    wasRecording = false;
                    playIndex = 0;
                }
            }
        });
    }

    @Override
    protected void onDisable() {
        record.setValue(false);
        playIndex = 0;
        super.onDisable();
        cancelPathfinding();
        movementPathfinderPlayer = null;
    }

    @Override
    protected void onEnable() {
        wasRecording = false;
        playIndex = 0;
        super.onEnable();
    }

    @Override
    protected FakePlayer create(LocalPlayer player, ClientLevel level) {
        FakePlayer fakePlayer = super.create(player, level);
        MovementPlayer movementPathfinderPlayer = new MovementPlayer(level);
        movementPathfinderPlayer.copyPosition(fakePlayer);
        this.movementPathfinderPlayer = movementPathfinderPlayer;
        return fakePlayer;
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // TODO: ticks arg does not show up in help etc.!!!
        builder.then(literal("cut")
                .then(literal("start").then(argument("ticks", integer(0)).executes(ctx -> cut(ctx, true))))
                .then(literal("end").then(argument("ticks", integer(0)).executes(ctx -> cut(ctx, false)))));
    }

    private void cut(CommandContext<CommandSource> ctx, boolean start) {
        int ticks = ctx.getArgument("ticks", Integer.class);
        for (int i = 0; i < ticks; i++) {
            if (positions.isEmpty()) {
                break;
            }

            if (start) {
                positions.remove(0);
            } else {
                positions.remove(positions.size() - 1);
            }
        }

        String result = "Cut " + ticks + " ticks from the " + (start ? "start" : "end") + ". "
                + (positions.isEmpty() ? "No ticks remain." : positions.size() + " ticks remain.");
        getPingBypass().getChat().send(Component.literal(result), "FakePlayerCut");
    }

    private void cancelPathfinding() {
        currentMeshNode = null;
        search.cancel();
        movementPathfinder.cancel();
    }

    private void doPathfinding(LocalPlayer player, ClientLevel level) {
        MeshNode currentMeshNode = this.currentMeshNode;
        if (currentMeshNode == null) {
            findInitialMeshNodes(player, level);
        } else if (!movementPathfinder.isFollowingPath() && !search.isSearching()) {
            Entity fakePlayer = getPlayer(level);
            if (fakePlayer != null) {
                var optionalStart = phobot.getNavigationMeshManager().getStartNode(fakePlayer);
                optionalStart.ifPresent(start -> {
                    this.currentMeshNode = start;
                    var closestMeshNodes = phobot.getNavigationMeshManager().getMap().values().stream()
                            .filter(meshNode -> meshNode.distanceSq(player.position()) < 100.0)
                            .filter(meshNode -> meshNode.distanceSq(start) > 36.0)
                            .filter(meshNode -> meshNode != start)
                            .collect(Collectors.toList());

                    Collections.shuffle(closestMeshNodes);
                    if (closestMeshNodes.isEmpty()) {
                        getPingBypass().getChat().send(Component.literal("Failed to find a path for the FakePlayer!").withStyle(ChatFormatting.RED), "FakePlayer");
                        return;
                    }

                    search.<MeshNode>applyForPathSearch(() -> 0, multiPathSearch -> {
                        for (MeshNode goal : closestMeshNodes) {
                            multiPathSearch.addFuture(goal, phobot.getPathfinder().findPath(start, goal, false));
                            if (multiPathSearch.getFutures().size() >= getPingBypass().getModuleManager().getByClass(Bot.class).orElseThrow().getParallelSearches().getValue()) {
                                break;
                            }
                        }

                        addCompletion(multiPathSearch, level);
                    });
                });
            }
        }
    }

    private void findInitialMeshNodes(LocalPlayer player, ClientLevel level) {
        if (search.isSearching()) {
            return;
        }

        var closestMeshNodes = phobot.getNavigationMeshManager().getMap().values().stream()
                .filter(meshNode -> meshNode.distanceSq(player.position()) < 100.0)
                .collect(Collectors.toList());
        Collections.shuffle(closestMeshNodes);
        if (closestMeshNodes.size() < 2) {
            getPingBypass().getChat().send(Component.literal("Failed to find a path for the FakePlayer!").withStyle(ChatFormatting.RED), "FakePlayer");
            return;
        }

        search.<MeshNode>applyForPathSearch(() -> 0, multiPathSearch -> {
            for (int i = 0; i < 10; i += 2) {
                if (i >= closestMeshNodes.size() - 1) {
                    break;
                }

                MeshNode start = closestMeshNodes.get(i);
                MeshNode goal = closestMeshNodes.get(i + 1);
                multiPathSearch.addFuture(goal, phobot.getPathfinder().findPath(start, goal, false));
                multiPathSearch.getFuture().thenAccept(result -> {
                    if (result.key() == goal) {
                        mc.submit(() -> {
                            Player movementPathfinderPlayer = this.movementPathfinderPlayer;
                            if (movementPathfinderPlayer != null) {
                                movementPathfinderPlayer.setPos(start.getCenter(new BlockPos.MutableBlockPos(), level));
                            }
                        });
                    }
                });
            }

            addCompletion(multiPathSearch, level);
        });
    }

    private void addCompletion(ParallelPathSearch<MeshNode> multiPathSearch, ClientLevel level) {
        multiPathSearch.getFuture().whenComplete((result, t) -> mc.submit(() -> {
            if (result != null && this.isEnabled()) {
                currentMeshNode = result.key();
                Vec3 goal = result.key().getCenter(new BlockPos.MutableBlockPos(), level);
                this.movementPathfinder.follow(phobot, result.algorithmResult(), goal);
            }
        }));
    }

    private static final class FakePlayerMovementPathfinder extends MovementPathfinder {
        private final FakePlayerModule fakePlayerModule;
        private final MutVec3 vec = new MutVec3();

        public FakePlayerMovementPathfinder(FakePlayerModule fakePlayerModule) {
            super(fakePlayerModule.getPingBypass(), false, false);
            this.fakePlayerModule = fakePlayerModule;
        }

        @Override
        protected @Nullable Player getPlayer(Player localPlayer) {
            return fakePlayerModule.movementPathfinderPlayer;
        }

        @Override
        protected void applyMovementNode(Player player, MovementNode movementNode) {
            super.applyMovementNode(player, movementNode);
            ClientLevel level = fakePlayerModule.mc.level;
            if (level != null) {
                Entity entity = fakePlayerModule.getPlayer(level);
                if (entity != null) {
                    vec.set(player.getDeltaMovement());
                    float[] rotations = RotationUtil.lookIntoMoveDirection(player, vec);
                    entity.lerpTo(player.getX(), player.getY(), player.getZ(), rotations[0], rotations[1], 3);
                }
            }
        }

        @Override
        protected void handlePlayerDidNotReachPosition(State current, Player player) {
            // no need to handle
        }

        @Override
        protected void handleMovement(MoveEvent event, Player player, Vec3 delta) {
            // player.travel(delta);
        }

        @Override
        protected void logFailedPath(String message) {
            // no logging
        }
    }

}
