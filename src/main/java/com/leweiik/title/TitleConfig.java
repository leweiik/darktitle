package com.leweiik.title;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/** All user-adjustable values. Saved to  config/leweiiktitle.properties. */
public final class TitleConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("leweiiktitle");

    // ---- values (defaults are set in reset()) ----
    public static float   fireSpeed;     // 0.1 .. 4    how fast the flames move
    public static float   fireHeight;    // 0.3 .. 2.5  height multiplier of the bottom flames
    public static float   sideFire;      // 0   .. 2    width of the flames on the left/right edges (0 = off)
    public static int     embers;        // 0   .. 400  number of rising sparks
    public static float   emberSpeed;    // 0.1 .. 4    how fast the sparks rise
    public static int     pixelRows;     // 60  .. 300  higher = finer pixels, lower = chunkier
    public static float   glow;          // 0   .. 1    dark-red glow behind the flames
    public static int     stickerCells;  // 16  .. 128  size of your stickers, in pixels
    public static boolean enterScreen;   // show the "Press to Enter" start

    /** Not saved: becomes true once you pressed the button this session. */
    public static boolean entered;

    static {
        reset();
        load();
    }

    private TitleConfig() {}

    public static void reset() {
        fireSpeed    = 1.0f;
        fireHeight   = 1.0f;
        sideFire     = 1.0f;
        embers       = 120;
        emberSpeed   = 1.0f;
        pixelRows    = 150;
        glow         = 0.6f;
        stickerCells = 64;
        enterScreen  = true;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("leweiiktitle.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.isRegularFile(f)) return;
        try (InputStream in = Files.newInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            fireSpeed    = clamp(getF(p, "fireSpeed", fireSpeed), 0.1f, 4f);
            fireHeight   = clamp(getF(p, "fireHeight", fireHeight), 0.3f, 2.5f);
            sideFire     = clamp(getF(p, "sideFire", sideFire), 0f, 2f);
            embers       = Math.round(clamp(getF(p, "embers", embers), 0f, 400f));
            emberSpeed   = clamp(getF(p, "emberSpeed", emberSpeed), 0.1f, 4f);
            pixelRows    = Math.round(clamp(getF(p, "pixelRows", pixelRows), 60f, 300f));
            glow         = clamp(getF(p, "glow", glow), 0f, 1f);
            stickerCells = Math.round(clamp(getF(p, "stickerCells", stickerCells), 16f, 128f));
            enterScreen  = Boolean.parseBoolean(p.getProperty("enterScreen", String.valueOf(enterScreen)));
        } catch (Throwable e) {
            LOGGER.warn("Could not read {}: {}", f, e.toString());
        }
    }

    public static void save() {
        Path f = file();
        try {
            Files.createDirectories(f.getParent());
            Properties p = new Properties();
            p.setProperty("fireSpeed", fmt(fireSpeed));
            p.setProperty("fireHeight", fmt(fireHeight));
            p.setProperty("sideFire", fmt(sideFire));
            p.setProperty("embers", String.valueOf(embers));
            p.setProperty("emberSpeed", fmt(emberSpeed));
            p.setProperty("pixelRows", String.valueOf(pixelRows));
            p.setProperty("glow", fmt(glow));
            p.setProperty("stickerCells", String.valueOf(stickerCells));
            p.setProperty("enterScreen", String.valueOf(enterScreen));
            try (OutputStream out = Files.newOutputStream(f)) {
                p.store(out, "Leweiik Title settings (you can also change these in-game)");
            }
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", f, e.toString());
        }
    }

    private static float getF(Properties p, String key, float fallback) {
        try {
            return Float.parseFloat(p.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String fmt(float v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
