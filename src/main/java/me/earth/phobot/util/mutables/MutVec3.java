package me.earth.phobot.util.mutables;

import lombok.Data;
import net.minecraft.world.phys.Vec3;

@Data
public class MutVec3 {
    private double x;
    private double y;
    private double z;

    public void set(double x, double y, double z) {
        setX(x);
        setY(y);
        setZ(z);
    }

    public void set(Vec3 vec) {
        set(vec.x, vec.y, vec.z);
    }

    public void set(MutVec3 vec) {
        set(vec.getX(), vec.getY(), vec.getZ());
    }

    public void normalize() {
        double length = Math.sqrt(getX() * getX() + getY() * getY() + getZ() * getZ());
        if (length < 1.0E-4) {
            set(0.0, 0.0, 0.0);
        } else {
            set(getX() / length, getY() / length, getZ() / length);
        }
    }

    public void scale(double factor) {
        set(getX() * factor, getY() * factor, getZ() * factor);
    }

    public double distanceSq(double x, double y, double z) {
        double xDiff = x - this.getX();
        double yDiff = y - this.getY();
        double zDiff = z - this.getZ();
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
    }

    public Vec3 immutable() {
        return new Vec3(getX(), getY(), getZ());
    }

}
