package org.telegram.ui.Stars;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.core.math.MathUtils;

public final class StarGiftPatterns2 {

    private StarGiftPatterns2() {
    }

    private static final float PATTERN_SCALE = 1f;
    private static final float VERTICAL_RATIO = 0.70f;

    /**
     * Per-icon group index (0,1,2) — configure order of collapse
     */
    private static final int[] ICON_GROUP = {
            0, 1, 0, 0, 1, 0,
            1, 0, 0, 1, 1, 2,
            2, 2, 1, 2, 2, 2,
    };

    /**
     * Group-specific timing: when collapse starts and how long it lasts
     */
    private static final float[] GROUP_DELAY = {0.50f, 0.53f, 0.55f}; // start offset
    private static final float[] GROUP_DURATION = {0.40f, 0.45f, 0.50f}; // fade‑in window
    private static final float[][] ORBIT_18 = buildOrbit();

    private static float[][] buildOrbit() {
        final float R_OUT = 128f, R_IN = 92f;
        final float SIZE_OUT = 16f, SIZE_IN = 20f;
        final float ALPHA_OUT = .20f, ALPHA_IN = .50f;

        double[] degOuter = {-180, -144, -108, -72, -36, 0, 36, 72, 108, 144};
        double[] degInner = {-180, -135, -90, -45, 0, 45, 90, 135};

        float[][] pts = new float[18][4];
        int k = 0;

        for (double d : degOuter) {
            double rad = Math.toRadians(d);
            pts[k++] = new float[]{
                    (float) (R_OUT * Math.cos(rad)),
                    (float) (R_OUT * Math.sin(rad) * VERTICAL_RATIO),
                    SIZE_OUT,
                    ALPHA_OUT
            };
        }
        for (double d : degInner) {
            double rad = Math.toRadians(d);
            pts[k++] = new float[]{
                    (float) (R_IN * Math.cos(rad)),
                    (float) (R_IN * Math.sin(rad) * VERTICAL_RATIO),
                    SIZE_IN,
                    ALPHA_IN
            };
        }
        return pts;
    }

    private static final float MIN_R, MAX_R;

    /* ─── Anchor – fixed once per pattern instance ─── */
    private static float ANCHOR_CX = Float.NaN;
    private static float ANCHOR_CY = Float.NaN;
    /** Call when you need to recalc anchor (e.g. on profile reopen) */
    public static void resetAnchor() {
        ANCHOR_CX = Float.NaN;
        ANCHOR_CY = Float.NaN;
    }

    static {
        float min = Float.MAX_VALUE, max = 0;
        for (float[] p : ORBIT_18) {
            float r = (float) Math.hypot(p[0], p[1]);
            min = Math.min(min, r);
            max = Math.max(max, r);
        }
        MIN_R = min;
        MAX_R = max;
    }

    public static void drawOrbitIcons(
            Canvas canvas,
            Drawable icon,
            float cx,
            float cy,
            float progress
    ) {

        progress = MathUtils.clamp(progress, 0f, 1f);
        if (progress >= 1) {
            resetAnchor();
        }

        // Freeze initial anchor (first call) so starting positions stay constant
        if (Float.isNaN(ANCHOR_CX) || Float.isNaN(ANCHOR_CY)) {
            ANCHOR_CX = cx;
            ANCHOR_CY = cy;
        }

        for (int i = 0; i < ORBIT_18.length; i++) {
            float[] p = ORBIT_18[i];
            final float tx = p[0], ty = p[1];
            final float sizeDp = p[2];
            final float baseA = p[3];

            // group‑based timing
            int g = ICON_GROUP[i];
            float baseDelay = GROUP_DELAY[g];
            float duration = GROUP_DURATION[g];

            float lp = (progress - baseDelay) / duration;      // 0…1 inside its window
            lp = MathUtils.clamp(lp, 0f, 1f);
            lp = 1f - (float) Math.pow(1f - lp, 3);            // ease‑out‑cubic

            float offsetX = dpf2(tx * PATTERN_SCALE);
            float offsetY = dpf2(ty * PATTERN_SCALE);

            /* start position is fixed relative to the anchor */
            float startX = ANCHOR_CX + offsetX;
            float startY = ANCHOR_CY + offsetY;

            /* interpolate from current avatar center → fixed start */
            float x = cx + (startX - cx) * lp;
            float y = cy + (startY - cy) * lp;

            float sz = dp(sizeDp * (0.5f + 0.5f * lp));

            int l = Math.round(x - sz / 2f);
            int t = Math.round(y - sz / 2f);
            icon.setBounds(l, t, l + Math.round(sz), t + Math.round(sz));
            icon.setAlpha(Math.round(255 * (baseA * (0.5f + 0.5f * lp))));

            icon.draw(canvas);
        }
    }
}