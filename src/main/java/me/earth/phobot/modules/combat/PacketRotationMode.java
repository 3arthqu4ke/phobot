package me.earth.phobot.modules.combat;

import me.earth.phobot.services.SurroundService;

public enum PacketRotationMode {
    None,
    Always,
    Surrounded;

    public boolean shouldUsePackets(SurroundService surroundService) {
        return switch (this) {
            case None -> false;
            case Always -> true;
            case Surrounded -> surroundService.isSurrounded();
        };
    }

}
