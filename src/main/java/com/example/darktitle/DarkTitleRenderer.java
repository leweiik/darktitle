package com.example.darktitle;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Pitch-black, pixel-art title screen: stepped red flames, rising embers, and optional
 * stickers (your own PNGs) on the left and right, all snapped to one chunky pixel grid.
 *
 * Put your images in  <game folder>/config/darktitle/left.png  and  right.png
 * (if only left.png exists it is mirrored onto the right side).
 *
 * Stateless apart from the sticker cache: everything else is computed from the current time.
 */
public final class DarkTitleRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("darktitle");

    // ---- tweak these ----
    private static final int   PIXEL_ROWS         = 150;   // screen is split into this many pixel rows (lower = chunkier)
    private static final int   EMBERS             = 120;   // number of rising sparks
    private static final float EMBER_SPEED        = 1.0f;  // speed multiplier for the sparks
    private static final float FLAME_HEIGHT       = 1.0f;  // 1.0 = default, 1.5 = taller flames
    private static final int   STICKER_MAX_CELLS  = 64;    // longest side of a sticker, in pixels
    private static final boolean STICKER_RETRO_COLORS = true; // reduce sticker colours to a retro palette
    private static final boolean STICKER_BOB      = true;  // gentle up/down idle bob

    // flame colours, bottom -> top (ARGB)
    private static final int[] FLAME_BACK  = {0xFF7A0000, 0xFF5A0000, 0xFF3A0000, 0xFF200000};
    private static final int[] FLAME_FRONT = {0xFFFF6A00, 0xFFE02000, 0xFFA00000, 0xFF600000};

    private static boolean failed;

    private static final Sticker LEFT  = new Sticker("left.png");
    private static final Sticker RIGHT = new Sticker("right.png");

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
        int px = Math.max(1, Math.round(h / (float) PIXEL_ROWS));
        float t = (System.nanoTime() / 1_000_000L % 3_600_000L) / 1000f; // seconds, wraps hourly
        float tFlame = (float) Math.floor(t * 10f) / 10f;                // flames step at 10 fps
        float tEmber = (float) Math.floor(t * 15f) / 15f;                // embers step at 15 fps

        // 1. pitch black
        g.fill(0, 0, w, h, 0xFF000000);

        // 2. stepped dark-red glow at the bottom (hard bands, no smooth gradient)
        g.fill(0, (int) (h * 0.60f), w, h, 0xFF0C0000);
        g.fill(0, (int) (h * 0.72f), w, h, 0xFF160000);
        g.fill(0, (int) (h * 0.84f), w, h, 0xFF220000);

        // 3. stickers behind the flames
        long nowMs = System.nanoTime() / 1_000_000L;
        LEFT.refresh(nowMs);
        RIGHT.refresh(nowMs);
        drawStickers(g, w, h, px, t);

        // 4. two layers of pixel flames: wide dark ones at the back, thin bright ones in front
        flames(g, w, h, px, tFlame, 0, 2, 0.24f, 1.0f, FLAME_BACK);
        flames(g, w, h, px, tFlame, 1, 1, 0.15f, 1.7f, FLAME_FRONT);

        // 5. embers: square pixels that cool from orange to dark red as they rise
        for (int i = 0; i < EMBERS; i++) {
            float speed = (10f + hash01(i, 3) * 30f) * EMBER_SPEED;       // gui px / second
            float phase = hash01(i, 4) * 6.2831855f;

            float rise = (tEmber * speed + hash01(i, 2) * h) % h;        // 0 at bottom -> h at top
            float fade = 1f - rise / h;
            if (fade < 0.08f) continue;

            float blink = (float) Math.sin(tEmber * (4f + hash01(i, 5) * 6f) + phase);
            if (blink < -0.6f) continue;                                  // flicker out for a moment

            float sway = Math.round(Math.sin(tEmber * 1.3f + phase) * 2f) * px;
            int ix = ((int) (hash01(i, 1) * w + sway) / px) * px;
            int iy = ((int) (h - rise) / px) * px;

            int color = fade > 0.66f ? 0xFFFF6A00
                      : fade > 0.40f ? 0xFFFF2A00
                      : fade > 0.20f ? 0xFFB00000
                      :                0xFF600000;
            int size = hash01(i, 7) > 0.85f ? 2 * px : px;
            g.fill(ix, iy, ix + size, iy + size, color);
        }
    }

    // ------------------------------------------------------------------ flames

    /** One row of flame columns along the bottom edge; heights flow like waves over time. */
    private static void flames(GuiGraphics g, int w, int h, int px, float t, int layer,
                               int colPixels, float maxHeightFrac, float speed, int[] palette) {
        int colWidth = colPixels * px;
        int cols = w / colWidth + 1;
        int totalRows = h / px;
        float ts = t * speed;
        int step = (int) (t * 10f) & 255;
        for (int c = 0; c < cols; c++) {
            float n = 0.5f * (float) Math.sin(c * 0.45f + ts * 1.3f + layer * 2f)
                    + 0.3f * (float) Math.sin(c * 1.10f - ts * 2.7f)
                    + 0.2f * (float) Math.sin(c * 2.30f + ts * 4.1f);
            n = (n + 1f) * 0.5f;                                          // 0..1
            n += (hash01(c + layer * 977, step) - 0.5f) * 0.25f;          // per-step jitter: ragged tips
            n = Math.max(0f, Math.min(1f, n));

            int rows = (int) (totalRows * maxHeightFrac * FLAME_HEIGHT * (0.2f + 0.8f * n));
            if (rows <= 0) continue;

            int x = c * colWidth;
            int[] cut = {rows * 35 / 100, rows * 70 / 100, rows * 90 / 100, rows};
            int prev = 0;
            for (int b = 0; b < 4; b++) {
                if (cut[b] > prev) {
                    g.fill(x, h - cut[b] * px, x + colWidth, h - prev * px, palette[b]);
                    prev = cut[b];
                }
            }
        }
    }

    // ---------------------------------------------------------------- stickers

    private static void drawStickers(GuiGraphics g, int w, int h, int px, float t) {
        int bob = STICKER_BOB ? Math.round((float) Math.sin(t * 1.6f)) * px : 0;
        int margin = px * 4;

        if (LEFT.present) {
            LEFT.draw(g, margin, h - LEFT.gh * px - px * 3 + bob, px, false);
        }
        if (RIGHT.present) {
            RIGHT.draw(g, w - margin - RIGHT.gw * px, h - RIGHT.gh * px - px * 3 + bob, px, false);
        } else if (LEFT.present) { // no right.png: mirror the left one
            LEFT.draw(g, w - margin - LEFT.gw * px, h - LEFT.gh * px - px * 3 + bob, px, true);
        }
    }

    /** A PNG turned into a small grid of coloured pixels, drawn as horizontal runs of fills. */
    private static final class Sticker {
        private final String fileName;
        private long lastCheckMs;
        private boolean checkedOnce;
        private long lastModified = -1;
        private boolean folderReady;

        volatile boolean present;
        int gw, gh;
        private List<int[]> runs = new ArrayList<>(); // {x, y, widthInCells, argb}

        Sticker(String fileName) {
            this.fileName = fileName;
        }

        void refresh(long nowMs) {
            if (checkedOnce && nowMs - lastCheckMs < 3000) return; // look for changes every 3 seconds
            checkedOnce = true;
            lastCheckMs = nowMs;
            try {
                Path dir = FabricLoader.getInstance().getConfigDir().resolve("darktitle");
                if (!folderReady) {
                    Files.createDirectories(dir);
                    folderReady = true;
                }
                Path file = dir.resolve(fileName);
                if (!Files.isRegularFile(file)) {
                    present = false;
                    lastModified = -1;
                    return;
                }
                long mod = Files.getLastModifiedTime(file).toMillis();
                if (mod == lastModified) return;
                lastModified = mod; // set first so a broken file is not re-read every 3 s
                load(file);
            } catch (Throwable e) {
                present = false;
                LOGGER.warn("Could not load sticker {}: {}", fileName, e.toString());
            }
        }

        private void load(Path file) throws IOException {
            BufferedImage img = ImageIO.read(file.toFile());
            if (img == null) throw new IOException("not a readable image");

            int sw = img.getWidth();
            int sh = img.getHeight();
            float scale = (float) STICKER_MAX_CELLS / Math.max(sw, sh);
            int cw = Math.max(1, Math.round(sw * scale));
            int ch = Math.max(1, Math.round(sh * scale));

            int[][] cells = new int[ch][cw]; // 0 = transparent
            for (int cy = 0; cy < ch; cy++) {
                int y0 = cy * sh / ch;
                int y1 = Math.max(y0 + 1, (cy + 1) * sh / ch);
                for (int cx = 0; cx < cw; cx++) {
                    int x0 = cx * sw / cw;
                    int x1 = Math.max(x0 + 1, (cx + 1) * sw / cw);

                    long sa = 0, sr = 0, sg = 0, sb = 0;
                    int n = 0;
                    for (int y = y0; y < y1 && y < sh; y++) {
                        for (int x = x0; x < x1 && x < sw; x++) {
                            int argb = img.getRGB(x, y);
                            int a = argb >>> 24;
                            sa += a;
                            sr += (long) ((argb >> 16) & 0xFF) * a;
                            sg += (long) ((argb >> 8) & 0xFF) * a;
                            sb += (long) (argb & 0xFF) * a;
                            n++;
                        }
                    }
                    if (n == 0 || sa == 0 || sa / n < 128) continue;     // mostly transparent: skip

                    int r = (int) (sr / sa), gC = (int) (sg / sa), b = (int) (sb / sa);
                    if (STICKER_RETRO_COLORS) {
                        r = quantize(r);
                        gC = quantize(gC);
                        b = quantize(b);
                    }
                    cells[cy][cx] = 0xFF000000 | (r << 16) | (gC << 8) | b;
                }
            }

            // merge neighbouring cells of the same colour into single wide rectangles
            List<int[]> out = new ArrayList<>();
            for (int cy = 0; cy < ch; cy++) {
                int cx = 0;
                while (cx < cw) {
                    int color = cells[cy][cx];
                    if (color == 0) { cx++; continue; }
                    int start = cx;
                    while (cx < cw && cells[cy][cx] == color) cx++;
                    out.add(new int[] {start, cy, cx - start, color});
                }
            }

            this.gw = cw;
            this.gh = ch;
            this.runs = out;
            this.present = true;
            LOGGER.info("Loaded sticker {} ({}x{} cells, {} rectangles)", fileName, cw, ch, out.size());
        }

        void draw(GuiGraphics g, int ox, int oy, int px, boolean mirror) {
            for (int[] r : runs) {
                int cellX = mirror ? gw - (r[0] + r[2]) : r[0];
                int x = ox + cellX * px;
                int y = oy + r[1] * px;
                g.fill(x, y, x + r[2] * px, y + px, r[3]);
            }
        }

        private static int quantize(int v) {
            return Math.min(255, ((v + 16) >> 5) << 5);
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
