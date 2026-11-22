package com.sirens.visuals.hud;

import com.sirens.visuals.cfg.Mods;
import com.sirens.visuals.gui.UI;

public final class Style {
    private Style() { }
    public static int text() { return 0xFF000000 | Mods.HS_TEXT.rgb(); }
    public static int accent() { return 0xFF000000 | Mods.HS_ACCENT.rgb(); }
    public static boolean shadow() { return Mods.HS_SHADOW.on(); }
    public static int bg() { return UI.argb((int) (Mods.HS_BGA.f() * 255), Mods.HS_BGC.rgb()); }
    public static int dim() { return UI.alpha(text(), 0.65f); }
}
