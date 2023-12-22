package me.earth.phobot.ducks;

import me.earth.phobot.util.time.TimeUtil;

public interface IEntity {
    long phobot$getAttackTime();

    void phobot$setAttackTime(long time);

    default long phobot$GetTimeSinceAttack() {
        return TimeUtil.getMillis() - phobot$getAttackTime();
    }

}
