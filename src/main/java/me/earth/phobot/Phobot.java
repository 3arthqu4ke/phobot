package me.earth.phobot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.earth.phobot.holes.HoleManager;
import me.earth.phobot.modules.client.anticheat.AntiCheat;
import me.earth.phobot.pathfinder.Pathfinder;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManager;
import me.earth.phobot.services.*;
import me.earth.phobot.services.inventory.InventoryService;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.api.EventBus;
import net.minecraft.client.Minecraft;

import java.util.concurrent.ExecutorService;

@Getter
@RequiredArgsConstructor
@SuppressWarnings("ClassCanBeRecord")
public class Phobot {
    public static final String NAME = "Phobot";

    private final PingBypass pingBypass;
    private final EventBus unloadingEventBus;
    private final ExecutorService executorService;
    private final NavigationMeshManager navigationMeshManager;
    private final Pathfinder pathfinder;
    private final Minecraft minecraft;
    private final HoleManager holeManager;
    private final LagbackService lagbackService;
    private final LocalPlayerPositionService localPlayerPositionService;
    private final AttackService attackService;
    private final InvincibilityFrameService invincibilityFrameService;
    private final DamageService damageService;
    private final BlockPlacer blockPlacer;
    private final AntiCheat antiCheat;
    private final MotionUpdateService motionUpdateService;
    private final BlockUpdateService blockUpdateService;
    private final BlockDestructionService blockDestructionService;
    private final ServerService serverService;
    private final TotemPopService totemPopService;
    private final InventoryService inventoryService;
    private final ThreadSafeLevelService threadSafeLevelService;
    private final TaskService taskService;
    private final MovementService movementService;
    private final WorldVersionService worldVersionService;

}
