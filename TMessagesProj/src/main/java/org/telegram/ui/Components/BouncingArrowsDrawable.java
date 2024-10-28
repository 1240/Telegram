package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;

public class BouncingArrowsDrawable extends Drawable {

    private Paint paint;
    private Path path = new Path();
    private float animProgress;
    private float animateToProgress;
    private long lastUpdateTime;

    public BouncingArrowsDrawable() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        animProgress = 0f;
        animateToProgress = 0f;
        lastUpdateTime = SystemClock.elapsedRealtime();

    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        updateAnimation();
        updatePath();
        canvas.drawPath(path, paint);
    }

    private void updatePath() {
        path.reset();

        float p = animProgress * 2 - 1;

        Rect bounds = getBounds();

        float chevronWidth = bounds.width() * 0.6f;
        float chevronHeight = bounds.height() * 0.25f;

        float centerX = bounds.centerX() + 6;
        float centerY = bounds.centerY() - 6;

        float bounceOffset = (bounds.height() * 0.08f) * p;

        path.moveTo(centerX - chevronWidth / 2, centerY - 2 - chevronHeight / 2 - bounceOffset);
        path.lineTo(centerX, centerY - 2 + chevronHeight / 2 - bounceOffset);
        path.lineTo(centerX + chevronWidth / 2, centerY - 2 - chevronHeight / 2 - bounceOffset);

        path.moveTo(centerX - chevronWidth / 2, centerY + 2 + chevronHeight / 2 - bounceOffset);
        path.lineTo(centerX, centerY + 2 + (3 * chevronHeight / 2) - bounceOffset);
        path.lineTo(centerX + chevronWidth / 2, centerY + 2 + chevronHeight / 2 - bounceOffset);
    }

    private void updateAnimation() {
        if (animProgress != animateToProgress) {
            long currentTime = SystemClock.elapsedRealtime();
            long deltaTime = currentTime - lastUpdateTime;
            lastUpdateTime = currentTime;

            float animationSpeed = deltaTime / 600f;

            if (animProgress < animateToProgress) {
                animProgress += animationSpeed;
                if (animProgress > animateToProgress) {
                    animProgress = animateToProgress;
                }
            } else {
                animProgress -= animationSpeed;
                if (animProgress < animateToProgress) {
                    animProgress = animateToProgress;
                }
            }

            invalidateSelf();
        } else {
            animateToProgress = animProgress == 0f ? 1f : 0f;
            lastUpdateTime = SystemClock.elapsedRealtime();
            invalidateSelf();
        }
    }

    public void startAnimation() {
        animateToProgress = 1f;
        lastUpdateTime = SystemClock.elapsedRealtime();
        invalidateSelf();
    }

    public void stopAnimation() {
        animateToProgress = 0f;
        lastUpdateTime = SystemClock.elapsedRealtime();
        invalidateSelf();
    }

    public void setColor(int color) {
        paint.setColor(color);
        invalidateSelf();
    }

    @Keep
    public float getAnimationProgress() {
        return animProgress;
    }

    @Override
    public int getIntrinsicWidth() {
        return AndroidUtilities.dp(12);
    }

    @Override
    public int getIntrinsicHeight() {
        return AndroidUtilities.dp(16);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public int getAlpha() {
        return paint.getAlpha();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return paint.getAlpha() == 255 ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
    }
}

