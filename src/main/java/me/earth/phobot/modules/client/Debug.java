package me.earth.phobot.modules.client;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.ducks.IAbstractClientPlayer;
import me.earth.phobot.event.LocalPlayerRenderEvent;
import me.earth.phobot.event.PostMotionPlayerUpdateEvent;
import me.earth.phobot.event.RenderEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.modules.combat.Blocker;
import me.earth.phobot.services.BlockDestructionService;
import me.earth.phobot.services.MotionUpdateService;
import me.earth.phobot.util.math.MathUtil;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.player.PredictionPlayer;
import me.earth.phobot.util.render.Renderer;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static me.earth.phobot.services.PlayerPredictionService.DISTANCE_SQUARED;

@Slf4j
public class Debug extends PhobotModule {
    private final Setting<Boolean> renderBreak = bool("Break", false, "Renders a break ESP for blocks that get broken around us.");
    private final Setting<Boolean> renderPrediction = bool("Prediction", false, "Renders the predicted path for all players around us.");
    private final Setting<Boolean> closestCrystalDamage = bool("ClosestCrystalDamage", false, "Prints out the damage the closest crystal to you deals.");
    private final Setting<Boolean> f5Rotations = bool("F5Rotations", true, "Renders your rotations in F5.");
    private LocalPlayerRenderEvent localPlayerRenderEvent;

    public Debug(Phobot phobot) {
        super(phobot, "Debug", Categories.CLIENT, "For debugging purposes.");
        this.register(phobot.getLocalPlayerPositionService().getFixLast());
        listen(new SafeListener<RenderEvent>(mc) {
            @Override
            public void onEvent(RenderEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (renderBreak.getValue()) {
                    for (BlockDestructionService.Progress progress : phobot.getBlockDestructionService().getPositions().values()) {
                        event.getAabb().set(progress.pos());
                        event.setBoxColor(Color.YELLOW, 0.2f);
                        float damageDelta = Mth.clamp(Blocker.getMaxDigSpeed(progress.pos(), Blocks.OBSIDIAN.defaultBlockState(), level), 0.0f, 1.0f);
                        float damage = damageDelta * TimeUtil.getPassedTimeSince(progress.timeStamp()) / 50L;
                        float previousDamage = damage - damageDelta;
                        event.getAabb().grow(-0.5 + Mth.clamp(Mth.lerp(event.getTickDelta(), previousDamage / 2.0, damage / 2.0), 0.0, 0.5));
                        Renderer.renderBoxWithOutlineAndSides(event, 1.0f, true);
                    }
                }

                if (renderPrediction.getValue()) {
                    event.getLineColor().set(Color.WHITE);
                    for (AbstractClientPlayer clientPlayer : level.players()) {
                        if (clientPlayer != player && clientPlayer.distanceToSqr(player) <= DISTANCE_SQUARED && clientPlayer instanceof IAbstractClientPlayer access) {
                            Renderer.startLines(1.5f, true);
                            event.getFrom().set(clientPlayer.position());
                            for (PredictionPlayer predictionPlayer : access.phobot$getPredictions()) {
                                if (predictionPlayer != null) {
                                    event.getTo().set(predictionPlayer.position());
                                    Renderer.drawLine(event);
                                    event.getFrom().set(event.getTo());
                                }
                            }

                            Renderer.end(true);
                        }
                    }
                }
            }
        });

        listen(new SafeListener<PostMotionPlayerUpdateEvent>(mc) {
            @Override
            public void onEvent(PostMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (closestCrystalDamage.getValue()) {
                    float highest = 0.0f;
                    for (EndCrystal crystal : level.getEntities(EntityType.END_CRYSTAL, PositionUtil.getAABBOfRadius(player, 12.0), endCrystal -> true)) {
                        float damage = phobot.getAntiCheat().getDamageCalculator().getDamage(player, level, crystal);
                        if (damage > highest) {
                            highest = damage;
                        }
                    }

                    getPingBypass().getChat().send(Component.literal("Crystal-Damage: ").append(Component.literal(MathUtil.round(highest, 2) + "").withStyle(ChatFormatting.RED)), "DebugCrystalDmg");
                }
            }
        });

        listen(new Listener<LocalPlayerRenderEvent>() {
            @Override
            public void onEvent(LocalPlayerRenderEvent event) {
                LocalPlayer player;
                if (f5Rotations.getValue() && (player = mc.player) != null) {
                    if (event.pre()) {
                        MotionUpdateService updateService = phobot.getMotionUpdateService();
                        localPlayerRenderEvent = event;
                        // TODO: probably needs some form of interpolation
                        player.setYRot(updateService.getChangedYRot());
                        player.setXRot(updateService.getChangedXRot());

                        player.xRotO = updateService.getXRotO();
                        player.yRotO = updateService.getYRotO();

                        player.yHeadRot = updateService.getChangedYRot();
                        player.yBodyRot = updateService.getChangedYRot();

                        player.yHeadRotO = updateService.getYRotO();
                        player.yBodyRotO = updateService.getYRotO();
                    } else if (localPlayerRenderEvent != null) {
                        player.setXRot(localPlayerRenderEvent.previousXRot());
                        player.setYRot(localPlayerRenderEvent.previousYRot());

                        player.xRotO = localPlayerRenderEvent.previousXRotO();
                        player.yRotO = localPlayerRenderEvent.previousYRotO();

                        player.yHeadRot = localPlayerRenderEvent.previousHeadYRot();
                        player.yBodyRot = localPlayerRenderEvent.previousBodyYRot();

                        player.yHeadRotO = localPlayerRenderEvent.previousHeadYRotO();
                        player.yBodyRotO = localPlayerRenderEvent.previousBodyYRotO();
                    }
                }
            }
        });
    }

    @Override
    protected void onEnable() {
        dumpWorld();
    }

    @SuppressWarnings("unused")
    private void dumpWorld() {
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (player != null && level != null) {
            StringBuilder builder = new StringBuilder();
            builder.append("[\n");
            boolean first = true;
            for (int x = -10; x < 10; x++) {
                for (int y = -10; y < 10; y++) {
                    for (int z = -10; z < 10; z++) {
                        BlockPos pos = player.getOnPos().offset(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir()) {
                            var key = state.getBlockHolder().unwrapKey();
                            if (key.isPresent()) {
                                if (first) {
                                    builder.append("{ ");
                                    first = false;
                                } else {
                                    builder.append(",\n{ ");
                                }

                                builder.append(" \"x\": ")
                                       .append(x)
                                       .append(", \"y\": ")
                                       .append(y)
                                       .append(", \"z\": ")
                                       .append(z)
                                       .append(", \"block\": \"")
                                       .append(key.get().location())
                                       .append("\" }");
                            }
                        }
                    }
                }
            }

            builder.append("\n]");
            try (FileOutputStream fos = new FileOutputStream("worlddump.json")) {
                fos.write(builder.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                log.error("Failed to save world dump", e);
            }
        }
    }

}
