package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import com.google.zxing.common.detector.MathUtils;

import org.telegram.messenger.Utilities;

public class AvatarMetaballOverlay extends View {
    private final Paint avatarOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float avatarCx, avatarCy, avatarR;
    private float progress;

    public AvatarMetaballOverlay(Context ctx) {
        super(ctx);
        setWillNotDraw(false);
        avatarOverlayPaint.setStyle(Paint.Style.FILL);
        avatarOverlayPaint.setColor(0xFF000000);
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
        avatarOverlayPaint.setAlpha((int) (255 * progress));
        canvas.drawCircle(avatarCx, avatarCy, avatarR, avatarOverlayPaint);
    }

}
