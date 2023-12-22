package me.earth.phobot;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PhobotApi {
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private static Phobot phobot;

}
