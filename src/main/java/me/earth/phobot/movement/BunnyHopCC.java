package me.earth.phobot.movement;

public class BunnyHopCC extends BunnyHop {
    @Override
    public double getBaseSpeed() {
        // return 0.28700000047683716;
        return BASE_SPEED;
    }

    @Override
    public double getJumpY() {
        return 0.4000000059604645;
    }

    @Override
    public double getStage2Speed(boolean boost) {
        return 1.535;
    }

}
