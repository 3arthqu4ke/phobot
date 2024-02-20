package me.earth.phobot.util.math;

import lombok.experimental.UtilityClass;
import net.minecraft.world.phys.Vec3;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
public class MathUtil {
    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }

        return new BigDecimal(Double.toString(value)).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    public static float clamp(float value, float min, float max) {
        return value > max ? max : Math.max(value, min);
    }

    public static double clamp(double value, double min, double max) {
        return value > max ? max : Math.max(value, min);
    }

    public static double distance2dSq(double x1, double z1, double x2, double z2) {
        double x = x1 - x2;
        double z = z1 - z2;
        return x * x + z * z;
    }

    public static double distanceSq(double x1, double y1, double z1, double x2, double y2, double z2) {
        double x = x1 - x2;
        double y = y1 - y2;
        double z = z1 - z2;
        return x * x + y * y + z * z;
    }

    public static double manhattan(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2) + Math.abs(z1 - z2);
    }

    public static double getAngle(Vec3 v1, Vec3 v2) {
        double dotProduct = v1.dot(v2);
        double magnitude1 = v1.length();
        double magnitude2 = v2.length();
        double cosAngle = dotProduct / (magnitude1 * magnitude2);
        double angleInRadians = Math.acos(cosAngle);
        return Math.toDegrees(angleInRadians);
    }

}
