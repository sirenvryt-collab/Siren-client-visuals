package com.sirens.visuals.fx;

import com.sirens.visuals.cfg.Mods;

public final class WaterFx {
    private WaterFx() { }

    /** Returns the new water ARGB/RGB colour, or null to leave vanilla. */
    public static Integer tint(int vanilla) {
        if (!Mods.WATER.on() || Mods.W_TINT.idx() == 0) return null;
        int target = switch (Mods.W_TINT.idx()) {
            case 1 -> 0x3FD8FF;
            case 2 -> 0x19E6C8;
            case 3 -> 0x1C4FD6;
            case 4 -> 0x4A6B3E;
            default -> Mods.W_CUSTOM.rgb();
        };
        int out = Fx.lerpRgb(vanilla & 0xFFFFFF, target, Mods.W_STR.f());
        return (vanilla & 0xFF000000) | out;
    }
}
