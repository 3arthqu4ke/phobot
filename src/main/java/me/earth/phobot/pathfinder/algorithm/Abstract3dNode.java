package me.earth.phobot.pathfinder.algorithm;

import lombok.Data;
import me.earth.phobot.util.math.MathUtil;
import net.minecraft.core.Position;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.NotNull;

// TODO: implement equals with EPSILON leniency?!?!?!?!?!?!?!?!?!?!?!?!
@Data
public abstract class Abstract3dNode<N extends Abstract3dNode<N>> implements PathfindingNode<N>, Position {
    private final double x;
    private final double y;
    private final double z;

    @Override
    public double x() {
        return x;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public double z() {
        return z;
    }

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
        return positionCompare(other);
    }

    @Override
    public String toString() {
        return "3d(" + x + ", " + y + ", " + z + ')';
    }

    public boolean positionEquals(@NotNull Position position) {
        return positionCompare(position) == 0;
    }

    public int positionCompare(@NotNull Position position) {
        int result = Double.compare(this.y(), position.y());
        if (result == 0) {
            result = Double.compare(this.z(), position.z());
            if (result == 0) {
                result = Double.compare(this.x(), position.x());
            }
        }

        return result;
    }

    public double distanceSq(double xIn, double yIn, double zIn) {
        return MathUtil.distanceSq(this.getX(), this.getY(), this.getZ(), xIn, yIn, zIn);
    }

}
