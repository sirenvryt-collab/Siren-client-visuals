package com.sirens.visuals.gui;

import com.sirens.visuals.cfg.Mods;
import com.sirens.visuals.fx.Emotes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class EmoteScreen extends Screen {
    public EmoteScreen() { super(Component.literal("Emotes")); }
    @Override public boolean isPauseScreen() { return false; }

    private int[] cell(int i) {
        int cols = 4, w = 90, h = 34, x0 = (width - cols * (w + 6)) / 2, y0 = height / 2 - 70;
        return new int[]{x0 + (i % cols) * (w + 6), y0 + (i / cols) * (h + 6), w, h};
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        UI.beginFrame();
        g.fill(0, 0, width, height, 0x66000000);
        UI.ctext(g, "EMOTES", width / 2, height / 2 - 92, UI.accent());
        for (int i = 0; i < Emotes.NAMES.length; i++) {
            int[] c = cell(i);
            boolean hv = UI.in(mx, my, c[0], c[1], c[2], c[3]);
            UI.rrect(g, c[0], c[1], c[2], c[3], 6, hv ? UI.alpha(UI.accent(), 0.6f) : 0xB00D1020);
            UI.ctext(g, Emotes.NAMES[i], c[0] + c[2] / 2, c[1] + 13, 0xFFFFFFFF);
        }
    }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean d) {
        for (int i = 0; i < Emotes.NAMES.length; i++) {
            int[] c = cell(i);
            if (UI.in(e.x(), e.y(), c[0], c[1], c[2], c[3])) { Emotes.play(i, Mods.EM_LEN.f()); onClose(); return true; }
        }
        return true;
    }
}
