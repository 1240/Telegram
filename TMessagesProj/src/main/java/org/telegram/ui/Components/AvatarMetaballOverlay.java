package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import com.google.zxing.common.detector.MathUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;

public class AvatarMetaballOverlay extends View {

    private final static float R_M = 2.1f;
    private final static float EARLY_PULL = .6f;
    private final static float STICKINESS = .2f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path metaballPath = new Path();

    private float avatarCx, avatarCy, avatarR;
    private float progress;

    private final Point p1 = new Point(), p2 = new Point(),
            p3 = new Point(), p4 = new Point(),
            h1 = new Point(), h2 = new Point(),
            h3 = new Point(), h4 = new Point();

    private static final float HALF_PI = (float) (Math.PI / 2);
    private static final float HANDLE_SIZE = 2.4f;

    public AvatarMetaballOverlay(Context ctx) {
        super(ctx);
        setWillNotDraw(false);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF000000);
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

        final float r = avatarR;
        final float tr = r * R_M;

        final float x1 = avatarCx;
        final float y1 = avatarCy;
        final float x2 = avatarCx;
        final float y2 = -tr;
        canvas.drawCircle(x2, y2, tr, paint);
        final float maxDist = r + tr * (1f + Utilities.clamp(EARLY_PULL, 2f, 0f));
        final float d = Math.abs(y2 - y1);
        if (d >= maxDist) {
            canvas.drawCircle(x1, y1, r, paint);
            return;
        }

        final float pre = 1f - (d - (r + tr))
                / (tr * Utilities.clamp(EARLY_PULL, 2f, .0001f));
        final float v = Utilities.clamp(pre * STICKINESS  * (float) Math.pow(1.0 + progress, 2.0), 1f, 0f);

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

        canvas.drawCircle(x1, y1, r, paint);
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
