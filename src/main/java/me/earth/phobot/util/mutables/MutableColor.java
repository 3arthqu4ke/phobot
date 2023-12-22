package me.earth.phobot.util.mutables;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;

@Getter
@Setter
public class MutableColor {
    private float red;
    private float green;
    private float blue;
    private float alpha;

    public void set(MutableColor color) {
        set(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public void set(float red, float green, float blue, float alpha) {
        setRed(red);
        setGreen(green);
        setBlue(blue);
        setAlpha(alpha);
    }

    public void set(Color color) {
        set(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
    }

}
