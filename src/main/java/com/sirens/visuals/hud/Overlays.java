package com.sirens.visuals.hud;

import com.sirens.visuals.Hooks;
import com.sirens.visuals.cfg.Mods;
import com.sirens.visuals.fx.Visuals;
import com.sirens.visuals.gui.UI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Full-screen effects and crosshair feedback. */
public final class Overlays {
    private Overlays() { }

    static final Identifier FIRE = Identifier.fromNamespaceAndPath("minecraft", "textures/block/fire_1.png");
    static float uw, aur, fireK;

    static float time() { return (System.currentTimeMillis() % 3_600_000L) / 1000f; }

    // ===================================================================== underlay
    public static void under(GuiGraphics g, Minecraft mc) {
        LocalPlayer p = mc.player;
        int W = mc.getWindow().getGuiScaledWidth(), H = mc.getWindow().getGuiScaledHeight();
        if (Hud.cinematicHidden()) return;
        wet(g, W, H);
        underwater(g, mc, p, W, H);
        lowFire(g, mc, p, W, H);
        vignette(g, p, W, H);
        aurora(g, mc, p, W, H);
    }

    static void wet(GuiGraphics g, int W, int H) {
        if (!Mods.WET.on() || !Mods.WET_GRADE.on() || Visuals.wet < 0.02f) return;
        int a = (int) (Visuals.wet * Mods.WET_STR.f() * 70);
        g.fill(0, 0, W, H, UI.argb(a, Mods.WET_COL.rgb()));
        // slightly cooler, softer edges
        int e = H / 6, a2 = (int) (Visuals.wet * Mods.WET_STR.f() * 40);
        g.fillGradient(0, H - e, W, H, UI.argb(0, 0x0A1426), UI.argb(a2, 0x0A1426));
    }

    static void underwater(GuiGraphics g, Minecraft mc, LocalPlayer p, int W, int H) {
        boolean in = Mods.WATER.on() && p.isUnderWater();
        uw = UI.ease(uw, in ? 1f : 0f, 4f);
        if (uw < 0.01f) return;
        float depth = Mth.clamp((63f - (float) p.getEyeY()) / 60f, 0f, 1f);
        float t = time();
        if (Mods.W_OVER.on()) g.fill(0, 0, W, H, UI.argb((int) (uw * Mods.W_OVERS.f() * 255), Mods.W_OVERC.rgb()));
        if (Mods.W_DEPTH.on()) g.fill(0, 0, W, H, UI.argb((int) (uw * depth * 150), 0x00060F));
        // vignette
        int e = H / 4;
        g.fillGradient(0, 0, W, e, UI.argb((int) (uw * 70), 0x021428), UI.argb(0, 0x021428));
        g.fillGradient(0, H - e, W, H, UI.argb(0, 0x021428), UI.argb((int) (uw * 90), 0x021428));
        float light = (1f - depth * 0.8f) * uw;
        if (Mods.W_RAYS.on()) {
            int rays = 6, step = 6;
            for (int i = 0; i < rays; i++) {
                float x0 = W * (i + 0.5f) / rays + (float) Math.sin(t * 0.35 + i * 1.7) * 28f;
                float wid = 16 + 10 * (float) Math.sin(t * 0.5 + i * 2.1);
                float slope = 0.35f + 0.1f * (float) Math.sin(t * 0.2 + i);
                for (int y = 0; y < H; y += step) {
                    float k = 1f - (float) y / H;
                    int a = (int) (34 * light * k * k);
                    if (a <= 0) continue;
                    int x1 = (int) (x0 + y * slope - wid / 2 - y * 0.08f);
                    g.fill(x1, y, (int) (x1 + wid + y * 0.16f), y + step, UI.argb(a, 0xBFE9FF));
                }
            }
        }
        if (Mods.W_CAUS.on()) {
            int cell = 12;
            for (int y = 0; y < H; y += cell) {
                for (int x = 0; x < W; x += cell) {
                    double v = Math.sin(x * 0.045 + t * 1.1) + Math.sin(y * 0.06 - t * 0.9) + Math.sin((x + y) * 0.03 + t * 0.7) + Math.sin((x - y) * 0.05 - t * 1.3);
                    double line = 1.0 - Math.abs(v) * 0.9;
                    if (line > 0.62) {
                        int a = (int) ((line - 0.62) / 0.38 * 60 * light);
                        if (a > 2) g.fill(x, y, x + cell - 2, y + cell - 2, UI.argb(a, 0xD8F6FF));
                    }
                }
            }
        }
    }

    static void lowFire(GuiGraphics g, Minecraft mc, LocalPlayer p, int W, int H) {
        boolean show = Mods.LOWFIRE.on() && p.isOnFire() && Visuals.tickN - Visuals.fireTick < 8;
        fireK = UI.ease(fireK, show ? 1f : 0f, 10f);
        if (fireK < 0.02f) return;
        int T = Math.max(16, (int) (H * Mods.LF_HEIGHT.f()));
        if (Mods.LF_GLOW.on())
            g.fillGradient(0, H - (int) (T * 1.5f), W, H, UI.argb(0, 0xFF5A00), UI.argb((int) (110 * fireK), 0xFF5A00));
        float sx = T / 16f * (4f / Mods.LF_TILE.f()), sy = T / 16f * fireK;
        int colW = Math.max(8, (int) (16 * sx));
        int cols = W / colW + 2;
        int tick = Visuals.tickN;
        for (int c = 0; c < cols; c++) {
            int frame = Math.floorMod(tick / 2 + c * 7, 32);
            UI.pushScale2(g, c * colW - colW / 2f, H - T * fireK + (1f - fireK) * 0, sx, sy);
            g.blit(RenderPipelines.GUI_TEXTURED, FIRE, 0, 0, 0f, (float) (frame * 16), 16, 16, 16, 512);
            UI.pop(g);
        }
    }

    static void vignette(GuiGraphics g, LocalPlayer p, int W, int H) {
        if (!Mods.LOWHP.on()) return;
        float lim = Mods.LOWHP_AT.f(), hp = p.getHealth();
        if (hp > lim) return;
        float k = (lim - hp) / lim, pulse = 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 200.0);
        int a = (int) (140 * k * pulse * Mods.LOWHP_STR.f()), h = H / 5, c = Mods.LOWHP_COL.rgb();
        g.fillGradient(0, 0, W, h, UI.argb(a, c), UI.argb(0, c));
        g.fillGradient(0, H - h, W, H, UI.argb(0, c), UI.argb(a, c));
        int w = W / 8;
        for (int i = 0; i < w; i += 4) {
            int al = (int) (a * 0.5f * (1f - (float) i / w));
            g.fill(i, 0, i + 4, H, UI.argb(al, c));
            g.fill(W - i - 4, 0, W - i, H, UI.argb(al, c));
        }
    }

    static void aurora(GuiGraphics g, Minecraft mc, LocalPlayer p, int W, int H) {
        if (!Mods.AURORA.on()) return;
        boolean night = true;
        if (Mods.A_NIGHT.on() && mc.level != null) { long t = ((mc.level.getDayTime() % 24000L) + 24000L) % 24000L; night = t > 12500 && t < 23500; }
        float up = night ? Mth.clamp((-p.getXRot() - 8f) / 40f, 0, 1) : 0;
        aur = UI.ease(aur, up, 6f);
        if (aur <= 0.01f) return;
        float t = time() * Mods.A_SPEED.f();
        int step = 6, mode = Mods.A_COLOR.idx();
        for (int x = 0; x < W; x += step) {
            float w1 = (float) Math.sin(x * 0.02 + t * 0.8) * 0.5f + 0.5f, w2 = (float) Math.sin(x * 0.05 - t * 1.3) * 0.5f + 0.5f;
            int h = (int) (H * (0.15f + 0.3f * w1 * aur)), top = (int) (H * 0.05f + w2 * H * 0.1f);
            int al = (int) (Mods.A_STR.f() * aur * (70 + 110 * w2));
            float hue = switch (mode) {
                case 1 -> 0.75f + 0.08f * w1;
                case 2 -> 0.5f + 0.05f * w1;
                case 3 -> (t * 0.03f + x / (float) W * 0.8f) % 1f;
                default -> 0.33f + 0.2f * w1;
            };
            int c = Mth.hsvToRgb(hue, 0.7f, 1f);
            g.fillGradient(x, top, x + step, top + h / 2, UI.argb(0, c), UI.argb(al, c));
            g.fillGradient(x, top + h / 2, x + step, top + h, UI.argb(al, c), UI.argb(0, c));
        }
    }

    // ===================================================================== crosshair feedback
    public static void over(GuiGraphics g, Minecraft mc) {
        int W = mc.getWindow().getGuiScaledWidth(), H = mc.getWindow().getGuiScaledHeight();
        if (Mods.DMG.on()) {
            float s = Mods.DMG_SCALE.f();
            for (Visuals.Pop q : Visuals.pops) {
                float k = q.age / 25f;
                UI.pushScale(g, W / 2f + 14 + q.dx, H / 2f - 6 - q.age * 0.9f * Mods.DMG_RISE.f(), s);
                g.drawString(mc.font, q.s, 0, 0, UI.argb((int) (255 * (1 - k)), Mods.DMG_COL.rgb()), true);
                UI.pop(g);
            }
        }
        if (Mods.HITMARK.on() && Visuals.marker > 0) {
            int a = 255 * Visuals.marker / 6, c = UI.argb(a, Mods.HM_COL.rgb()), cx = W / 2, cy = H / 2;
            int gap = 3, len = Math.max(2, (int) (5 * Mods.HM_SIZE.f()));
            switch (Mods.HM_STYLE.idx()) {
                case 1 -> {
                    g.fill(cx - gap - len, cy, cx - gap, cy + 1, c); g.fill(cx + gap + 1, cy, cx + gap + len + 1, cy + 1, c);
                    g.fill(cx, cy - gap - len, cx + 1, cy - gap, c); g.fill(cx, cy + gap + 1, cx + 1, cy + gap + len + 1, c);
                }
                case 2 -> {
                    int r = (int) (7 * Mods.HM_SIZE.f()) + (6 - Visuals.marker);
                    for (int i = 0; i < 36; i++) {
                        double an = i * Math.PI / 18;
                        int px = cx + (int) Math.round(Math.cos(an) * r), py = cy + (int) Math.round(Math.sin(an) * r);
                        g.fill(px, py, px + 1, py + 1, c);
                    }
                }
                default -> {
                    for (int i = 0; i < len; i++) {
                        int o = gap + i;
                        g.fill(cx - o, cy - o, cx - o + 1, cy - o + 1, c); g.fill(cx + o, cy - o, cx + o + 1, cy - o + 1, c);
                        g.fill(cx - o, cy + o, cx - o + 1, cy + o + 1, c); g.fill(cx + o, cy + o, cx + o + 1, cy + o + 1, c);
                    }
                }
            }
        }
    }
}
