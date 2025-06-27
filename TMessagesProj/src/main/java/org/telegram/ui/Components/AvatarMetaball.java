package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import android.os.Build;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.DisplayCutout;
import android.graphics.Rect;
import java.util.List;

import com.google.zxing.common.detector.MathUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;

public class AvatarMetaball extends View {


    // Punch‑hole (center camera) support
    private boolean useCameraTarget = false;
    private float cameraCx, cameraCy, cameraR;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path metaballPath = new Path();

    private float avatarCx, avatarCy, avatarR;
    private float progress;

    private final Point p1 = new Point(), p2 = new Point(),
            p3 = new Point(), p4 = new Point(),
            h1 = new Point(), h2 = new Point(),
            h3 = new Point(), h4 = new Point();

    private static final float HALF_PI = (float) (Math.PI / 2);
    // Distance (in px) at which the connecting blob starts to appear
    private static final float CONNECT_THRESHOLD = AndroidUtilities.dp(13); // 13 dp
    private static final float HANDLE_SIZE = 2.4f;

    public AvatarMetaball(Context ctx) {
        super(ctx);
        setWillNotDraw(false);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF000000);
        // Detect centred punch‑hole / notch camera and use it as absorption target
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
                                // accept only top‑edge cut‑outs centred horizontally
                                if (r.top == 0 &&
                                        Math.abs((r.left + r.right) / 2f - screenW / 2f) < r.width() / 2f) {
                                    cameraCx = (r.left + r.right) / 2f;
                                    cameraR  = Math.min(r.width(), r.height()) / 2f; // exact physical radius (use width if taller than wide)
                                    cameraCy = r.bottom - cameraR; // center = bottom minus radius
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

    public void setCameraTarget(float cx, float cy, float radius) {
        cameraCx = cx;
        cameraCy = cy;
        cameraR  = radius;
        useCameraTarget = true;
        invalidate();
    }

    /**
     * @return true if a centered punch‑hole camera target is being used.
     */
    public boolean hasCameraTarget() {
        return useCameraTarget;
    }

    /**
     * Returns the Y‑coordinate (in this view's coordinate system) of the bottom edge
     * of the inner metaball.  Call this right after `update()` to align external
     * animations (e.g. avatar starting Y) so they attach to the metaball's bottom.
     *
     * If a centered camera is detected the anchor equals (cameraCy + cameraR);
     * otherwise it returns 0, which corresponds to the top edge of the screen.
     */
    public float getInnerBottomY() {
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        if (useCameraTarget) {
            return (cameraCy + cameraR) - loc[1];
        } else {
            return 0f;
        }
    }

    /**
     * Returns the Y‑coordinate of the bottom edge of the inner metaball
     * in **window** coordinates (independent of this view’s position).
     */
    public float getInnerBottomYInWindow() {
        return useCameraTarget ? (cameraCy + cameraR) : 0f;
    }

    public void update(float cx, float cy, float r, float prog) {
        avatarCx = cx;
        avatarCy = cy;
        avatarR = r;
        progress = Utilities.clamp(prog, 1f, 0f);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (progress <= 0f) return;

        float EARLY_PULL = .1f;
        float R_M = 4f;
        float STICKINESS = progress < .6 ? .5f : 0.5f - (progress - 0.6f);

        final float r = avatarR;
        float tr, x2, y2;
        if (useCameraTarget) {
            int[] loc = new int[2];
            getLocationOnScreen(loc);          // view's top‑left in window coords
            x2 = cameraCx - loc[0];
            y2 = cameraCy - loc[1];
            tr = cameraR;
        } else {
            tr = r * R_M;
            x2 = avatarCx;
            y2 = -tr;
        }

        final float x1 = avatarCx;
        final float y1 = avatarCy;
        canvas.drawCircle(x2, y2, tr, paint);
        final float d = Math.abs(y2 - y1);
        // Draw separate circles until surfaces are within CONNECT_THRESHOLD px
        float gap = d - (r + tr); // distance between outer surfaces
        if (gap > CONNECT_THRESHOLD) {
            canvas.drawCircle(x1, y1, r, paint);
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

        getVector(x1, y1, angle1, r, p1);
        getVector(x1, y1, angle2, r, p2);
        getVector(x2, y2, angle3, tr, p3);
        getVector(x2, y2, angle4, tr, p4);

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
}
