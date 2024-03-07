package me.earth.phobot.pathfinder.algorithm;

import lombok.Data;
import me.earth.phobot.util.math.MathUtil;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Data
public abstract class Abstract3iNode<N extends Abstract3iNode<N>> implements PathfindingNode<N> {
    private final int x;
    private final int y;
    private final int z;

    @Override
    public double getRenderX() {
        return getX() + 0.5;
    }

    @Override
    public double getRenderY() {
        return getY();
    }

    @Override
    public double getRenderZ() {
        return getZ() + 0.5;
    }

    @Override
    public int getChunkX() {
        return SectionPos.blockToSectionCoord(getX());
    }

    @Override
    public int getChunkZ() {
        return SectionPos.blockToSectionCoord(getZ());
    }

    @Override
    public double distanceSq(N node) {
        return distanceSq(node.getX(), node.getY(), node.getZ());
    }

    @Override
    public int compareTo(@NotNull N other) {
        if (this.getY() == other.getY()) {
            if (this.getZ() == other.getZ()) {
                return this.getX() - other.getX();
            }

            return this.getZ() - other.getZ();
        }

        return this.getY() - other.getY();
    }

    @Override
    public String toString() {
        return "3i(" + x + ", " + y + ", " + z + ')';
    }

    public double distanceSq(Vec3 vec) {
        return distanceSq(vec.x, vec.y, vec.z);
    }

    public double distanceSq(Vec3i vec) {
        return distanceSq(vec.getX(), vec.getY(), vec.getZ());
    }

    public double distanceSq(double xIn, double yIn, double zIn) {
        return MathUtil.distanceSq(this.getX(), this.getY(), this.getZ(), xIn, yIn, zIn);
    }

    public double distanceSqToCenter(double xIn, double yIn, double zIn) {
        return MathUtil.distanceSq(this.getX() + 0.5, this.getY(), this.getZ() + 0.5, xIn, yIn, zIn);
    }

}
