package me.earth.phobot.util.mutables;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@MethodsReturnNonnullByDefault
public class MutPos extends BlockPos.MutableBlockPos {
    @Override
    public MutPos setX(int x) {
        super.setX(x);
        return this;
    }

    @Override
    public MutPos setY(int y) {
        super.setY(y);
        return this;
    }

    @Override
    public MutPos setZ(int z) {
        super.setZ(z);
        return this;
    }

    public void setRelative(Direction direction) {
        set(this.getX() + direction.getStepX(), this.getY() + direction.getStepY(), this.getZ() + direction.getStepZ());
    }

    public void set(Entity entity) {
        Vec3 pos = entity.position();
        set(pos.x, pos.y, pos.z);
    }

    public void incrementX(int by) {
        super.setX(this.getX() + by);
    }

    public void incrementY(int by) {
        super.setY(this.getY() + by);
    }

    public void incrementZ(int by) {
        super.setZ(this.getZ() + by);
    }

}
