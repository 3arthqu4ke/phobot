package me.earth.phobot.modules.misc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.event.*;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.modules.client.anticheat.StrictDirection;
import me.earth.phobot.services.PlayerPosition;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.math.MathUtil;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.math.RaytraceUtil;
import me.earth.phobot.util.math.RotationUtil;
import me.earth.phobot.util.render.Renderer;
import me.earth.phobot.util.time.StopWatch;
import me.earth.phobot.util.world.BlockStateLevel;
import me.earth.phobot.util.world.PredictionUtil;
import me.earth.pingbypass.PingBypassApi;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.event.CancellableEvent;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.gui.hud.DisplaysHudInfo;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.api.setting.impl.Complexities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

// TODO: test what happens when target is out of range more, instead of just aborting
// TODO: make the rendering a bit smarter
@Slf4j
public class Speedmine extends PhobotModule implements DisplaysHudInfo {
    private final Setting<Boolean> fast = bool("Fast", true, "Allows you mine blocks quicker if you place them on the same position.");
    private final Setting<Boolean> silentSwitch = bool("Switch", true, "Silently switches to your tool to mine.");
    private final Setting<Boolean> noGlitchBlocks = bool("NoGlitchBlocks", false, "If off sets the block to air on the clientside immediately.");
    private final Setting<Boolean> swing = bool("Swing", false, "Swings every tick.");
    private final Setting<Boolean> addTick = boolBuilder("AddTick", false).withDescription("Waits another tick before breaking.").withComplexity(Complexities.DEV).register(this);
    private final StopWatch.ForSingleThread expectingAirTimer = new StopWatch.ForSingleThread();
    private final StopWatch.ForSingleThread timer = new StopWatch.ForSingleThread();

    @Getter
    private BlockPos currentPos = null;
    private BlockState currentState = Blocks.AIR.defaultBlockState();
    private List<AABB> renderBBs = Collections.emptyList();
    private float renderDamageDelta = 0.0f;
    private int renderTicks = 0;
    private boolean sendAbortNextTick = true;
    private boolean expectingAir;

    public Speedmine(Phobot phobot) {
        super(phobot, "Speedmine", Categories.MISC, "Better mining.");
        timer.reset();
        listen(new SafeListener<StartDestroyBlockEvent>(mc) {
            @Override
            public void onEvent(StartDestroyBlockEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (player.isCreative() || phobot.getAntiCheat().isAboveBuildHeight(event.getPos().getY())) {
                    reset();
                    return;
                }

                event.setCancelled(true);
                startDestroy(event.getPos(), event.getDirection(), level, player);
            }
        });

        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc, -1000) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (player.isCreative()) {
                    reset();
                    return;
                }

                if (currentPos != null) {
                    Direction currentDirection = getDirection();
                    if (currentDirection == null) {
                        player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, currentPos, Direction.DOWN));
                        reset();
                        return;
                    }

                    // PreMotionPlayerUpdateEvent is before we sent our position to the server, so we can still abort
                    if (player.getEyePosition().distanceToSqr(currentPos.getX() + 0.5, currentPos.getY() + 0.5, currentPos.getZ() + 0.5) > phobot.getAntiCheat().getMiningRangeSq()) {
                        player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, currentPos, currentDirection));
                        reset();
                    } else {
                        renderTicks++;
                        update(level, player, gameMode);
                    }
                }
            }
        });

        listen(new SafeListener<PostMotionPlayerUpdateEvent>(mc) {
            @Override
            public void onEvent(PostMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (player.isCreative()) {
                    reset();
                    return;
                }

                if (currentPos != null) {
                    Direction currentDirection = getDirection();
                    if (currentDirection == null) {
                        player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, currentPos, Direction.DOWN));
                        reset();
                        return;
                    }

                    update(level, player, gameMode);
                }
            }
        });

        listen(new Listener<RenderEvent>() {
            @Override
            public void onEvent(RenderEvent event) {
                if (currentPos != null && !currentState.isAir()) {
                    for (AABB bb : renderBBs) {
                        event.getAabb().set(bb);
                        event.getAabb().move(currentPos.getX(), currentPos.getY(), currentPos.getZ());
                        float damage = MathUtil.clamp(renderDamageDelta * renderTicks, 0.0f, 1.0f);
                        // TODO: proper offsets
                        event.getAabb().grow(-0.5 + MathUtil.clamp((renderDamageDelta * renderTicks / 2.0) + renderDamageDelta * event.getTickDelta() - renderDamageDelta, 0.0, 0.5));
                        event.setBoxColor(1.0f - damage, damage, 0.0f, 1.0f, 0.4f);
                        Renderer.renderBoxWithOutlineAndSides(event, 1.5f, true);
                        return; // TODO: do this properly and fill out the entire voxel shape bbs, LightSectionDebugRenderer has a bit of an example
                    }
                }
            }
        });

        listen(new SafeListener<ContinueDestroyBlockEvent>(mc) {
            @Override
            public void onEvent(ContinueDestroyBlockEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (player.isCreative()) {
                    reset();
                    return;
                }

                event.setCancelled(true);
            }
        });

        listen(new SafeListener<StopDestroyBlockEvent>(mc) {
            @Override
            public void onEvent(StopDestroyBlockEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (player.isCreative()) {
                    reset();
                    return;
                }

                event.setCancelled(true);
            }
        });

        listen(new SafeListener<PacketEvent.PostReceive<ClientboundBlockUpdatePacket>>(mc) {
            @Override
            public void onEvent(PacketEvent.PostReceive<ClientboundBlockUpdatePacket> event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                mc.submit(() -> {
                    if (!player.isCreative() && Objects.equals(event.getPacket().getPos(), currentPos)) {
                        if (expectingAir && event.getPacket().getBlockState().isAir()) {
                            expectingAir = false;
                        }

                        update(level, player, gameMode);
                    }
                });
            }
        });

        listen(new SafeListener<PacketEvent.PostSend<ServerboundSetCarriedItemPacket>>(mc) {
            @Override
            public void onEvent(PacketEvent.PostSend<ServerboundSetCarriedItemPacket> event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                mc.submit(() -> {
                    if (!player.isCreative() && currentPos != null) {
                        Direction currentDirection = getDirection();
                        if (currentDirection == null) {
                            reset();
                            return;
                        }

                        player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, currentPos, currentDirection));
                        player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, currentPos, currentDirection));
                        timer.reset();
                        renderTicks = 0;
                    }
                });
            }
        });

        ResetUtil.onRespawnOrWorldChange(this, mc, this::reset);
    }

    @Override
    public String getHudInfo() {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player != null && level != null && currentPos != null) {
            return getTimeLeftMS(player, level) + "ms";
        }

        return null;
    }

    public int getTimeLeftMS(LocalPlayer player, ClientLevel level) {
        BlockPos current = getCurrentPos();
        if (current == null) {
            return Integer.MAX_VALUE;
        }

        return phobot.getInventoryService().supply(context -> {
            MutableObject<Float> bestDamage = new MutableObject<>(0.0f);
            MutableObject<Float> bestDamageDelta = new MutableObject<>(0.0f);
            calculateSlots(context, player, level, bestDamage, new MutableObject<>(), bestDamageDelta);
            float timeLeft = ((1.0f - bestDamage.getValue()) / bestDamageDelta.getValue()) * 50;
            return (int) timeLeft;
        }, true, Integer.MAX_VALUE);
    }

    private void update(ClientLevel level, LocalPlayer player, MultiPlayerGameMode gameMode) {
        Direction currentDirection = getDirection();
        if (currentDirection == null) {
            reset();
            return;
        }

        if (expectingAir && expectingAirTimer.passed(125L) && currentPos != null) {
            getPingBypass().getChat().send(Component.literal("Failed to mine " + PositionUtil.toSimpleString(currentPos) + " re-mining.").withStyle(ChatFormatting.RED), "Speedmine");
            BlockPos posBeforeReset = currentPos;
            startDestroy(posBeforeReset, currentDirection, level, player); // abort
            startDestroy(posBeforeReset, currentDirection, level, player); // re-mine
            if (currentPos == null) {
                return;
            }
        }

        if (currentPos == null) {
            return;
        }

        currentState = level.getBlockState(currentPos);
        renderBBs = currentState.getShape(level, currentPos).toAabbs();
        if (canMine(currentPos, currentState, player, level)) {
            if (sendAbortNextTick && fast.getValue()) {
                player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, currentPos, currentDirection));
                sendAbortNextTick = false;
            }

            phobot.getInventoryService().use(context -> breakCurrentPos(context, level, player, gameMode, true));
        }
    }

    public boolean breakCurrentPos(InventoryContext context, ClientLevel level, LocalPlayer player, MultiPlayerGameMode gameMode, boolean withEvent) {
        BlockPos currentPos = this.currentPos;
        if (currentPos == null) {
            return false;
        }

        Direction direction = getDirection();
        if (direction == null) {
            return false;
        }

        Slot slot = getBreakingSlot(context, player, level);
        if (slot != null) {
            if (phobot.getAntiCheat().getMiningRotations().getValue()) {
                Player rotationPlayer = phobot.getLocalPlayerPositionService().getPlayerOnLastPosition(player);
                BlockHitResult result = RaytraceUtil.raytraceChecked(rotationPlayer, level, currentPos,
                        r -> phobot.getAntiCheat().getMiningStrictDirectionCheck().strictDirectionCheck(r.getBlockPos(), r.getDirection(), level, rotationPlayer),
                        phobot.getAntiCheat().getOpposite().getValue());
                if (result == null || !phobot.getAntiCheat().getMiningStrictDirectionCheck().strictDirectionCheck(currentPos, result.getDirection(), level, rotationPlayer)) {
                    Direction strictDirection = phobot.getAntiCheat().getMiningStrictDirectionCheck().getStrictDirection(currentPos, player, level);
                    if (strictDirection == null) {
                        player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, currentPos, Direction.DOWN));
                        reset();
                        return false;
                    }

                    if (phobot.getMotionUpdateService().isInPreUpdate()) {
                        float[] rotations = RotationUtil.getRotations(player, level, currentPos, strictDirection);
                        phobot.getMotionUpdateService().rotate(player, rotations[0], rotations[1]);
                    }

                    return false;
                } else {
                    direction = result.getDirection();
                }
            }

            boolean isCurrentAndCanBreak = context.isSelected(slot);
            if (withEvent) {
                BreakEvent event = new BreakEvent(currentPos, currentState, direction, player, level, gameMode);
                PingBypassApi.getEventBus().post(event);
                if (event.isCancelled()) {
                    return false;
                }
            }

            if (isCurrentAndCanBreak) {
                breakCurrentPos(player, direction, level);
            } else if (silentSwitch.getValue()) {
                context.switchTo(slot, InventoryContext.PREFER_MAINHAND | InventoryContext.SKIP_OFFHAND | InventoryContext.SWITCH_BACK);
                breakCurrentPos(player, direction, level);
            }

            if (swing.getValue()) {
                player.connection.send(Swing.uncancellable(InteractionHand.MAIN_HAND));
            }

            return true;
        }

        return false;
    }

    public @Nullable Slot getBreakingSlot(InventoryContext context, LocalPlayer player, ClientLevel level) {
        MutableObject<Float> bestDamage = new MutableObject<>(0.0f);
        MutableObject<Slot> bestSlot = new MutableObject<>();
        calculateSlots(context, player, level, bestDamage, bestSlot, new MutableObject<>(0.0f));
        if (bestSlot.getValue() != null) {
            renderDamageDelta = getDestroyProgress(currentPos, currentState, level, player, bestSlot.getValue().getItem(), true);
        }

        return bestDamage.getValue() >= 1.0f ? bestSlot.getValue() : null;
    }

    private double getTicks() {
        return timer.getPassedTime() / 50.0 - (addTick.getValue() ? 1 : 0);
    }

    private void calculateSlots(InventoryContext context, LocalPlayer player, ClientLevel level,
                                MutableObject<Float> bestDamage, MutableObject<Slot> bestSlot, MutableObject<Float> bestDamageDelta) {
        context.find(slot -> {
            ItemStack stack = slot.getItem();
            double ticks = getTicks();
            float damage = 0.0f;
            for (PlayerPosition position : phobot.getLocalPlayerPositionService().getTickPositions()) {
                damage += getDestroyProgress(currentPos, currentState, level, player, stack, position.isOnGround());
                ticks--;
                if (ticks <= 0.0) {
                    break;
                }
            }

            while (ticks > 0.0) {
                damage += getDestroyProgress(currentPos, currentState, level, player, stack, player.onGround());
                ticks--;
            }

            float damageDelta = getDestroyProgress(currentPos, currentState, level, player, stack, true);
            if (damageDelta > bestDamageDelta.getValue()) {
                bestDamageDelta.setValue(damageDelta);
            }

            boolean isCurrentAndCanBreak = context.isSelected(slot) && damage >= 1.0f;
            if (damage > bestDamage.getValue() || isCurrentAndCanBreak) {
                bestDamage.setValue(damage);
                bestSlot.setValue(slot);
                if (isCurrentAndCanBreak) {
                    return slot;
                }
            }

            return null;
        });
    }

    public void startDestroy(BlockPos pos, ClientLevel level, LocalPlayer player) {
        Direction direction = Direction.UP;
        if (phobot.getAntiCheat().getMiningStrictDirection().getValue() != StrictDirection.Type.Vanilla) {
            direction = phobot.getAntiCheat().getMiningStrictDirectionCheck().getStrictDirection(pos, player, level);
            if (direction == null) {
                return;
            }
        }

        startDestroy(pos, direction, level, player);
    }

    private void startDestroy(BlockPos pos, Direction direction, ClientLevel level, LocalPlayer player) {
        if (player.isCreative() || phobot.getAntiCheat().isAboveBuildHeight(pos.getY())) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!canMine(pos, state, player, level)) {
            return;
        }

        renderTicks = 0;
        expectingAir = false;
        renderDamageDelta = 0.0f;
        if (currentPos != null) {
            player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, currentPos, Direction.DOWN));
            if (Objects.equals(currentPos, pos)) {
                reset();
                return;
            }
        }

        currentPos = pos.immutable();
        sendStartDestroyPacket(pos, direction, level, player);
    }

    private void sendStartDestroyPacket(BlockPos pos, Direction direction, ClientLevel level, LocalPlayer player) {
        PredictionUtil.predict(level, i -> player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, i)));
        player.connection.send(Swing.uncancellable(InteractionHand.MAIN_HAND));
        player.swing(InteractionHand.MAIN_HAND, false);
        sendAbortNextTick = true;
        timer.reset();
        renderTicks = 0;
    }

    private void breakCurrentPos(LocalPlayer player, Direction currentDirection, ClientLevel level) {
        if (currentPos != null && !player.isCreative()) {
            PredictionUtil.predict(level, seq -> {
                player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, currentPos, currentDirection, seq));
                if (!noGlitchBlocks.getValue() && mc.gameMode != null) {
                    mc.submit(() -> mc.gameMode.destroyBlock(currentPos));
                }
            });

            expectingAirTimer.reset();
            expectingAir = true;
            player.connection.send(Swing.uncancellable(InteractionHand.MAIN_HAND));
            player.swing(InteractionHand.MAIN_HAND, false);
            if (fast.getValue()) {
                player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, currentPos.relative(currentDirection), currentDirection.getOpposite()));
                player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, currentPos, currentDirection));
                player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, currentPos, currentDirection));
                timer.reset();
                renderDamageDelta = 0.0f;
                renderTicks = 0;
                sendAbortNextTick = false;
            } else {
                reset();
            }
        }
    }

    public boolean canMine(BlockPos pos, BlockState state, LocalPlayer player, ClientLevel level) {
        if (!player.isCreative() && state.getDestroySpeed(level, pos) < 0) {
            return false;
        }

        return !state.getShape(level, pos).isEmpty();
    }

    public float getDestroyProgress(BlockPos pos, BlockState state, ClientLevel level, LocalPlayer player, ItemStack stack, boolean onGround) {
        float destroySpeed = state.getDestroySpeed(level, pos);
        if (destroySpeed == -1.0f) {
            return 0.0f;
        }

        int correctTool = !state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state) ? 30 : 100;
        return getPlayerDestroySpeed(player, stack, state, onGround) / destroySpeed / (float) correctTool;
    }

    private float getPlayerDestroySpeed(LocalPlayer player, ItemStack stack, BlockState state, boolean onGround) {
        float digSpeed = stack.getDestroySpeed(state);
        if (digSpeed > 1.0F) {
            // TODO: actually the call is EnchantmentHelper.getBlockEfficiency(player);
            int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, stack);
            if (i > 0 && !stack.isEmpty()) {
                digSpeed += (float)(i * i + 1);
            }
        }

        MobEffectInstance haste = player.getEffect(MobEffects.DIG_SPEED);
        if (haste != null) {
            digSpeed *= 1.0F + (haste.getAmplifier() + 1) * 0.2F;
        }

        MobEffectInstance digSlowdown = player.getEffect(MobEffects.DIG_SLOWDOWN);
        if (digSlowdown != null) {
            // TODO: use Attribute modifier instead!
            float miningFatigue = switch (digSlowdown.getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            digSpeed *= miningFatigue;
        }

        // TODO: safe if in fluid also in position history!
        if (player.isEyeInFluid(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(player)) {
            digSpeed /= 5.0F;
        }

        if (!onGround) {
            digSpeed /= 5.0F;
        }

        return Math.max(digSpeed, 0.0f);
    }

    private @Nullable Direction getDirection() {
        if (phobot.getAntiCheat().getMiningStrictDirection().getValue() == StrictDirection.Type.Vanilla) {
            return Direction.UP; // I think we like up the most because it automatically makes us rotate towards were the crystal will be
        }

        BlockPos currentPos = this.currentPos;
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player != null && level != null && currentPos != null) {
            if (level.getBlockState(currentPos).isAir()) {
                level = new BlockStateLevel.Delegating(level);
                ((BlockStateLevel) level).getMap().put(currentPos, Blocks.OBSIDIAN.defaultBlockState());
            }

            return phobot.getAntiCheat().getMiningStrictDirectionCheck().getStrictDirection(currentPos, player, level);
        }

        return null;
    }

    private void reset() {
        currentPos = null;
        currentState = Blocks.AIR.defaultBlockState();
        renderBBs = Collections.emptyList();
        renderDamageDelta = 0.0f;
        renderTicks = 0;
        sendAbortNextTick = true;
        timer.reset();
        expectingAir = false;
    }

    @Getter
    @RequiredArgsConstructor
    public static class BreakEvent extends CancellableEvent {
        private final BlockPos pos;
        private final BlockState currentState;
        private final Direction currentDirection;
        private final LocalPlayer player;
        private final ClientLevel level;
        private final MultiPlayerGameMode gameMode;
    }

}
