package me.earth.phobot.modules.client.anticheat;

import lombok.Getter;
import me.earth.phobot.damagecalc.DamageCalculator;
import me.earth.phobot.damagecalc.Raytracer;
import me.earth.phobot.modules.PhobotNameSpacedModule;
import me.earth.phobot.services.ServerService;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.HoldsValue;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

@Getter
public class AntiCheat extends PhobotNameSpacedModule {
    private static final StrictDirection NCP_AND_GRIM = new Combined(Grim.INSTANCE, NCP.INSTANCE);

    private final Setting<Integer> actions = number("Actions", 8, 1, 100, "Block placement actions to perform per tick.");
    private final Setting<Double> miningRange = precise("MiningRange", 5.25, 0.1, 6.0, "Range within which you can mine blocks.");

    private final Setting<MovementAntiCheat> movement = constant("Movement", MovementAntiCheat.Grim, "Movement AntiCheat checks, important for NoSlowDown and Velocity.");

    private final Setting<StrictDirection.Type> strictDirection = constant("StrictDirection", StrictDirection.Type.Grim, "Strict direction checks.");
    private final Setting<StrictDirection.Type> miningStrictDirection = constant("MiningStrictDirection", StrictDirection.Type.NCP, "There seems to be no strict direction check for mining on Grim.");
    private final Setting<StrictDirection.Type> crystalStrictDirection = constant("CrystalStrictDirection", StrictDirection.Type.NCP, "There seems to be no strict direction check for placing crystals on Grim.");

    private final Setting<Boolean> miningRotations = bool("MiningRotations", true, "Rotates when mining.");
    private final Setting<Boolean> blockRotations = bool("BlockRotations", true, "Rotates when placing blocks.");
    private final Setting<Boolean> crystalRotations = bool("CrystalRotations", false, "Rotates when placing crystals.");
    private final Setting<Boolean> attackRotations = bool("AttackRotations", false, "Rotates when attacking.");

    private final Setting<Integer> maxBuildHeight = number("MaxBuildHeight", 100, -64, 320, "Maximum buildheight of the server.");
    private final Setting<Integer> inventoryDelay = number("InventoryDelay", 75, 0, 500, "Delay between inventory clicks. (I could spam SWAP packets as slow as 67ms on CC).");
    private final Setting<Integer> updates = number("Updates", 1, 1, 10, "Frequency with which entities get updated. CrystalPvP.cc seems to send an update packet every tick.");
    private final Setting<Double> lagbackThreshold = precise("LagBackThreshold", 3.0, 1.0, 10.0, "If a player moves this amount during a tick we consider it a lagback/teleport.");
    private final Setting<Boolean> cC = bool("CC", false, "We automatically detect if a server is crystalpvp.cc, but with this you can manually use cc settings on other servers.");
    private final Setting<Boolean> closeInv = bool("CloseInventory", false, "If we have to send a ServerboundContainerClosePacket after every inventory click.");
    private final Setting<Boolean> opposite = bool("Opposite", true, "Allows to raytrace through a block and place on the other side.");
    private final Setting<RaytraceOptimization> raytraceOptimization = constant("Optimization", RaytraceOptimization.None, "Mode cc makes some raytrace optimizations for the FFA world.");
    private final DamageCalculator defaultCalculator = new DamageCalculator(Raytracer.level());
    private final DamageCalculator ccCalculator = new DamageCalculator(Raytracer.cc());
    private final ServerService serverService;

    public AntiCheat(PingBypass pingBypass, ServerService serverService) {
        super(pingBypass, "AntiCheat", Categories.CLIENT, "Configures the AntiCheat.");
        this.serverService = serverService;
    }

    public DamageCalculator getDamageCalculator() {
        return raytraceOptimization.getValue() == RaytraceOptimization.CC ? ccCalculator : defaultCalculator;
    }

    public boolean isAboveBuildHeight(LocalPlayer player) {
        return player.getY() > maxBuildHeight.getValue();
    }

    public boolean isAboveBuildHeight(double y) {
        return y > maxBuildHeight.getValue();
    }

    public boolean isCC() {
        return serverService.isCurrentServerCC() || cC.getValue();
    }

    public double getMiningRangeSq() {
        return Mth.square(miningRange.getValue());
    }

    public StrictDirection getStrictDirectionCheck() {
        return getStrictDirectionCheck(strictDirection);
    }

    public StrictDirection getMiningStrictDirectionCheck() {
        return getStrictDirectionCheck(miningStrictDirection);
    }

    public StrictDirection getCrystalStrictDirectionCheck() {
        return getStrictDirectionCheck(crystalStrictDirection);
    }

    public StrictDirection getStrictDirectionCheck(HoldsValue<StrictDirection.Type> type) {
        return switch (type.getValue()) {
            case NCP -> NCP.INSTANCE;
            case Grim -> Grim.INSTANCE;
            case Combined -> NCP_AND_GRIM;
            default -> Vanilla.INSTANCE;
        };
    }

    public enum RaytraceOptimization {
        None,
        CC
    }

}
