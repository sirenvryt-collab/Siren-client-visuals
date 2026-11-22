package com.sirens.visuals.fx;

import com.sirens.visuals.cfg.Mods;
import net.minecraft.world.phys.Vec3;

public final class SkyFx {
    private SkyFx() { }

    public static Long time(long vanilla) {
        if (!Mods.SKY.on()) return null;
        long day = Math.floorDiv(vanilla, 24000L);
        int ph = Mods.S_PHASE.idx();
        if (ph > 0) day = ph - 1; // vanilla moon phase = (dayTime / 24000) % 8
        return switch (Mods.S_MODE.idx()) {
            case 1 -> day * 24000L + Mods.S_TIME.i();
            case 2 -> {
                long t = (long) (vanilla * (double) Mods.S_SPEED.f());
                yield ph > 0 ? (day * 24000L + Math.floorMod(t, 24000L)) : t;
            }
            default -> ph > 0 ? day * 24000L + Math.floorMod(vanilla, 24000L) : null;
        };
    }

    public static Vec3 skyColor(Vec3 v) {
        if (!Mods.SKY.on() || !Mods.S_TINT.on()) return null;
        int c = Mods.S_TINTC.rgb();
        float s = Mods.S_TINTS.f();
        double r = ((c >> 16) & 255) / 255.0, g = ((c >> 8) & 255) / 255.0, b = (c & 255) / 255.0;
        double lum = Math.max(0.15, (v.x + v.y + v.z) / 3.0 / 0.6);
        lum = Math.min(1.2, lum);
        return new Vec3(v.x + (r * lum - v.x) * s, v.y + (g * lum - v.y) * s, v.z + (b * lum - v.z) * s);
    }

    public static Float stars(float vanilla) {
        if (!Mods.SKY.on() || Mods.S_STARS.f() <= 0f) return null;
        return Math.max(vanilla, Mods.S_STARS.f());
    }
}
