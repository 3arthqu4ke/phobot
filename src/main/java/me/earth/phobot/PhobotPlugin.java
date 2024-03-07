package me.earth.phobot;

import me.earth.phobot.bot.Bot;
import me.earth.phobot.commands.GotoCommand;
import me.earth.phobot.commands.KitCommand;
import me.earth.phobot.holes.HoleManager;
import me.earth.phobot.modules.client.*;
import me.earth.phobot.modules.client.anticheat.AntiCheat;
import me.earth.phobot.modules.combat.*;
import me.earth.phobot.modules.combat.autocrystal.AutoCrystal;
import me.earth.phobot.modules.misc.*;
import me.earth.phobot.modules.movement.*;
import me.earth.phobot.modules.render.Fullbright;
import me.earth.phobot.modules.render.Holes;
import me.earth.phobot.modules.render.HolesRenderListener;
import me.earth.phobot.modules.render.NoRender;
import me.earth.phobot.pathfinder.Pathfinder;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManager;
import me.earth.phobot.services.*;
import me.earth.phobot.services.inventory.InventoryService;
import me.earth.phobot.util.collections.XZMap;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.api.Subscriber;
import me.earth.pingbypass.api.plugin.impl.AbstractUnloadablePlugin;
import me.earth.pingbypass.api.plugin.impl.PluginUnloadingService;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class PhobotPlugin extends AbstractUnloadablePlugin {
    private Phobot phobot;

    @Override
    public void load(PingBypass pingBypass, PluginUnloadingService unloadingService) {
        ExecutorService executor = getExecutorService();
        Phobot phobot = initalizePhobot(pingBypass, executor, unloadingService);
        registerCommands(pingBypass, phobot, unloadingService);
        registerModules(pingBypass, phobot, unloadingService);
        unloadingService.runOnUnload(executor::shutdown);
        PhobotApi.setPhobot(phobot);
        this.phobot = phobot;
    }

    @Override
    public void unload() {
        super.unload();
        if (Objects.equals(PhobotApi.getPhobot(), phobot)) {
            if (phobot != null) {
                phobot.getInventoryService().releasePlayerUpdateLock();
            }

            PhobotApi.setPhobot(null);
            this.phobot = null;
        }
    }

    private Phobot initalizePhobot(PingBypass pingBypass, ExecutorService executor, PluginUnloadingService unloadingService) {
        WorldVersionService worldVersionService = subscribe(unloadingService, new WorldVersionService());
        MovementService movementService = new MovementService();

        Holes holes = new Holes(pingBypass, executor);
        unloadingService.registerModule(holes);
        HoleManager holeManager = subscribe(unloadingService, new HoleManager(holes, new ConcurrentHashMap<>()));
        new HolesRenderListener(holeManager, holes).getListeners().forEach(holes::listen);

        ProtectionCacheService protectionCacheService = subscribe(unloadingService, new ProtectionCacheService(pingBypass.getMinecraft()));
        AttackService attackService = subscribe(unloadingService, new AttackService(pingBypass.getMinecraft()));
        TotemPopService totemPopService = subscribe(unloadingService, new TotemPopService(pingBypass.getMinecraft()));

        DamageService damageService = subscribe(unloadingService, new DamageService(protectionCacheService, pingBypass.getMinecraft()));

        Pathfinding pathfinding = new Pathfinding(pingBypass, executor);
        unloadingService.registerModule(pathfinding);
        NavigationMeshManager navigationMeshManager = subscribe(unloadingService,
                new NavigationMeshManager(pathfinding, movementService.getMovement(), new ConcurrentHashMap<>(), new XZMap<>(new ConcurrentHashMap<>())));
        pathfinding.listen(new GraphDebugRenderer(navigationMeshManager, pathfinding, pingBypass.getMinecraft()));

        LagbackService lagbackService = subscribe(unloadingService, new LagbackService());

        TaskService taskService = subscribe(unloadingService, new TaskService(pingBypass.getMinecraft()));
        Pathfinder pathfinder = subscribe(unloadingService, new Pathfinder(pingBypass, navigationMeshManager, executor, taskService));

        ServerService serverService = subscribe(unloadingService, new ServerService());
        AntiCheat antiCheat = new AntiCheat(pingBypass, serverService);
        unloadingService.registerModule(antiCheat);

        subscribe(unloadingService, new TickPredictionService(pingBypass.getMinecraft()));
        LocalPlayerPositionService localPlayerPositionService = subscribe(unloadingService, new LocalPlayerPositionService(pingBypass.getMinecraft()));
        InvincibilityFrameService invincibilityFrameService = subscribe(unloadingService, new InvincibilityFrameService(pingBypass.getMinecraft(), antiCheat, localPlayerPositionService));

        InventoryService inventoryService = subscribe(unloadingService, new InventoryService(antiCheat));
        MotionUpdateService motionUpdateService = subscribe(unloadingService, new MotionUpdateService(pingBypass.getMinecraft()));
        BlockPlacer blockPlacer = subscribe(unloadingService, new BlockPlacer(localPlayerPositionService, motionUpdateService, inventoryService, pingBypass.getMinecraft(), antiCheat));
        BlockUpdateService blockUpdateService = subscribe(unloadingService, new BlockUpdateService());
        subscribe(unloadingService, new PlayerPredictionService(antiCheat, pingBypass.getMinecraft(), movementService));
        BlockDestructionService blockDestructionService = subscribe(unloadingService, new BlockDestructionService(pingBypass.getMinecraft()));
        ThreadSafeLevelService threadSafeLevelService = subscribe(unloadingService, new ThreadSafeLevelService(pingBypass.getMinecraft()));
        return new Phobot(pingBypass, executor, navigationMeshManager, pathfinder, pingBypass.getMinecraft(),
                holeManager, lagbackService, localPlayerPositionService, attackService,
                invincibilityFrameService, damageService, blockPlacer, antiCheat, motionUpdateService,
                blockUpdateService, blockDestructionService, serverService, totemPopService, inventoryService, threadSafeLevelService,
                taskService, movementService, worldVersionService);
    }

    private void registerCommands(PingBypass pingBypass, Phobot phobot, PluginUnloadingService unloadingService) {
        unloadingService.registerCommand(new GotoCommand(phobot));
        unloadingService.registerCommand(new KitCommand());
    }

    private void registerModules(PingBypass pingBypass, Phobot phobot, PluginUnloadingService unloadingService) {
        unloadingService.registerAddOn(new NotificationAddOn(phobot));

        unloadingService.registerModule(new PacketSpam(pingBypass));
        unloadingService.registerModule(new Packets(pingBypass));
        unloadingService.registerModule(new Reach(pingBypass));
        unloadingService.registerModule(new Debug(phobot));
        unloadingService.registerModule(new NoInterp(pingBypass));
        unloadingService.registerModule(new AccountSpoof(pingBypass));
        unloadingService.registerModule(new LoggerModule(pingBypass));
        unloadingService.registerModule(new Sprint(pingBypass));
        unloadingService.registerModule(new NoSlowDown(phobot));
        unloadingService.registerModule(new Velocity(phobot));
        unloadingService.registerModule(new Step(phobot));
        unloadingService.registerModule(new AutoKit(phobot));
        unloadingService.registerModule(new Timer(phobot));
        unloadingService.registerModule(new Replenish(phobot));
        unloadingService.registerModule(new Scaffold(phobot));
        unloadingService.registerModule(new Stop(pingBypass));
        unloadingService.registerModule(new XCarry(phobot));
        unloadingService.registerModule(new FakePlayerModule(pingBypass));
        unloadingService.registerModule(new Criticals(pingBypass));
        unloadingService.registerModule(new Swing(pingBypass));
        unloadingService.registerModule(new AutoConnect(phobot));
        unloadingService.registerModule(new Repair(phobot));
        unloadingService.registerModule(new Potions(phobot));
        unloadingService.registerModule(new Fullbright(phobot));
        unloadingService.registerModule(new NoRender(phobot));
        unloadingService.registerModule(new AutoRespawn(phobot));

        Speed speed = new Speed(phobot);
        unloadingService.registerModule(speed);
        unloadingService.registerModule(new FastFall(phobot, speed));
        unloadingService.registerModule(new Strafe(phobot, speed));

        Surround surround = new Surround(phobot);
        SurroundService surroundService = subscribe(unloadingService, new SurroundService(surround));
        unloadingService.registerModule(surround);
        Speedmine speedmine = new Speedmine(phobot);
        unloadingService.registerModule(speedmine);
        AutoCrystal autoCrystal = new AutoCrystal(phobot, surroundService);
        unloadingService.registerModule(autoCrystal);
        Suicide suicide = new Suicide(phobot, surroundService);
        unloadingService.registerModule(suicide);
        AutoTrap autoTrap = new AutoTrap(phobot, surroundService);
        unloadingService.registerModule(autoTrap);
        Bomber bomber = new Bomber(phobot, speedmine, autoCrystal, autoTrap, surroundService);
        unloadingService.registerModule(bomber);
        KillAura killAura = new KillAura(phobot);
        unloadingService.registerModule(killAura);
        AutoMine autoMine = new AutoMine(phobot, speedmine, bomber, surround);
        unloadingService.registerModule(autoMine);

        unloadingService.registerModule(new SelfTrap(phobot, surroundService));
        unloadingService.registerModule(new AutoEat(phobot, killAura));
        unloadingService.registerModule(new HoleFiller(phobot, surroundService));
        unloadingService.registerModule(new Blocker(phobot, surround));
        unloadingService.registerModule(new AutoTotem(phobot, suicide, surroundService));
        unloadingService.registerModule(new AutoEchest(phobot, surroundService, autoMine));

        unloadingService.registerModule(new Bot(phobot, surroundService));
    }

    private ExecutorService getExecutorService() {
        AtomicInteger id = new AtomicInteger();
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("Phobot-Thread-" + id.getAndIncrement());
            return thread;
        });
    }

    private <T extends Subscriber> T subscribe(PluginUnloadingService unloadingService, T t) {
        unloadingService.subscribe(t);
        return t;
    }

}
