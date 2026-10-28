package com.leweiik.title;

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
 * Pitch-black pixel-art title screen: layered stepped flames (yellow core, orange body,
 * red tips), fire walls on the left and right edges, rising embers, and optional stickers
 * (your own PNGs). Everything snaps to one chunky pixel grid.
 *
 * Stickers: put left.png / right.png in  config/leweiiktitle/  (left.png is mirrored onto the
 * right if right.png is missing). All other values live in TitleConfig and the in-game settings.
 */
public final class TitleRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("leweiiktitle");

    private static final boolean STICKER_RETRO_COLORS = true; // reduce sticker colours to a retro palette
    private static final boolean STICKER_BOB          = true; // gentle up/down idle bob

    // Flame colours, bottom -> top (ARGB). Back layers are dark and tall, front layers bright and short.
    private static final int[] FLAME_BACK  = {0xFFC83A00, 0xFF9A1800, 0xFF6A0C00, 0xFF3A0600};
    private static final int[] FLAME_MID   = {0xFFFF9A00, 0xFFF06000, 0xFFC02800, 0xFF801000};
    private static final int[] FLAME_FRONT = {0xFFFFE860, 0xFFFFC020, 0xFFFF8000, 0xFFE04000};
    // Side fire, edge -> inwards
    private static final int[] SIDE_BACK   = {0xFFA02000, 0xFF701400, 0xFF420A00, 0xFF220400};
    private static final int[] SIDE_FRONT  = {0xFFFF7000, 0xFFD03000, 0xFF801000, 0xFF400800};

    private static boolean failed;
    private static long lastNanos;
    private static double flamePhase;
    private static double emberPhase;

    private static final Sticker LEFT  = new Sticker("left.png");
    private static final Sticker RIGHT = new Sticker("right.png");

    private TitleRenderer() {}

    public static void render(GuiGraphics g) {
        if (failed) return;
        try {
            draw(g);
        } catch (Throwable t) {
            // Never crash the game over a cosmetic effect: log once and stop drawing.
            failed = true;
            LOGGER.error("Leweiik Title effect disabled after an error", t);
        }
    }

    private static void draw(GuiGraphics g) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        int px = Math.max(1, Math.round(h / (float) TitleConfig.pixelRows));

        // animation clocks advance by (real time x speed setting), so changing a slider never makes a jump
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : Math.min(0.1, (now - lastNanos) / 1e9);
        lastNanos = now;
        flamePhase = (flamePhase + dt * TitleConfig.fireSpeed) % 3600.0;
        emberPhase = (emberPhase + dt * TitleConfig.emberSpeed) % 3600.0;
        float tFlame = (float) (Math.floor(flamePhase * 10.0) / 10.0);   // flames step at 10 fps
        float tEmber = (float) (Math.floor(emberPhase * 15.0) / 15.0);   // embers step at 15 fps
        float tReal  = (now / 1_000_000L % 3_600_000L) / 1000f;

        // 1. pitch black
        g.fill(0, 0, w, h, 0xFF000000);

        // 2. stepped dark-red glow at the bottom (hard bands, no smooth gradient)
        float gl = TitleConfig.glow;
        if (gl > 0f) {
            g.fill(0, (int) (h * 0.55f), w, h, red(0x16, gl));
            g.fill(0, (int) (h * 0.68f), w, h, red(0x2A, gl));
            g.fill(0, (int) (h * 0.82f), w, h, red(0x44, gl));
        }

        // 3. fire walls on the left and right edges
        if (TitleConfig.sideFire > 0f) {
            sideFlames(g, w, h, px, tFlame, 0, 0.16f, 0.9f, SIDE_BACK);
            sideFlames(g, w, h, px, tFlame, 1, 0.09f, 1.5f, SIDE_FRONT);
        }

        // 4. back and middle flame layers
        flames(g, w, h, px, tFlame, 0, 3, 0.42f, 0.10f, 0.8f, FLAME_BACK);
        flames(g, w, h, px, tFlame, 1, 2, 0.30f, 0.17f, 1.2f, FLAME_MID);

        // 5. stickers stand in front of those, behind the front flames
        long nowMs = now / 1_000_000L;
        LEFT.refresh(nowMs);
        RIGHT.refresh(nowMs);
        drawStickers(g, w, h, px, tReal);

        // 6. front flame layer: bright yellow core, orange body, red tips
        flames(g, w, h, px, tFlame, 2, 2, 0.20f, 0.26f, 1.7f, FLAME_FRONT);

        // 7. embers: square pixels that cool from yellow to dark red as they rise
        int count = TitleConfig.embers;
        for (int i = 0; i < count; i++) {
            float speed = 10f + hash01(i, 3) * 30f;                       // gui px / second (x ember speed setting)
            float phase = hash01(i, 4) * 6.2831855f;

            float rise = (tEmber * speed + hash01(i, 2) * h) % h;        // 0 at bottom -> h at top
            float fade = 1f - rise / h;
            if (fade < 0.08f) continue;

            float blink = (float) Math.sin(tEmber * (4f + hash01(i, 5) * 6f) + phase);
            if (blink < -0.6f) continue;                                  // flicker out for a moment

            float sway = Math.round(Math.sin(tEmber * 1.3f + phase) * 2f) * px;
            int ix = ((int) (hash01(i, 1) * w + sway) / px) * px;
            int iy = ((int) (h - rise) / px) * px;

            int color = fade > 0.66f ? 0xFFFFD43A
                      : fade > 0.40f ? 0xFFFF8A00
                      : fade > 0.20f ? 0xFFE03000
                      :                0xFF8A1000;
            int size = hash01(i, 7) > 0.85f ? 2 * px : px;
            g.fill(ix, iy, ix + size, iy + size, color);
        }
    }

    private static int red(int max, float amount) {
        return 0xFF000000 | (Math.round(max * amount) << 16);
    }

    // ------------------------------------------------------------------ flames

    /** One row of flame columns along the bottom edge. Peaks are pointed, valleys rounded, and they drift sideways. */
    private static void flames(GuiGraphics g, int w, int h, int px, float t, int layer, int colPixels,
                               float maxHeightFrac, float freq, float speed, int[] palette) {
        int colWidth = colPixels * px;
        int cols = w / colWidth + 1;
        int totalRows = h / px;
        float ts = t * speed;
        int step = (int) (t * 10f) & 255;
        for (int c = 0; c < cols; c++) {
            float a = 1f - Math.abs((float) Math.sin(c * freq + ts * 0.9f + layer * 1.3f));
            float b = 1f - Math.abs((float) Math.sin(c * freq * 2.3f - ts * 1.4f + layer * 1.7f));
            float low = 0.5f + 0.5f * (float) Math.sin(c * 0.045f + ts * 0.35f + layer);
            float n = 0.50f * a + 0.28f * b + 0.22f * low;
            n += (hash01(c + layer * 977, step) - 0.5f) * 0.18f;          // per-step jitter: ragged tips
            n = Math.max(0f, Math.min(1f, n));

            int rows = (int) (totalRows * maxHeightFrac * TitleConfig.fireHeight * (0.12f + 0.88f * n));
            if (rows <= 0) continue;

            int x = c * colWidth;
            int[] cut = {rows * 30 / 100, rows * 60 / 100, rows * 85 / 100, rows};
            int prev = 0;
            for (int k = 0; k < 4; k++) {
                if (cut[k] > prev) {
                    g.fill(x, h - cut[k] * px, x + colWidth, h - prev * px, palette[k]);
                    prev = cut[k];
                }
            }
        }
    }

    /** Fire walls along both side edges: widest at the bottom, licking upwards. */
    private static void sideFlames(GuiGraphics g, int w, int h, int px, float t, int layer,
                                   float maxWidthFrac, float speed, int[] palette) {
        int totalRows = h / px;
        float ts = t * speed;
        int step = (int) (t * 10f) & 255;
        for (int r = 0; r < totalRows; r++) {
            float a = 1f - Math.abs((float) Math.sin(r * 0.14f + ts * 1.1f + layer * 2.1f));
            float b = 1f - Math.abs((float) Math.sin(r * 0.33f + ts * 1.9f + layer));
            float n = 0.6f * a + 0.4f * b;
            n += (hash01(r + layer * 811, step) - 0.5f) * 0.15f;
            n = Math.max(0f, Math.min(1f, n));

            float taper = 0.25f + 0.75f * ((float) r / totalRows);        // narrower towards the top
            int cols = (int) (w * maxWidthFrac * TitleConfig.sideFire * taper * (0.15f + 0.85f * n)) / px;
            if (cols <= 0) continue;

            int y = r * px;
            int[] cut = {cols * 30 / 100, cols * 60 / 100, cols * 85 / 100, cols};
            int prev = 0;
            for (int k = 0; k < 4; k++) {
                if (cut[k] > prev) {
                    g.fill(prev * px, y, cut[k] * px, y + px, palette[k]);                 // left edge
                    g.fill(w - cut[k] * px, y, w - prev * px, y + px, palette[k]);         // right edge
                    prev = cut[k];
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
        private int loadedCells = -1;
        private boolean folderReady;

        volatile boolean present;
        int gw, gh;
        private List<int[]> runs = new ArrayList<>(); // {x, y, widthInCells, argb}

        Sticker(String fileName) {
            this.fileName = fileName;
        }

        void refresh(long nowMs) {
            boolean sizeChanged = present && loadedCells != TitleConfig.stickerCells;
            if (!sizeChanged && checkedOnce && nowMs - lastCheckMs < 3000) return; // look for changes every 3 s
            checkedOnce = true;
            lastCheckMs = nowMs;
            try {
                Path dir = FabricLoader.getInstance().getConfigDir().resolve("leweiiktitle");
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
                if (mod == lastModified && !sizeChanged) return;
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

            int maxCells = TitleConfig.stickerCells;
            int sw = img.getWidth();
            int sh = img.getHeight();
            float scale = (float) maxCells / Math.max(sw, sh);
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
            this.loadedCells = maxCells;
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
