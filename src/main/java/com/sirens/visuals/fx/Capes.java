package com.sirens.visuals.fx;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;

/** Generates procedural 64x32 cape textures (the cape back face occupies x 1..10, y 1..16 of the sheet). */
public final class Capes {
    private Capes() { }

    public static byte[] generate(int pat, int c1, int c2) throws Exception {
        BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        int W = 64, H = 32;
        Random rnd = new Random(1234);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                float u = x / (float) (W - 1), v = y / (float) (H - 1);
                int c = switch (pat) {
                    case 0 -> c1;
                    case 1 -> (x / 2) % 2 == 0 ? c1 : c2;
                    case 2 -> (y / 2) % 2 == 0 ? c1 : c2;
                    case 3 -> Fx.lerpRgb(c1, c2, v);
                    case 4 -> ((x + y) / 3) % 2 == 0 ? c1 : c2;
                    case 5 -> ((x / 2) + (y / 2)) % 2 == 0 ? c1 : c2;
                    case 6 -> { // galaxy
                        int base = Fx.lerpRgb(c1, 0x05030F, 0.55f + 0.3f * (float) Math.sin(u * 5 + v * 3));
                        if (rnd.nextInt(28) == 0) base = Fx.lerpRgb(base, c2, 0.9f);
                        yield Fx.lerpRgb(base, c2, 0.25f * (float) Math.max(0, Math.sin(u * 8 - v * 6)));
                    }
                    case 7 -> { // flames
                        float f = v + 0.18f * (float) Math.sin(u * 40) * (1 - v) ;
                        yield Fx.lerpRgb(c2, c1, Math.max(0, Math.min(1, f * 1.3f)));
                    }
                    default -> Fx.lerpRgb(c1, c2, 0.5f + 0.5f * (float) Math.sin(u * 30 + v * 10));
                };
                img.setRGB(x, y, 0xFF000000 | c);
            }
        // darker border on the cape face for definition
        for (int x = 1; x <= 10; x++) { img.setRGB(x, 1, shade(img.getRGB(x, 1))); img.setRGB(x, 16, shade(img.getRGB(x, 16))); }
        for (int y = 1; y <= 16; y++) { img.setRGB(1, y, shade(img.getRGB(1, y))); img.setRGB(10, y, shade(img.getRGB(10, y))); }
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", o);
        return o.toByteArray();
    }

    static int shade(int argb) {
        int r = ((argb >> 16) & 255) * 3 / 4, g = ((argb >> 8) & 255) * 3 / 4, b = (argb & 255) * 3 / 4;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
