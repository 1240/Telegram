package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;

import com.google.zxing.common.detector.MathUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;

import java.util.List;

public class AvatarMetaball extends View {


    private boolean useCameraTarget = false;
    private float cameraCy, cameraR;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path metaballPath = new Path();
    private float avatarCy, avatarR;
    private float progress;

    private final Point p1 = new Point(), p2 = new Point(),
            p3 = new Point(), p4 = new Point(),
            h1 = new Point(), h2 = new Point(),
            h3 = new Point(), h4 = new Point();

    private static final float HALF_PI = (float) (Math.PI / 2);
    private static final float HANDLE_SIZE = 2.4f;
    public static final float OFFSCREEN_TARGET_FACTOR = 4f;
    public static final float CAMERA_EXPANSION_MAX = 1.01f;
    private float cameraScale = 1f;

    public float getConnectThreshold() {
        float value;
        if (hasCameraTarget()) {
            value = AndroidUtilities.dp(60);
        } else {
            value = AndroidUtilities.dp(64);
        }
        return value;
    }

    public AvatarMetaball(Context ctx) {
        super(ctx);
        setWillNotDraw(false);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF000000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    WindowInsets insets = getRootWindowInsets();
                    if (insets != null && insets.getDisplayCutout() != null) {
                        DisplayCutout cutout = insets.getDisplayCutout();
                        List<Rect> bounds = cutout.getBoundingRects();
                        if (bounds != null) {
                            int screenW = getResources().getDisplayMetrics().widthPixels;
                            for (Rect r : bounds) {
                                if (r.top == 0 &&
                                        Math.abs((r.left + r.right) / 2f - screenW / 2f) < r.width() / 2f) {
                                    cameraR = Math.min(r.width(), r.height()) / 2f;
                                    cameraCy = r.bottom - cameraR;
                                    useCameraTarget = true;
                                    break;
                                }
                            }
                        }
                    }
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }
    }

    public boolean hasCameraTarget() {
        return useCameraTarget;
    }

    public float getInnerBottomYInWindow() {
        return useCameraTarget ? (cameraCy + cameraR) : 0f;
    }

    public void update(float cy, float r, float prog) {
        avatarCy = cy;
        avatarR = r;
        progress = Utilities.clamp(prog, 1f, 0f);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (progress <= 0f || progress == 1f) return;

        float centerX = getWidth() / 2f;

        final float r = avatarR;
        float tr, y2;
        float EARLY_PULL;
        float STICKINESS;
        if (useCameraTarget) {
            EARLY_PULL = .5f;
            STICKINESS = .3f;
            int[] loc = new int[2];
            getLocationOnScreen(loc);
            y2 = cameraCy - loc[1];
            tr = cameraR * cameraScale;
        } else {
            EARLY_PULL = .1f;
            STICKINESS = progress < .3 ? .5f : 0.5f - (progress - 0.3f);
            tr = r * OFFSCREEN_TARGET_FACTOR;
            y2 = -tr;
        }

        final float y1 = avatarCy;
        canvas.drawCircle(centerX, y2, tr, paint);
        final float d = Math.abs(y2 - y1);
        float gap = d - (r + tr);
        if (gap > getConnectThreshold()) {
            canvas.drawCircle(centerX, y1, r, paint);
            return;
        }

        final float pre = 1f - (d - (r + tr))
                / (tr * Utilities.clamp(EARLY_PULL, 2f, .0001f));
        final float v = Utilities.clamp(pre * STICKINESS, 1f, 0f);

        double u1 = Math.acos(clamp((r * r + d * d - tr * tr) / (2 * r * d)));
        double u2 = Math.acos(clamp((tr * tr + d * d - r * r) / (2 * tr * d)));

        final double angleBetweenCenters = y2 < y1 ? -Math.PI / 2 : Math.PI / 2;
        final double maxSpread = Math.acos(clamp((r - tr) / d));

        double angle1 = angleBetweenCenters + u1 + (maxSpread - u1) * v;
        double angle2 = angleBetweenCenters - u1 - (maxSpread - u1) * v;
        double angle3 = angleBetweenCenters + Math.PI - u2 - (Math.PI - u2 - maxSpread) * v;
        double angle4 = angleBetweenCenters - Math.PI + u2 + (Math.PI - u2 - maxSpread) * v;

        getVector(centerX, y1, angle1, r, p1);
        getVector(centerX, y1, angle2, r, p2);
        getVector(centerX, y2, angle3, tr, p3);
        getVector(centerX, y2, angle4, tr, p4);

        final float totalR = r + tr;
        float d2Base = Math.min(v * HANDLE_SIZE, dist(p1, p3) / totalR);
        float d2 = d2Base * Math.min(1f, (d * 2f) / totalR);

        float r1 = r * d2;
        float r2 = tr * d2;

        getVector(p1.x, p1.y, angle1 - HALF_PI, r1, h1);
        getVector(p2.x, p2.y, angle2 + HALF_PI, r1, h2);
        getVector(p3.x, p3.y, angle3 + HALF_PI, r2, h3);
        getVector(p4.x, p4.y, angle4 - HALF_PI, r2, h4);

        metaballPath.reset();
        metaballPath.moveTo(p1.x, p1.y);
        metaballPath.cubicTo(h1.x, h1.y, h3.x, h3.y, p3.x, p3.y);
        metaballPath.lineTo(p4.x, p4.y);
        metaballPath.cubicTo(h4.x, h4.y, h2.x, h2.y, p2.x, p2.y);
        metaballPath.close();
        canvas.drawPath(metaballPath, paint);
    }

    private static class Point {
        float x, y;
    }

    private void getVector(float cx, float cy, double a, float r, Point out) {
        out.x = (float) (cx + Math.cos(a) * r);
        out.y = (float) (cy + Math.sin(a) * r);
    }

    private float dist(Point a, Point b) {
        return MathUtils.distance(a.x, a.y, b.x, b.y);
    }

    private static double clamp(double v) {
        return v > 1 ? 1 : (v < -1 ? -1 : v);
    }

    /**
     * Scale applied to the camera metaball during the connection animation.
     */
    public void setCameraScale(float scale) {
        cameraScale = scale;
        invalidate();
    }

    /**
     * Base (unscaled) camera radius, returns 0 if not using the camera target.
     */
    public float getBaseCameraRadius() {
        return cameraR;
    }

    /**
     * Center‑Y of the camera metaball in window coordinates (unscaled).
     */
    public float getCameraCenterYInWindow() {
        return cameraCy;
    }
}
