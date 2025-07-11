package org.telegram.ui.Stars;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.core.math.MathUtils;

import org.telegram.messenger.AndroidUtilities;

import java.util.List;

public final class StarGiftPatterns2 {

    private StarGiftPatterns2() {
    }

    private static final float PATTERN_SCALE = 1f;
    private static final float VERTICAL_RATIO = 0.70f;

    private static final int[] ICON_GROUP = {
            0, 1, 0, 0, 1, 0,
            1, 0, 0, 1, 1, 2,
            2, 2, 1, 2, 2, 2,
    };

    private static final float[] GROUP_DELAY = {0.3f, 0.33f, 0.35f};
    private static final float[] GROUP_DURATION = {0.40f, 0.45f, 0.50f};
    private static final float[][] ORBIT_18 = buildOrbit();

    private static float[][] buildOrbit() {
        final float R_OUT = 128f, R_IN = 92f;
        final float SIZE_OUT = 16f, SIZE_IN = 20f;
        final float ALPHA_OUT = .20f, ALPHA_IN = .35f;

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

    private static float ANCHOR_CX = Float.NaN;
    private static float ANCHOR_CY = Float.NaN;
    public static void resetAnchor() {
        ANCHOR_CX = Float.NaN;
        ANCHOR_CY = Float.NaN;
    }

    public static void drawOrbitIcons(
            Canvas canvas,
            Drawable icon,
            float cx,
            float cy,
            float progress
    ) {
        if (Float.isNaN(ANCHOR_CX) || Float.isNaN(ANCHOR_CY)) {
            ANCHOR_CX = canvas.getWidth() / 2f;
            ANCHOR_CY = cy;
        }

        for (int i = 0; i < ORBIT_18.length; i++) {
            float[] p = ORBIT_18[i];
            final float tx = p[0], ty = p[1];
            final float sizeDp = p[2];
            final float baseA = p[3];

            int g = ICON_GROUP[i];
            float baseDelay = GROUP_DELAY[g];
            float duration = GROUP_DURATION[g];

            float lp = (progress - baseDelay) / duration;
            lp = MathUtils.clamp(lp, 0f, 1f);
            lp = 1f - (float) Math.pow(1f - lp, 3);
            if (lp <= 0f) {
                continue;
            }

            float offsetX = dpf2(tx * PATTERN_SCALE);
            float offsetY = dpf2(ty * PATTERN_SCALE);

            float startX = ANCHOR_CX + offsetX;
            float startY = ANCHOR_CY + offsetY;

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

    private static final int[] GIFT_MASK_18 = {
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 1,
            0, 1, 1, 1, 0, 1
    };

    private static final int[] GIFT_GROUP = {
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 2,
            0, 1, 0, 2, 0, 1
    };

    private static final float[] GIFT_DELAY = {0.30f, 0.33f, 0.45f};
    private static final float[] GIFT_DURATION = {0.40f, 0.45f, 0.50f};


    private static final float GIFT_PATTERN_SCALE = 1.2f;   // глобальный масштаб орбиты
    private static final float GIFT_HORIZONTAL_RATIO = 1.0f;  // «растяжение» по X
    private static final float GIFT_VERTICAL_RATIO = 1f;  // «сплющивание» по Y
    private static final float GIFT_SIZE_MULT = 1.1f;  // итоговый размер (1.0 = как emoji)

    public static void drawOrbitGifts(
            Canvas canvas,
            List<ProfileGiftsView.Gift> gifts,  // упорядоченный список 0…N
            float cx, float cy,                 // центр притяжения (тот же, что для emoji)
            float progress                      // общий прогресс анимации 0…1
    ) {
        if (Float.isNaN(ANCHOR_CX) || Float.isNaN(ANCHOR_CY)) {
            ANCHOR_CX = canvas.getWidth() / 2f;
            ANCHOR_CY = cy;
        }

        int giftIndex = 0;
        for (int i = 0; i < ORBIT_18.length; i++) {
            if (GIFT_MASK_18[i] == 0) continue;
            if (giftIndex >= gifts.size()) break;

            ProfileGiftsView.Gift gift = gifts.get(giftIndex++);

            float[] p = ORBIT_18[i];
            float tx = p[0], ty = p[1];
            float baseAlpha = p[3];

            int g = GIFT_GROUP[i];
            float lp = (progress - GIFT_DELAY[g]) / GIFT_DURATION[g];
            lp = MathUtils.clamp(lp, 0f, 1f);
            lp = 1f - (float) Math.pow(1f - lp, 3);   // ease‑out‑cubic
            if (lp <= 0f) {
                continue;
            }
            
            float offsetX = dpf2(tx * GIFT_PATTERN_SCALE * GIFT_HORIZONTAL_RATIO);
            float offsetY = dpf2(ty * GIFT_PATTERN_SCALE * GIFT_VERTICAL_RATIO);
            float startX = ANCHOR_CX + offsetX;
            float startY = ANCHOR_CY + offsetY;
            float x = cx + (startX - cx) * lp;
            float y = cy + (startY - cy) * lp - AndroidUtilities.dp(12);

            float scale = (0.5f + 0.5f * lp) * GIFT_SIZE_MULT;
//            float alpha = baseAlpha * (0.5f + 0.5f * lp);
            float alpha = 1f;

            gift.draw(canvas, x, y, scale, 0 /* rotation */, gift.animatedFloat.set(1f) * alpha, 1f);
        }
    }
}