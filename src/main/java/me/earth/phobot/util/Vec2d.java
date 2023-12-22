package me.earth.phobot.util;

public record Vec2d(double x, double z) {
    public static final Vec2d ZERO = new Vec2d(0.0, 0.0);

    public Vec2d normalize() {
        double d = Math.sqrt(this.x * this.x + this.z * this.z);
        if (d < 1.0E-4) {
            return ZERO;
        }

        return new Vec2d(this.x / d, this.z / d);
    }

    public double dot(Vec2d vec) {
        return this.x * vec.x + this.z * vec.z;
    }

    public Vec2d subtract(Vec2d vec) {
        return this.subtract(vec.x, vec.z);
    }

    public Vec2d subtract(double x, double z) {
        return this.add(-x, -z);
    }

    public Vec2d add(Vec2d vec) {
        return this.add(vec.x, vec.z);
    }

    public Vec2d add(double x, double z) {
        return new Vec2d(this.x + x, this.z + z);
    }

    public Vec2d scale(double d) {
        return this.multiply(d, d);
    }

    public Vec2d reverse() {
        return this.scale(-1.0);
    }

    public Vec2d multiply(Vec2d vec) {
        return this.multiply(vec.x, vec.z);
    }

    public Vec2d multiply(double x, double z) {
        return new Vec2d(this.x * x, this.z * z);
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.z * this.z);
    }

    public double lengthSqr() {
        return this.x * this.x + this.z * this.z;
    }

}
