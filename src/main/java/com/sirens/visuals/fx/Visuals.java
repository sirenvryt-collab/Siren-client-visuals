package com.sirens.visuals.fx;

import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Per-tick gameplay effects and the shared state read by the HUD layer. */
public final class Visuals {
    private Visuals() { }

    public static final class Pop { public String s; public int age; public float dx; }

    public static int tickN, fireTick = -100, combo, marker;
    public static float wet, speed;
    public static boolean cinematicOn;
    public static final List<Pop> pops = new ArrayList<>();

    private static final Map<Integer, Float> tracked = new HashMap<>();
    private static final Map<Integer, Integer> trackedAge = new HashMap<>();
    private static int lastHit = -1000;
    private static double stepDist;
    private static boolean nvForced;

    // click counters (polled per frame so fast clicking is counted)
    private static final long[] lc = new long[256], rc = new long[256];
    private static int lh, rh, ln, rn;
    private static boolean lDown, rDown;

    public static void pollClicks(Minecraft mc) {
        if (mc.options == null) return;
        long now = System.currentTimeMillis();
        boolean l = mc.options.keyAttack.isDown(), r = mc.options.keyUse.isDown();
        if (l && !lDown) { lc[lh++ & 255] = now; ln = Math.min(256, ln + 1); }
        if (r && !rDown) { rc[rh++ & 255] = now; rn = Math.min(256, rn + 1); }
        lDown = l; rDown = r;
    }

    public static int cps(boolean right) {
        long now = System.currentTimeMillis();
        long[] a = right ? rc : lc;
        int h = right ? rh : lh, n = right ? rn : ln, c = 0;
        for (int i = 1; i <= n; i++) if (now - a[(h - i) & 255] <= 1000) c++; else break;
        return c;
    }

    /** Called by the attack callback. */
    public static void onAttack(Entity target) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel lv = mc.level;
        if (lv == null || target == null) return;
        if (tickN - lastHit > 40) combo = 0;
        combo++;
        lastHit = tickN;
        marker = 6;
        if (target instanceof LivingEntity le) {
            tracked.put(target.getId(), le.getHealth());
            trackedAge.put(target.getId(), tickN);
        }
        if (Mods.HITFX.on()) {
            int rings = Mods.HIT_RINGS.i();
            double y = target.getY() + target.getBbHeight() * 0.5;
            for (int r = 0; r < rings; r++) {
                double rad = (0.45 + r * 0.35) * Mods.HIT_SIZE.f();
                Fx.ring(lv, target.getX(), y, target.getZ(), rad, 18 + r * 6,
                        Fx.particle(Mods.HIT_PART.idx(), Mods.HIT_COL.rgb(), 1f));
            }
        }
    }

    static void kill(ClientLevel lv, Entity e) {
        if (!Mods.KILLFX.on()) return;
        var o = switch (Mods.KILL_STYLE.idx()) {
            case 0 -> ParticleTypes.TOTEM_OF_UNDYING;
            case 1 -> ParticleTypes.SOUL;
            case 2 -> ParticleTypes.FIREWORK;
            case 3 -> ParticleTypes.HEART;
            case 4 -> ParticleTypes.END_ROD;
            case 5 -> ParticleTypes.FLAME;
            case 6 -> ParticleTypes.SNOWFLAKE;
            default -> ParticleTypes.ENCHANT;
        };
        int n = (int) (14 * Mods.KILL_INT.f());
        for (int i = 0; i < n; i++)
            lv.addParticle(o, e.getX() + (Fx.R.nextDouble() - .5) * e.getBbWidth(), e.getY() + Fx.R.nextDouble() * e.getBbHeight(),
                    e.getZ() + (Fx.R.nextDouble() - .5) * e.getBbWidth(), (Fx.R.nextDouble() - .5) * 0.2, 0.1 + Fx.R.nextDouble() * 0.15, (Fx.R.nextDouble() - .5) * 0.2);
    }

    public static void tick(Minecraft mc) {
        tickN++;
        ClientLevel lv = mc.level;
        LocalPlayer p = mc.player;
        if (lv == null || p == null) { pops.clear(); tracked.clear(); return; }

        if (marker > 0) marker--;
        if (tickN - lastHit > 60) combo = 0;
        for (Iterator<Pop> it = pops.iterator(); it.hasNext(); ) { Pop q = it.next(); if (++q.age > 25) it.remove(); }

        // damage numbers + kill effects for entities we hit
        for (Iterator<Map.Entry<Integer, Float>> it = tracked.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Entity e = lv.getEntity(en.getKey());
            if (!(e instanceof LivingEntity le) || tickN - trackedAge.getOrDefault(en.getKey(), 0) > 100) { it.remove(); continue; }
            float hp = le.getHealth(), old = en.getValue();
            if (hp < old - 0.01f) {
                if (Mods.DMG.on()) {
                    Pop q = new Pop();
                    q.s = String.format("-%.1f", old - hp);
                    q.dx = Fx.R.nextInt(20) - 10;
                    pops.add(q);
                }
                en.setValue(hp);
            } else if (hp > old) en.setValue(hp);
            if (le.isDeadOrDying() || hp <= 0) { kill(lv, e); it.remove(); }
        }

        // speed
        double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
        speed = (float) (Math.sqrt(dx * dx + dz * dz) * 20.0);

        // wetness
        BlockPos bp = p.blockPosition();
        boolean inWater = p.isInWater() || p.isUnderWater();
        boolean rained = lv.isRaining() && lv.canSeeSky(bp.above());
        float dry = Math.max(5f, Mods.WET_DRY.f()) * 20f;
        if (Mods.WET.on()) {
            if (inWater) wet = Math.min(1f, wet + 0.08f);
            else if (rained) wet = Math.min(1f, wet + 0.01f);
            else wet = Math.max(0f, wet - 1f / dry);
        } else wet = Math.max(0f, wet - 0.05f);

        if (Mods.WET.on()) {
            if (Mods.WET_PUDDLE.on() && lv.isRaining()) {
                int n = (int) (3 * Mods.WET_DENS.f());
                for (int i = 0; i < n; i++) {
                    int x = bp.getX() + Fx.R.nextInt(17) - 8, z = bp.getZ() + Fx.R.nextInt(17) - 8;
                    BlockPos g = lv.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, bp.getY(), z));
                    if (Math.abs(g.getY() - bp.getY()) < 6)
                        lv.addParticle(ParticleTypes.RAIN, x + Fx.R.nextDouble(), g.getY() + 0.05, z + Fx.R.nextDouble(), 0, 0, 0);
                }
            }
            if (wet > 0.3f && p.onGround() && speed > 1f && Mods.WET_STEPS.on()) {
                stepDist += speed / 20.0;
                if (stepDist > 0.8) {
                    stepDist = 0;
                    lv.addParticle(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.05, p.getZ(), 0, 0.02, 0);
                }
            }
            if (Mods.WET_DRIP.on() && wet > 0.35f && !inWater && Fx.R.nextFloat() < wet * 0.2f)
                lv.addParticle(ParticleTypes.FALLING_WATER, p.getX() + (Fx.R.nextDouble() - .5) * 0.6, p.getY() + 0.3 + Fx.R.nextDouble() * 1.4, p.getZ() + (Fx.R.nextDouble() - .5) * 0.6, 0, 0, 0);
        }

        // footsteps
        if (Mods.STEPS.on() && p.onGround() && speed > 1f) {
            double d = Math.sqrt(dx * dx + dz * dz);
            footDist += d;
            if (footDist >= Mods.ST_GAP.f()) {
                footDist = 0; footSide = !footSide;
                double yaw = Math.toRadians(p.getYRot()), side = footSide ? 0.15 : -0.15;
                double ox = Math.cos(yaw) * side, oz = Math.sin(yaw) * side;
                lv.addParticle(Fx.particle(Mods.ST_PART.idx(), Mods.ST_COL.rgb(), Mods.ST_SIZE.f()), p.getX() + ox, p.getY() + 0.03, p.getZ() + oz, 0, 0, 0);
            }
        }

        // fire overlay timestamp (fallback when the render hook is not available)
        if (p.isOnFire()) fireTick = tickN;

        // fullbright via a client-side night vision effect (no particles / icon)
        if (Mods.FULLBRIGHT.on()) {
            MobEffectInstance cur = p.getEffect(MobEffects.NIGHT_VISION);
            if (cur == null || cur.getDuration() < 220) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, false));
                nvForced = true;
            }
        } else if (nvForced) {
            p.removeEffect(MobEffects.NIGHT_VISION);
            nvForced = false;
        }

        if (Mods.NOWEATHER.on()) { lv.setRainLevel(0f); lv.setThunderLevel(0f); }

        Cosmetics.tick(mc, lv, p, tickN);
        Emotes.tick(lv, p);
    }

    private static double footDist;
    private static boolean footSide;
}
