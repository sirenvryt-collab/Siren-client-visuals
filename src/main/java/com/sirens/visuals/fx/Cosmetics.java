package com.sirens.visuals.fx;

import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;

/** Particle-based cosmetics around the local player. Only visible to you (third person or with the first-person option). */
public final class Cosmetics {
    private Cosmetics() { }

    static int col(float t, boolean second) {
        if (Mods.CO_RAINBOW.on()) return Fx.rainbow(t);
        return second ? Mods.CO_C2.rgb() : Mods.CO_C1.rgb();
    }

    /** Position relative to the player body: right/back/up in blocks, rotated by body yaw. */
    static double[] at(LocalPlayer p, double right, double back, double up) {
        double yaw = Math.toRadians(p.yBodyRot);
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw), bx = Math.sin(yaw), bz = -Math.cos(yaw);
        return new double[]{p.getX() + rx * right + bx * back, p.getY() + up, p.getZ() + rz * right + bz * back};
    }

    static boolean chance(float perTick) {
        int whole = (int) perTick;
        return Fx.R.nextFloat() < (perTick - whole);
    }

    public static void tick(Minecraft mc, ClientLevel lv, LocalPlayer p, int tick) {
        if (!Mods.COSM.on() || Mods.CO_TYPE.idx() == 0) return;
        if (!Mods.CO_FIRST.on() && mc.options.getCameraType().isFirstPerson()) return;
        if (p.isInvisible() || p.isSpectator()) return;
        float size = Mods.CO_SIZE.f(), dens = Mods.CO_DENS.f();
        float t = tick * 0.05f;
        double h = p.getBbHeight();
        switch (Mods.CO_TYPE.idx()) {
            case 1 -> { // halo
                int n = Math.max(2, (int) (5 * dens));
                for (int i = 0; i < n; i++) {
                    double a = tick * 0.15 + i * Math.PI * 2 / n;
                    double r = 0.36 * size;
                    Fx.dust(lv, p.getX() + Math.cos(a) * r, p.getY() + h + 0.32 * size, p.getZ() + Math.sin(a) * r, col(t + i * 0.05f, false), 0.55f);
                }
            }
            case 2 -> { // wings
                int rows = 3;
                float flap = (float) Math.sin(tick * 0.22) * 0.25f;
                for (int side = -1; side <= 1; side += 2) {
                    for (int r = 0; r < rows; r++) {
                        int pts = Math.max(1, (int) (2 * dens));
                        for (int k = 0; k < pts; k++) {
                            float u = Fx.R.nextFloat();
                            double right = side * (0.22 + u * (0.55 + r * 0.25) * size);
                            double up = 1.05 + (r - 1) * 0.28 * size + Math.sin(u * Math.PI) * 0.35 * size + u * flap * size;
                            double back = 0.2 + u * 0.18 * size;
                            double[] q = at(p, right, back, up - (p.isCrouching() ? 0.2 : 0) );
                            Fx.dust(lv, q[0], q[1], q[2], col(t + u * 0.3f, r % 2 == 1), 0.5f);
                        }
                    }
                }
            }
            case 3 -> { // aura
                int n = Math.max(1, (int) (3 * dens));
                for (int i = 0; i < n; i++) {
                    double a = Fx.R.nextDouble() * Math.PI * 2, yy = Fx.R.nextDouble() * h;
                    double r = (0.55 + 0.15 * Fx.R.nextDouble()) * size;
                    Fx.dust(lv, p.getX() + Math.cos(a) * r, p.getY() + yy, p.getZ() + Math.sin(a) * r, col(t + (float) yy * 0.2f, Fx.R.nextBoolean()), 0.5f);
                }
            }
            case 4 -> { // orbit
                for (int k = 0; k < 3; k++) {
                    for (int trail = 0; trail < 3; trail++) {
                        double a = tick * 0.18 - trail * 0.12 + k * Math.PI * 2 / 3;
                        double r = 0.95 * size;
                        Fx.dust(lv, p.getX() + Math.cos(a) * r, p.getY() + h * 0.55 + Math.sin(tick * 0.1 + k) * 0.25, p.getZ() + Math.sin(a) * r, col(t + k * 0.2f, k == 1), 0.7f - trail * 0.15f);
                    }
                }
            }
            case 5 -> { // crown
                int spikes = 5;
                for (int i = 0; i < spikes; i++) {
                    double a = i * Math.PI * 2 / spikes + tick * 0.01;
                    double r = 0.3 * size, bx = p.getX() + Math.cos(a) * r, bz = p.getZ() + Math.sin(a) * r;
                    if (chance(0.5f * dens)) {
                        for (int s = 0; s < 3; s++) Fx.dust(lv, bx, p.getY() + h + 0.05 + s * 0.08 * size, bz, col(t, false), 0.5f);
                        lv.addParticle(ParticleTypes.END_ROD, bx, p.getY() + h + 0.3 * size, bz, 0, 0, 0);
                    }
                }
                for (int i = 0; i < 4; i++) {
                    double a = tick * 0.2 + i * Math.PI / 2;
                    Fx.dust(lv, p.getX() + Math.cos(a) * 0.3 * size, p.getY() + h + 0.05, p.getZ() + Math.sin(a) * 0.3 * size, col(t + 0.3f, true), 0.45f);
                }
            }
            case 6 -> { // trail
                double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
                if (dx * dx + dz * dz > 0.0004) {
                    int n = Math.max(1, (int) (3 * dens));
                    for (int i = 0; i < n; i++)
                        Fx.dust(lv, p.getX() + (Fx.R.nextDouble() - .5) * 0.4, p.getY() + 0.1 + Fx.R.nextDouble() * 0.2, p.getZ() + (Fx.R.nextDouble() - .5) * 0.4, col(t + i * 0.1f, i % 2 == 1), 0.7f * size);
                }
            }
            case 7 -> { // cape sparkle
                if (chance(Math.min(1f, 0.6f * dens))) {
                    double[] q = at(p, (Fx.R.nextDouble() - .5) * 0.5, 0.3, 0.6 + Fx.R.nextDouble() * 0.8);
                    lv.addParticle(ParticleTypes.END_ROD, q[0], q[1], q[2], 0, -0.01, 0);
                }
            }
            default -> { }
        }
    }
}
