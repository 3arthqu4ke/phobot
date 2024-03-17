package me.earth.phobot.pathfinder.blocks;

import lombok.Getter;
import lombok.ToString;
import me.earth.phobot.pathfinder.algorithm.PathfindingNode;
import me.earth.phobot.util.mutables.MutPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Getter
@ToString
public class BlockNode implements PathfindingNode<BlockNode> {
    @ToString.Exclude
    private final BlockNode[] adjacent = new BlockNode[6];
    private final BlockPos offsetToCenter;
    private final MutPos current = new MutPos();

    public BlockNode(BlockPos offsetToCenter) {
        this.offsetToCenter = offsetToCenter;
    }

    @Override
    public double getRenderX() {
        return current.getX() + 0.5;
    }

    @Override
    public double getRenderY() {
        return current.getY();
    }

    @Override
    public double getRenderZ() {
        return current.getZ() + 0.5;
    }

    @Override
    public int getChunkX() {
        return SectionPos.blockToSectionCoord(current.getX());
    }

    @Override
    public int getChunkZ() {
        return SectionPos.blockToSectionCoord(current.getZ());
    }

    @Override
    public double distanceSq(BlockNode blockNode) {
        return blockNode.getCurrent().distSqr(this.current);
    }

    @Override
    public int compareTo(@NotNull BlockNode o) {
        int compare = Double.compare(this.offsetToCenter.distManhattan(BlockPos.ZERO), o.offsetToCenter.distManhattan(BlockPos.ZERO));
        if (compare == 0) {
            compare = Integer.compare(this.offsetToCenter.getY(), o.offsetToCenter.getY());
            if (compare == 0) {
                compare = this.offsetToCenter.compareTo(o.offsetToCenter);
            }
        }

        return compare;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockNode blockNode)) return false;
        return Objects.equals(getOffsetToCenter(), blockNode.getOffsetToCenter());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOffsetToCenter());
    }

    public void setToOffsetFromCenter(BlockPos center) {
        current.set(center.getX() + offsetToCenter.getX(), center.getY() + offsetToCenter.getY(), center.getZ() + offsetToCenter.getZ());
    }

}
