package com.example.darktitle;

import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stateless renderer: every particle position is computed from the current time and a
 * deterministic hash of its index, so there is nothing to tick, store, or reset.
 */
public final class DarkTitleRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("darktitle");

    // ---- tweak these ----
    private static final int   DARKNESS  = 0xCC;  // 0-255, how strongly the panorama is darkened
    private static final int   PARTICLES = 110;   // number of white specks
    private static final float DRIFT     = 1.0f;  // speed multiplier for the specks

    private static boolean failed;

    private DarkTitleRenderer() {}

    public static void render(GuiGraphics g) {
        if (failed) return;
        try {
            draw(g);
        } catch (Throwable t) {
            // Never crash the game over a cosmetic effect: log once and stop drawing.
            failed = true;
            LOGGER.error("Dark title effect disabled after an error", t);
        }
    }

    private static void draw(GuiGraphics g) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        float t = (System.nanoTime() / 1_000_000L % 3_600_000L) / 1000f; // seconds, wraps hourly

        // 1. dark base over the panorama
        g.fill(0, 0, w, h, DARKNESS << 24);

        // 2. soft white glow rising from the bottom, slowly pulsing
        int glow = 0x26 + (int) (0x10 * Math.sin(t * 0.6));
        g.fillGradient(0, h / 2, w, h, 0x00FFFFFF, (glow << 24) | 0xFFFFFF);

        // 3. darker top so the logo area stays moody
        g.fillGradient(0, 0, w, h / 3, 0xA0000000, 0x00000000);

        // 4. drifting, twinkling white specks
        for (int i = 0; i < PARTICLES; i++) {
            float speed = (3f + hash01(i, 3) * 9f) * DRIFT;          // gui px / second
            float phase = hash01(i, 4) * 6.2831855f;

            float y = ((hash01(i, 2) * h - t * speed) % h + h) % h;   // drifts upward, wraps
            float x = hash01(i, 1) * w + (float) Math.sin(t * 0.4f + phase) * 10f;

            float twinkle = 0.5f + 0.5f * (float) Math.sin(t * (0.8f + hash01(i, 5) * 1.6f) + phase);
            int alpha = (int) ((0.25f + 0.75f * twinkle) * (70 + 150 * hash01(i, 6)));
            int size = hash01(i, 7) > 0.85f ? 2 : 1;

            int ix = (int) x;
            int iy = (int) y;

            if (size == 2) { // faint halo around the bigger ones
                g.fill(ix - 1, iy - 1, ix + size + 1, iy + size + 1, ((alpha / 5) << 24) | 0xFFFFFF);
            }
            g.fill(ix, iy, ix + size, iy + size, (alpha << 24) | 0xFFFFFF);
        }
    }

    /** Deterministic pseudo-random float in [0, 1) from (index, salt). */
    private static float hash01(int i, int salt) {
        int n = i * 374761393 + salt * 668265263;
        n = (n ^ (n >>> 13)) * 1274126177;
        n ^= n >>> 16;
        return (n & 0xFFFFFF) / (float) 0x1000000;
    }
}
