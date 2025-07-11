package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;

public class AvatarMetaballOverlay extends View {
    private final Paint avatarOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avatarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float avatarCy, avatarR;
    private float progress;
    private Bitmap avatar;
    private Bitmap avartarBlur;

    public void setAvatar(Bitmap avatar) {
        int targetWidthPx = AndroidUtilities.dp2(64);
        if (avatar != null && avatar.getWidth() != targetWidthPx) {
            int targetHeightPx = (int) (avatar.getHeight() * (targetWidthPx / (float) avatar.getWidth()));
            this.avatar = Bitmap.createScaledBitmap(avatar, targetWidthPx, targetHeightPx, /* filter = */ false);
        } else {
            this.avatar = avatar;
        }
        invalidate();
    }

    private int inset = 0;

    public AvatarMetaballOverlay(Context ctx, boolean hasStories) {
        super(ctx);
        setWillNotDraw(false);
        inset = (hasStories ? AndroidUtilities.dp2(3.5f) : 0);
        avatarOverlayPaint.setStyle(Paint.Style.FILL);
        avatarOverlayPaint.setColor(0xFF000000);
        avatarPaint.setFilterBitmap(true);
    }

    public void update(float cy, float r, float prog) {
        avatarCy = cy;
        avatarR = r;
        progress = Utilities.clamp(prog, .999f, 0f);
        invalidate();
    }

    Rect src = new Rect();
    RectF dst = new RectF();
    Path clip = new Path();

    private boolean useNew = true;
    @Override
    protected void onDraw(Canvas canvas) {
        if (progress <= 0f || progress == 1f) {
            return;
        }

        float cx = getWidth() / 2f;

        if (avatar != null) {
            avartarBlur = avatar.copy(Bitmap.Config.ARGB_8888, true);
            try {
                if (useNew) {
                    Utilities.stackBlurBitmap2(avartarBlur, (int) (progress * 20));
                } else {
                    Utilities.stackBlurBitmap(avartarBlur, (int) (progress * 20));
                }
            } catch (Exception e) {
                useNew = false;
                Utilities.stackBlurBitmap(avartarBlur, (int) (progress * 20));
            }
        }
        if (avartarBlur != null) {
            canvas.save();
            clip.reset();
            clip.addCircle(cx, avatarCy, avatarR - inset * (1 - progress), Path.Direction.CW);
            canvas.clipPath(clip);

            src.set(0, 0, avartarBlur.getWidth(), avartarBlur.getHeight());
            dst.set(cx - avatarR, avatarCy - avatarR, cx + avatarR, avatarCy + avatarR);
            canvas.drawBitmap(avartarBlur, src, dst, avatarPaint);

            canvas.restore();
        }

        avatarOverlayPaint.setAlpha((int) (255 * progress));
        canvas.drawCircle(cx, avatarCy, avatarR + AndroidUtilities.dp(1)  * (1 - progress) - inset * (1 - progress), avatarOverlayPaint);
    }
}
