package com.sirens.visuals.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;

import java.util.Random;

public final class Fx {
    private Fx() { }
    public static final Random R = new Random();

    /** Index order must match Mods.PARTICLES. */
    public static ParticleOptions particle(int idx, int rgb, float size) {
        return switch (idx) {
            case 0 -> ParticleTypes.END_ROD;
            case 1 -> ParticleTypes.ELECTRIC_SPARK;
            case 2 -> ParticleTypes.FLAME;
            case 3 -> ParticleTypes.SOUL_FIRE_FLAME;
            case 4 -> ParticleTypes.HEART;
            case 5 -> ParticleTypes.NOTE;
            case 6 -> ParticleTypes.CRIT;
            case 7 -> ParticleTypes.ENCHANT;
            case 8 -> ParticleTypes.GLOW;
            case 9 -> ParticleTypes.TOTEM_OF_UNDYING;
            case 10 -> ParticleTypes.CLOUD;
            case 11 -> ParticleTypes.SNOWFLAKE;
            case 12 -> ParticleTypes.FIREWORK;
            case 13 -> ParticleTypes.HAPPY_VILLAGER;
            case 14 -> ParticleTypes.ANGRY_VILLAGER;
            default -> new DustParticleOptions(rgb & 0xFFFFFF, size);
        };
    }

    public static void dust(ClientLevel lv, double x, double y, double z, int rgb, float size) {
        lv.addParticle(new DustParticleOptions(rgb & 0xFFFFFF, size), x, y, z, 0, 0, 0);
    }

    public static int rainbow(float t) { return Mth.hsvToRgb(((t % 1f) + 1f) % 1f, 0.75f, 1f); }

    public static int lerpRgb(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }

    public static void ring(ClientLevel lv, double x, double y, double z, double rad, int n, ParticleOptions o) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            lv.addParticle(o, x + Math.cos(a) * rad, y, z + Math.sin(a) * rad, 0, 0, 0);
        }
    }
}
