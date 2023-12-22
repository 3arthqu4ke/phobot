package me.earth.phobot.damagecalc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.earth.phobot.ducks.IEntity;
import me.earth.phobot.modules.client.anticheat.AntiCheat;
import me.earth.phobot.util.mutables.MutPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class CrystalPosition extends MutPos {
    public static final long MAX_DEATHTIME = 100L;
    private final Entity[] crystals = new Entity[4];
    private final Vec3i offset;

    private List<BlockPos> path;
    private float selfDamage = Float.MAX_VALUE;
    private float damage = 0.0f;
    private boolean obsidian;
    private boolean blockedByCrystal;
    private boolean clearOfEntities;
    private boolean valid;
    private boolean killing;
    private boolean faceplacingForAReason;
    private Integer targetId;
    private int obbyTries;

    public void computeValidity(AntiCheat antiCheat, ClientLevel level, LocalPlayer player, double range, long maxDeathTime) {
        computeValidity(antiCheat, level, player, range, player.getX(), player.getEyeY(), player.getZ(), maxDeathTime);
    }

    public void computeValidity(AntiCheat antiCheat, ClientLevel level, LocalPlayer player, double range, Vec3i pos, long maxDeathTime) {
        computeValidity(antiCheat, level, player, range, pos.getX(), pos.getY(), pos.getZ(), maxDeathTime);
    }

    public void computeValidity(AntiCheat antiCheat, ClientLevel level, LocalPlayer player, double range, double x, double y, double z, long maxDeathTime) {
        reset();
        setWithCrystalPositionOffset(x, y, z);
        if (player.getEyePosition().distanceToSqr(getX() + 0.5, getY() + 0.5, getZ() + 0.5) > range * range || !level.getWorldBorder().isWithinBounds(this)) {
            return;
        }

        BlockState state = level.getBlockState(this);
        if (state.canBeReplaced()) {
            obsidian = true;
        } else if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) {
            return;
        }

        if (!obsidian && antiCheat.getCrystalStrictDirectionCheck().getStrictDirection(this, player, level) == null) {
            return;
        }

        this.incrementY(1);
        if (!level.isEmptyBlock(this)) {
            this.incrementY(-1);
            return;
        }

        computeEntityValidityInternal(level, this, antiCheat.isCC(), maxDeathTime);
        this.incrementY(-1);
        valid = clearOfEntities;
    }

    public void computeEntityValidity(ClientLevel level, boolean cc, long maxDeathTime) {
        this.incrementY(1);
        computeEntityValidityInternal(level, this, cc, maxDeathTime);
        this.incrementY(-1);
    }

    public void setWithCrystalPositionOffset(BlockPos pos) {
        setWithCrystalPositionOffset(pos.getX(), pos.getY(), pos.getZ());
    }

    public void setWithCrystalPositionOffset(double x, double y, double z) {
        set(Math.floor(x) + offset.getX(), Math.floor(y) + offset.getY(), Math.floor(z) + offset.getZ());
    }

    public void setDamageIfHigher(float damage) {
        if (damage > this.damage) {
            setDamage(damage);
        }
    }

    public float getRatio(float balance) {
        if (balance <= 0.0) {
            balance = 0.1f;
        }

        if (Float.compare(selfDamage, 0.0f) <= 0) {
            return (damage * balance) / (0.1f * (1 / balance));
        }

        return (damage * balance) / (selfDamage * (1 / balance));
    }

    public void reset() {
        path = null;
        damage = 0.0f;
        killing = false;
        faceplacingForAReason = false;
        blockedByCrystal = false;
        selfDamage = Float.MAX_VALUE;
        clearOfEntities = false;
        obsidian = false;
        valid = false;
        targetId = null;
        Arrays.fill(crystals, null);
    }

    public CrystalPosition copy() {
        CrystalPosition copy = new CrystalPosition(offset);
        copy.copyFrom(this);
        return copy;
    }

    public void copyFrom(CrystalPosition crystalPosition) {
        set(crystalPosition);
        System.arraycopy(crystalPosition.getCrystals(), 0, crystals, 0, crystals.length);
        this.path = crystalPosition.getPath();
        this.selfDamage = crystalPosition.getSelfDamage();
        this.damage = crystalPosition.getDamage();
        this.obsidian = crystalPosition.isObsidian();
        this.blockedByCrystal = crystalPosition.isBlockedByCrystal();
        this.clearOfEntities = crystalPosition.isClearOfEntities();
        this.valid = crystalPosition.isValid();
        this.killing = crystalPosition.isKilling();
        this.faceplacingForAReason = crystalPosition.isFaceplacingForAReason();
    }

    public boolean isBetterThan(@Nullable CrystalPosition other, float balance) {
        if (other == null || !this.isObsidian() && other.isObsidian() || !this.isBlockedByCrystal() && other.isBlockedByCrystal() && this.isObsidian() == other.isObsidian()) {
            return true;
        }

        if (this.isObsidian() && !other.isObsidian() || this.isBlockedByCrystal() && !other.isBlockedByCrystal()) {
            return false;
        }

        return getRatio(balance) > other.getRatio(balance);
    }

    private void computeEntityValidityInternal(ClientLevel level, MutPos computePos, boolean cc, long maxDeathTime) {
        int bbX = computePos.getX();
        int bbY = computePos.getY();
        int bbZ = computePos.getZ();
        // TODO: mutable AABB
        int i = 0;
        for (Entity entity : level.getEntities(null, new AABB(bbX, bbY, bbZ, bbX + 1.0, bbY + (cc ? 1.0 : 2.0), bbZ + 1.0))) {
            if (entity instanceof EndCrystal) {
                if (((IEntity) entity).phobot$GetTimeSinceAttack() >= maxDeathTime && i < crystals.length) {
                    crystals[i] = entity;
                    blockedByCrystal = true;
                    i++;
                }

                continue;
            } else if (entity instanceof ItemEntity && (((IEntity) entity).phobot$GetTimeSinceAttack() < maxDeathTime)) {
                continue;
            }

            clearOfEntities = false;
            return;
        }

        clearOfEntities = true;
    }

    public static @Nullable CrystalPosition copy(@Nullable CrystalPosition crystalPosition) {
        return crystalPosition == null ? null : crystalPosition.copy();
    }

}
