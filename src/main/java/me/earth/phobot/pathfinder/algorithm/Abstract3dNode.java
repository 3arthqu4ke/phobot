package me.earth.phobot.pathfinder.algorithm;

import lombok.Data;
import me.earth.phobot.util.math.MathUtil;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.NotNull;

@Data
public abstract class Abstract3dNode<N extends Abstract3dNode<N>> implements PathfindingNode<N> {
    private final double x;
    private final double y;
    private final double z;

    @Override
    public double getRenderX() {
        return getX();
    }

    @Override
    public double getRenderY() {
        return getY();
    }

    @Override
    public double getRenderZ() {
        return getZ();
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
        int result = Double.compare(this.getY(), other.getY());
        if (result == 0) {
            result = Double.compare(this.getZ(), other.getZ());
            if (result == 0) {
                result = Double.compare(this.getX(), other.getX());
            }
        }

        return result;
    }

    public double distanceSq(double xIn, double yIn, double zIn) {
        return MathUtil.distanceSq(this.getX(), this.getY(), this.getZ(), xIn, yIn, zIn);
    }

}
