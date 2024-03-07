package me.earth.phobot.modules.combat.autocrystal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.damagecalc.CrystalPosition;
import me.earth.phobot.ducks.IEntity;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.event.RenderEvent;
import me.earth.phobot.modules.BlockPlacingModule;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.services.SurroundService;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.PositionPool;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.math.RaytraceUtil;
import me.earth.phobot.util.math.RotationUtil;
import me.earth.phobot.util.render.Renderer;
import me.earth.phobot.util.time.StopWatch;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.gui.hud.DisplaysHudInfo;
import me.earth.pingbypass.api.input.Bind;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.api.traits.Nameable;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.loop.GameloopEvent;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.event.network.PrePostSubscriber;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO: rotations are still bad!
// TODO: AntiOutPlace, attempt to schedule crystal placement in a way that we can get crystals in
//  crystal spawns -> schedule placement to arrive after player around us broke one
@Slf4j
@Getter
@Accessors(fluent = true)
public class CrystalPlacingModule extends BlockPlacingModule implements DisplaysHudInfo {
    public static final double CRYSTAL_RADIUS = 12.0;
    public static final double CRYSTAL_RADIUS_SQ = CRYSTAL_RADIUS * CRYSTAL_RADIUS;

    private final Setting<Double> minDamage = precise("MinDamage", 6.0, 0.1, 36.0, "Minimum damage to deal to a player.");
    private final Setting<Integer> placeDelay = number("PlaceDelay", 0, 0, 500, "Crystals will be placed in intervals of at least this value in milliseconds.");
    private final Setting<Integer> breakDelay = number("BreakDelay", 0, 0, 500, "Crystals will be broken in intervals of at least this value in milliseconds.");
    private final Setting<Boolean> multiThreading = bool("MultiThreading", true, "Runs the calculation on a separate thread to improve performance.");
    private final Setting<Integer> replaceTime = number("ReplaceTime", 25, -1, 500, "Time in ms after which we run a calculation after breaking a crystal. -1 means off.");
    private final Setting<Integer> reCalcTime = number("ReCalcTime", 25, 0, 500, "Time in ms after which we run a second calculation after the replace calculation. 0 means off.");
    private final Setting<Integer> maxDeathTime = number("MaxDeathTime", 45, 0, 500, "Maximum time for a crystal to be dead until we consider our attack failed.");
    private final Setting<Float> balance = floating("Balance", 2.0f, 0.1f, 10.0f, "How much you want to factor in damage over self damage.");
    private final Setting<Float> minDamageFactor = floating("MinFactor", 2.0f, 0.1f, 10.0f, "Minimum Factor between self damage and damage we are dealing.");
    private final Setting<Boolean> terrain = bool("Terrain", false, "Will take into account terrain we blow up.");
    private final Setting<Integer> obsidian = number("Obsidian", 1, 0, 10, "Maximum amount of obsidian blocks to place.");
    private final Setting<Boolean> replaceWhenNotFound = bool("ReplaceWhenFailedPlace", false, "Will rerun a calculation when our crystal has not spawned yet.");
    private final Setting<Double> minObbyDamage = precise("MinObsidianDamage", 10.0, 0.1, 36.0, "Minimum damage to deal to a player when placing an obsidian block for the crystal.");
    private final Setting<Double> faceplace = precise("Faceplace", 10.0, 0.1, 36.0, "Health at which we start to faceplace.");
    private final Setting<Double> armor = precise("Armor", 7.5, 0.1, 100.0, "Armor percentage at which we start to faceplace.");
    private final Setting<Bind> facePlaceBind = bind("FacePlaceBind", "Hold this bind to faceplace.");
    private final Setting<Boolean> faceplaceWhenUnsafe = bool("FacePlaceWhenUnsafe", false, "Will faceplace if you are not safe.");
    private final Setting<Boolean> fastWhenUnsafe = bool("FastWhenUnsafe", false, "Will speed up faceplacing while you are outside holes.");
    private final Setting<Integer> placePrediction = number("PlacePrediction", 0, 0, 6, "Ticks to predict enemy player movement for when calculating damage for placements.");
    private final Setting<Integer> breakPrediction = number("BreakPrediction", 0, 0, 6, "Ticks to predict enemy player movement for when calculating damage for breaking crystals.");

    private final CalculationService calculationService = new CalculationService(this);
    private final PositionPool<CrystalPosition> positionPool = new PositionPool<>(7, CrystalPosition::new, new CrystalPosition[0]);

    private final Map<BlockPos, Long> blockBlackList = new ConcurrentHashMap<>();
    private final Map<Integer, Long> entityBlacklist = new ConcurrentHashMap<>();

    private final Map<BlockPos, Long> map = new ConcurrentHashMap<>();
    private final StopWatch.ForMultipleThreads breakTimer = new StopWatch.ForMultipleThreads();
    private final StopWatch.ForMultipleThreads placeTimer = new StopWatch.ForMultipleThreads();
    private final StopWatch.ForMultipleThreads obbyTimer = new StopWatch.ForMultipleThreads();
    private final StopWatch.ForSingleThread renderTimer = new StopWatch.ForSingleThread();
    private final StopWatch.ForSingleThread instantPlaceTimer = new StopWatch.ForSingleThread();
    private final CrystalPlacer placer = new CrystalPlacer(this, phobot);
    @Getter(AccessLevel.NONE)
    private final BlockPlacer blockPlacer = new BlockPlacer(
            phobot.getLocalPlayerPositionService(),
            phobot.getMotionUpdateService(),
            phobot.getInventoryService(),
            phobot.getMinecraft(),
            phobot.getAntiCheat());
    private final SurroundService surroundService;

    @Setter
    private volatile BlockPos lastCrystalPos = new BlockPos(0, -1000, 0);
    @Setter
    private volatile CrystalPosition obbyPos = null;
    @Setter
    private volatile CrystalPlacingAction rotationAction = null;

    @Setter(AccessLevel.PROTECTED)
    @Getter(AccessLevel.NONE)
    private Entity target;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private BlockPos renderPos;
    private Entity lastCrystal;

    public CrystalPlacingModule(Phobot phobot, SurroundService surroundService, String name, Nameable category, String description) {
        super(phobot, phobot.getBlockPlacer(), name, category, description, 0);
        this.surroundService = surroundService;
        unregister(getDelay());
        unregister(getNoGlitchBlocks());
        register(getNoGlitchBlocks());
        listen(new SafeListener<GameloopEvent>(mc) {
            @Override
            public void onEvent(GameloopEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                calculationService.calculate(level, player, placeTimer.passed(100L) && breakTimer.passed(100L) ? 50 : 100, multiThreading.getValue());
            }
        });

        getListeners().addAll(new PrePostSubscriber<>(ClientboundAddEntityPacket.class) {
            @Override
            public void onPreEvent(PacketEvent.Receive<ClientboundAddEntityPacket> event) {
                ClientLevel level = mc.level;
                LocalPlayer localPlayer = mc.player;
                Player player = phobot.getLocalPlayerPositionService().getPlayerOnLastPosition(mc.player);
                if (localPlayer == null || level == null || player == null || !event.getPacket().getType().equals(EntityType.END_CRYSTAL)) {
                    return;
                }

                double x = event.getPacket().getX();
                double y = event.getPacket().getY();
                double z = event.getPacket().getZ();
                EndCrystal endCrystal = new EndCrystal(level, x, y, z);
                endCrystal.setId(event.getPacket().getId());
                if (endCrystal.getBoundingBox().distanceToSqr(player.getEyePosition()) >= ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE
                    || phobot.getAntiCheat().getAttackRotations().getValue() && !RaytraceUtil.areRotationsLegit(phobot, endCrystal)) {
                    return;
                }

                breakSpawningCrystal(this, localPlayer, level, endCrystal, false);
            }
        }.getListeners());

        listen(new Listener<RenderEvent>() {
            @Override
            public void onEvent(RenderEvent event) {
                BlockPos renderPos = getRenderPos();
                if (renderPos != null) {
                    event.getAabb().set(renderPos);
                    event.setBoxColor(1.0f, 1.0f, 1.0f, 1.0f, 0.4f);
                    Renderer.renderBoxWithOutlineAndSides(event, 1.5f, true);
                }
            }
        });

        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc, 1000) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode multiPlayerGameMode) {
                if (phobot.getAntiCheat().getAttackRotations().getValue() && lastCrystalPos.getY() != -1000 && !placeTimer.passed(150)) {
                    float[] rotations = RotationUtil.getRotations(player, lastCrystalPos.getX() + 0.5, lastCrystalPos.getY(), lastCrystalPos.getZ() + 0.5);
                    phobot.getMotionUpdateService().rotate(player, rotations[0], rotations[1]);
                }

                calculationService.calculate(level, player, 0, multiThreading.getValue());
            }
        });

        ResetUtil.onRespawnOrWorldChange(this, mc, this::reset);
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    @Override
    public String getHudInfo() {
        Entity target = this.target;
        if (target instanceof Player player) {
            return player.getScoreboardName();
        }

        return null;
    }

    @Override
    public BlockPlacer getBlockPlacer() {
        return mc.isSameThread() ? super.getBlockPlacer() : blockPlacer;
    }

    @Override
    public void updatePlacements(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        CrystalPlacingAction rotation = rotationAction();
        if (rotation != null) {
            obbyPos(null);
            rotationAction(null);
            Calculation calculation = createCalculation(level, player);
            calculation.calculatePlacements(false, rotation.getCrystalPosition());
            obbyPos(null); // clear again to prevent loop
            rotationAction(null);
            return;
        }

        CrystalPosition obbyPos = obbyPos();
        if (obbyPos != null) {
            obbyPos(null);
            rotationAction(null);
            Calculation calculation = createCalculation(level, player);
            calculation.preparePlaceCalculation();
            calculation.calculateObsidian(obbyPos);
            obbyPos(null); // clear again to prevent loop
            rotationAction(null);
        }
    }

    public Calculation createCalculation(ClientLevel level, LocalPlayer player) {
        return new Calculation(this, mc, phobot, player, level, phobot.getAntiCheat().getDamageCalculator());
    }

    public void setRenderPos(BlockPos pos) {
        renderTimer.reset();
        this.renderPos = pos;
    }

    public Entity getTarget() {
        if (renderTimer.passed(250)) {
            renderPos = null;
            target = null;
        }

        return target;
    }

    public BlockPos getRenderPos() {
        if (renderTimer.passed(250)) {
            renderPos = null;
            target = null;
        }

        return renderPos;
    }

    public void reset() {
        rotationAction = null;
        lastCrystal = null;
        target = null;
        renderPos = null;
        lastCrystalPos = new BlockPos(0, -1000, 0);
        obbyPos = null;
        // for garbage collection, we have to remove all the EndCrystals
        for (CrystalPosition crystalPosition : this.positionPool.getPositions()) {
            crystalPosition.reset();
        }
    }

    private void breakSpawningCrystal(PrePostSubscriber<?> subscriber, LocalPlayer player, ClientLevel level, EndCrystal endCrystal, boolean scheduled) {
        try {
            var breakCalculation = new BreakCalculation(this, mc, phobot, player, level, phobot.getAntiCheat().getDamageCalculator());
            if (breakCalculation.calculateSingleCrystal(endCrystal, new MutableObject<>(0.0f), new MutableObject<>(false)) && breakTimer.passed(breakDelay.getValue())) {
                phobot.getAttackService().attack(player, endCrystal);
                breakTimer.reset();
                long attackTime = TimeUtil.getMillis();
                subscriber.addScheduledPostEvent(mc, () -> {
                    Entity crystal = level.getEntity(endCrystal.getId());
                    lastCrystal = crystal;
                    if (crystal instanceof IEntity iEndCrystal) {
                        iEndCrystal.phobot$setAttackTime(attackTime);
                    }

                    int replaceTime = this.replaceTime.getValue();
                    if (replaceTime == 0) {
                        calculationService.calculate(level, player, 0, multiThreading.getValue(), calculation -> calculation.setRunBreakingCalculation(false));
                    } else if (replaceTime > 0) {
                        phobot.getTaskService().addTaskToBeExecutedIn(replaceTime, CrystalPlacingModule.this, () -> {
                            calculationService.calculate(level, player, 0, multiThreading.getValue(), calculation -> calculation.setRunBreakingCalculation(false));
                            if (reCalcTime.getValue() != 0) {
                                phobot.getTaskService().addTaskToBeExecutedIn(reCalcTime.getValue(), CrystalPlacingModule.this, () -> {
                                    Entity stillLivingCrystal = level.getEntity(endCrystal.getId());
                                    if (stillLivingCrystal != null && !stillLivingCrystal.isRemoved()
                                            || (replaceWhenNotFound.getValue() && level.getEntities(EntityType.END_CRYSTAL, new AABB(lastCrystalPos), ec -> lastCrystalPos.equals(ec.blockPosition())).isEmpty())) {
                                        calculationService.calculate(level, player, 0, multiThreading.getValue());
                                    }
                                });
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            log.info("Failed to breakSpawningCrystal asynchronously, scheduling for main thread.");
            if (!scheduled && !mc.isSameThread()) {
                mc.submit(() -> breakSpawningCrystal(subscriber, player, level, endCrystal, true));
            }
        }
    }

}
