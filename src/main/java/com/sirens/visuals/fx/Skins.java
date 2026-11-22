package com.sirens.visuals.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.sirens.visuals.Hooks;
import com.sirens.visuals.Net;
import com.sirens.visuals.cfg.Config;
import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Skin / cape overriding for the local player. Everything here is client-side only (others do not see it). */
public final class Skins {
    private Skins() { }

    public static volatile Identifier skinId, capeId;
    public static volatile Boolean slim;
    public static String status = "";
    private static int counter;

    public static Path skinsDir() { Path p = Config.dir().resolve("skins"); try { Files.createDirectories(p); } catch (Exception ignored) { } return p; }
    public static Path capesDir() { Path p = Config.dir().resolve("capes"); try { Files.createDirectories(p); } catch (Exception ignored) { } return p; }

    public static void scan() {
        Mods.SK_FILE.setOpts(list(skinsDir()));
        Mods.C_FILE.setOpts(list(capesDir()));
    }

    private static String[] list(Path d) {
        List<String> l = new ArrayList<>();
        try (var s = Files.list(d)) { s.filter(f -> f.toString().toLowerCase().endsWith(".png")).forEach(f -> l.add(f.getFileName().toString())); } catch (Exception ignored) { }
        if (l.isEmpty()) l.add("(none)");
        return l.toArray(new String[0]);
    }

    // ------------------------------------------------------------ texture registration
    static Identifier register(byte[] png, String key) throws Exception {
        NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
        DynamicTexture tex = null;
        for (Constructor<?> c : DynamicTexture.class.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 1 && p[0] == NativeImage.class) tex = (DynamicTexture) c.newInstance(img);
            else if (p.length == 2 && p[0] == java.util.function.Supplier.class && p[1] == NativeImage.class)
                tex = (DynamicTexture) c.newInstance((java.util.function.Supplier<String>) () -> key, img);
            if (tex != null) break;
        }
        if (tex == null) throw new IllegalStateException("DynamicTexture constructor not found");
        Identifier id = Identifier.fromNamespaceAndPath("sirensvisuals", key + "_" + (counter++));
        Minecraft.getInstance().getTextureManager().register(id, tex);
        return id;
    }

    static void registerAs(byte[] png, Identifier id) throws Exception {
        NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
        DynamicTexture tex = null;
        for (Constructor<?> c : DynamicTexture.class.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 1 && p[0] == NativeImage.class) tex = (DynamicTexture) c.newInstance(img);
            else if (p.length == 2 && p[0] == java.util.function.Supplier.class && p[1] == NativeImage.class)
                tex = (DynamicTexture) c.newInstance((java.util.function.Supplier<String>) () -> id.toString(), img);
            if (tex != null) break;
        }
        if (tex == null) throw new IllegalStateException("DynamicTexture constructor not found");
        Minecraft.getInstance().getTextureManager().register(id, tex);
    }

    // ------------------------------------------------------------ sources
    record Profile(String skinUrl, boolean slim, String capeUrl) { }

    static Profile profile(String name) throws Exception {
        JsonObject o = JsonParser.parseString(Net.text("https://api.mojang.com/users/profiles/minecraft/" + Net.enc(name))).getAsJsonObject();
        String id = o.get("id").getAsString();
        JsonObject pr = JsonParser.parseString(Net.text("https://sessionserver.mojang.com/session/minecraft/profile/" + id)).getAsJsonObject();
        JsonArray props = pr.getAsJsonArray("properties");
        for (int i = 0; i < props.size(); i++) {
            JsonObject p = props.get(i).getAsJsonObject();
            if (!p.get("name").getAsString().equals("textures")) continue;
            JsonObject t = JsonParser.parseString(new String(Base64.getDecoder().decode(p.get("value").getAsString()))).getAsJsonObject().getAsJsonObject("textures");
            String s = null, c = null; boolean sl = false;
            if (t.has("SKIN")) { JsonObject sk = t.getAsJsonObject("SKIN"); s = sk.get("url").getAsString(); sl = sk.has("metadata") && sk.getAsJsonObject("metadata").has("model"); }
            if (t.has("CAPE")) c = t.getAsJsonObject("CAPE").get("url").getAsString();
            return new Profile(s, sl, c);
        }
        throw new IllegalStateException("no textures");
    }

    public static void applySkin() {
        status = "Loading skin...";
        Thread t = new Thread(() -> {
            try {
                byte[] png; Boolean sl = null;
                switch (Mods.SK_SRC.idx()) {
                    case 0 -> png = Files.readAllBytes(skinsDir().resolve(Mods.SK_FILE.value()));
                    case 1 -> { Profile p = profile(Mods.SK_NAME.get().trim()); if (p.skinUrl() == null) throw new IllegalStateException("player has no skin"); png = Net.bytes(p.skinUrl()); sl = p.slim(); }
                    default -> png = Net.bytes(Mods.SK_URL.get().trim());
                }
                png = fixSkin(png);
                byte[] fin = png; Boolean fsl = sl;
                Minecraft.getInstance().execute(() -> {
                    try {
                        skinId = register(fin, "skin");
                        int m = Mods.SK_MODEL.idx();
                        slim = m == 1 ? Boolean.FALSE : m == 2 ? Boolean.TRUE : fsl;
                        Mods.SKIN.enabled.set(true);
                        status = "Skin applied.";
                    } catch (Exception e) { status = "Skin failed: " + e.getMessage(); }
                });
            } catch (Exception e) { status = "Skin failed: " + e; }
        }, "sirens-skin");
        t.setDaemon(true); t.start();
    }

    /** Converts legacy 64x32 skins to 64x64 so the model renders correctly. */
    static byte[] fixSkin(byte[] png) throws Exception {
        BufferedImage in = ImageIO.read(new ByteArrayInputStream(png));
        if (in == null) throw new IllegalStateException("not a PNG");
        if (in.getWidth() == 64 && in.getHeight() == 64) return png;
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(in, 0, 0, null);
        if (in.getHeight() == 32) { // mirror legs/arms to the left side
            g.drawImage(in.getSubimage(0, 16, 16, 16), 16, 48, null);
            g.drawImage(in.getSubimage(40, 16, 16, 16), 32, 48, null);
        }
        g.dispose();
        return toPng(out);
    }

    static byte[] toPng(BufferedImage b) throws Exception { ByteArrayOutputStream o = new ByteArrayOutputStream(); ImageIO.write(b, "png", o); return o.toByteArray(); }

    public static void applyCape() {
        status = "Loading cape...";
        Thread t = new Thread(() -> {
            try {
                byte[] png;
                switch (Mods.C_SRC.idx()) {
                    case 0 -> png = Capes.generate(Mods.C_PAT.idx(), Mods.C_C1.rgb(), Mods.C_C2.rgb());
                    case 1 -> png = Files.readAllBytes(capesDir().resolve(Mods.C_FILE.value()));
                    case 2 -> { Profile p = profile(Mods.C_NAME.get().trim()); if (p.capeUrl() == null) throw new IllegalStateException("player has no cape"); png = Net.bytes(p.capeUrl()); }
                    default -> png = Net.bytes(Mods.C_URL.get().trim());
                }
                byte[] fin = png;
                Minecraft.getInstance().execute(() -> {
                    try { capeId = register(fin, "cape"); Mods.CAPE.enabled.set(true); status = "Cape applied."; }
                    catch (Exception e) { status = "Cape failed: " + e.getMessage(); }
                });
            } catch (Exception e) { status = "Cape failed: " + e; }
        }, "sirens-cape");
        t.setDaemon(true); t.start();
    }

    // ------------------------------------------------------------ mixin entry point
    public static PlayerSkin modify(PlayerSkin base) {
        try {
            boolean s = Mods.SKIN.on() && skinId != null, c = Mods.CAPE.on() && capeId != null;
            if (!s && !c) return base;
            Hooks.hit("skin");
            RecordComponent[] rc = PlayerSkin.class.getRecordComponents();
            Object[] args = new Object[rc.length];
            Class<?>[] types = new Class<?>[rc.length];
            for (int i = 0; i < rc.length; i++) {
                types[i] = rc[i].getType();
                Object v = rc[i].getAccessor().invoke(base);
                String n = rc[i].getName().toLowerCase();
                if (s && (n.equals("texture") || n.equals("body"))) v = make(rc[i].getType(), skinId, v);
                else if (c && n.equals("cape")) v = make(rc[i].getType(), capeId, v);
                else if (c && n.equals("elytra") && Mods.C_ELYTRA.on()) v = make(rc[i].getType(), capeId, v);
                else if (s && n.equals("model") && slim != null) v = model(rc[i].getType(), slim, v);
                args[i] = v;
            }
            Constructor<PlayerSkin> k = PlayerSkin.class.getDeclaredConstructor(types);
            k.setAccessible(true);
            return k.newInstance(args);
        } catch (Throwable t) {
            return base;
        }
    }

    private static Object make(Class<?> type, Identifier id, Object old) throws Exception {
        if (type == Identifier.class) return id;
        for (Constructor<?> c : type.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            c.setAccessible(true);
            if (p.length == 1 && p[0] == Identifier.class) return c.newInstance(id);
            if (p.length == 2 && p[0] == Identifier.class && p[1] == Identifier.class) return c.newInstance(id, id);
        }
        return old;
    }

    private static Object model(Class<?> type, boolean slim, Object old) {
        if (!type.isEnum()) return old;
        for (Object o : type.getEnumConstants()) if (o.toString().toUpperCase().contains(slim ? "SLIM" : "WIDE")) return o;
        return old;
    }
}
