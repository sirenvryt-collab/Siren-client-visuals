package com.sirens.visuals.gui;

import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Small immediate-mode drawing helpers shared by the menu, editors and HUD. */
public final class UI {
    private UI() { }

    private static long lastNs = System.nanoTime();
    private static float dt = 0.016f;

    /** Call once per rendered frame of any screen that animates. */
    public static void beginFrame() {
        long n = System.nanoTime();
        dt = Mth.clamp((n - lastNs) / 1_000_000_000f, 0.0005f, 0.05f);
        lastNs = n;
    }
    public static float dt() { return dt; }

    public static Font font() { return Minecraft.getInstance().font; }
    public static int tw(String s) { return font().width(s); }

    public static int argb(int a, int rgb) { return (Mth.clamp(a, 0, 255) << 24) | (rgb & 0xFFFFFF); }
    public static int alpha(int argb, float mul) {
        int a = Mth.clamp((int) (((argb >>> 24) & 255) * mul), 0, 255);
        return (a << 24) | (argb & 0xFFFFFF);
    }
    public static int mix(int c1, int c2, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int a = (int) Mth.lerp(t, (c1 >>> 24) & 255, (c2 >>> 24) & 255);
        int r = (int) Mth.lerp(t, (c1 >> 16) & 255, (c2 >> 16) & 255);
        int g = (int) Mth.lerp(t, (c1 >> 8) & 255, (c2 >> 8) & 255);
        int b = (int) Mth.lerp(t, c1 & 255, c2 & 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    public static int accent() { return 0xFF000000 | Mods.M_ACCENT.rgb(); }
    public static int accent2() { return 0xFF000000 | Mods.M_ACCENT2.rgb(); }

    public static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Exponential smoothing towards a target, frame-rate independent. */
    public static float ease(float cur, float target, float speed) {
        if (!Mods.M_ANIM.on()) return target;
        float k = 1f - (float) Math.exp(-speed * dt);
        float r = cur + (target - cur) * k;
        return Math.abs(r - target) < 0.001f ? target : r;
    }

    public static void rect(GuiGraphics g, int x, int y, int w, int h, int c) { if (w > 0 && h > 0) g.fill(x, y, x + w, y + h, c); }

    public static void rrect(GuiGraphics g, int x, int y, int w, int h, int r, int c) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { g.fill(x, y, x + w, y + h, c); return; }
        g.fill(x, y + r, x + w, y + h - r, c);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, c);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, c);
        }
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y + 1, x + 1, y + h - 1, c);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    /** Horizontal gradient drawn as vertical strips. */
    public static void hgrad(GuiGraphics g, int x, int y, int w, int h, int c1, int c2) {
        int steps = Math.max(1, Math.min(w, 64));
        for (int i = 0; i < steps; i++) {
            int x1 = x + w * i / steps, x2 = x + w * (i + 1) / steps;
            if (x2 > x1) g.fill(x1, y, x2, y + h, mix(c1, c2, (i + 0.5f) / steps));
        }
    }

    public static void text(GuiGraphics g, String s, int x, int y, int c) { g.drawString(font(), s, x, y, c, true); }
    public static void textNS(GuiGraphics g, String s, int x, int y, int c) { g.drawString(font(), s, x, y, c, false); }
    public static void ctext(GuiGraphics g, String s, int cx, int y, int c) { g.drawString(font(), s, cx - tw(s) / 2, y, c, true); }
    public static void rtext(GuiGraphics g, String s, int rx, int y, int c) { g.drawString(font(), s, rx - tw(s), y, c, true); }

    public static String fit(String s, int maxW) {
        if (tw(s) <= maxW) return s;
        while (s.length() > 1 && tw(s + "..") > maxW) s = s.substring(0, s.length() - 1);
        return s + "..";
    }

    public static void pushScale(GuiGraphics g, float x, float y, float s) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(s, s);
    }
    public static void pushScale2(GuiGraphics g, float x, float y, float sx, float sy) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(sx, sy);
    }
    public static void pop(GuiGraphics g) { g.pose().popMatrix(); }
}
