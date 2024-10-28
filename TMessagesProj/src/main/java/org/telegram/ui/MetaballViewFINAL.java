package org.telegram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AvatarDrawable;

import java.util.ArrayList;
import java.util.List;

public class MetaballViewFINAL extends View {

    private final ChatActivity chatActivity;
    private int currentAccount = UserConfig.selectedAccount;
    private final Drawable drawable;
    private Paint circlePaint1;
    private Paint circlePaint2;
    private Paint popupPaint;
    private Paint gradientPaint;
    private Paint tooltipPaint;
    private TextPaint textPaint;
    private int targetIconAlfa;

    private Bitmap bitmap;
    private Canvas tempCanvas;
    private int w, h;
    private final int THRESHOLD = 210;

    private float circleX, circleY, circleSize;
    private float popupX, popupY, popupSize;

    private float targetWidth;
    private int circleColor;
    private int rectColor;
    private int contentPaddingTop;
    private Paint shadowPaint;
    private ArrayList<MessageObject> sendingMessageObjects;
    private final View parentView;
    private boolean darkTheme = Theme.getActiveTheme().isDark();
    private Theme.ResourcesProvider resourcesProvider;
    private final long popupStartTime;

    public MetaballViewFINAL(Context context, Drawable drawable, ChatActivity chatActivity, View parentView, Theme.ResourcesProvider resourcesProvider, int h, int w, int contentPaddingTop, long popupStartTime) {
        super(context);
        this.parentView = parentView;
        this.drawable = drawable;
        this.chatActivity = chatActivity;
        this.resourcesProvider = resourcesProvider;
        this.h = h;
        this.w = w;
        this.contentPaddingTop = contentPaddingTop;
        this.popupStartTime = popupStartTime;
        init();
    }

//    private Paint thresholdPaint;

    private void init() {
//        thresholdPaint = new Paint();
//        thresholdPaint.setFilterBitmap(true);
//        thresholdPaint.setColorFilter(new PorterDuffColorFilter(Color.argb(THRESHOLD, 0, 0, 0), PorterDuff.Mode.DST_IN));
//        choreographer = Choreographer.getInstance();
        rectColor = getThemedColor(darkTheme ? Theme.key_voipgroup_searchBackground : Theme.key_dialogSearchBackground);
        circleColor = Theme.getThemePaint(Theme.key_paint_chatActionBackground).getColor();
        targetIconAlfa = Color.alpha(circleColor);
        circleColor = Color.argb(255, Color.red(circleColor), Color.green(circleColor), Color.blue(circleColor));
        circlePaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint1.setColor(circleColor);
        circlePaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint2.setColor(circleColor);
        popupPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        popupPaint.setColor(rectColor);

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setMaskFilter(new BlurMaskFilter(dpToPx(80f), BlurMaskFilter.Blur.NORMAL));

        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        circleSize = dpToPx(32f);
        popupSize = dpToPx(58f);

        tooltipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipPaint.setStyle(Paint.Style.FILL);

        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dpToPx(11));
        textPaint.setTypeface(AndroidUtilities.bold());
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    float minY, maxY;
    float drawingAreaTop;
    float drawingAreaBottom;
    float drawingAreaHeight;

    private void calculateDrawingArea() {

        float circle1UpwardMovement = dpToPx(12f) * 2;
        float circle1DownwardMovement = dpToPx(18f) + dpToPx(6f);

        float circle1MinY = circleY - circle1UpwardMovement;
        float circle1MaxY = circleY + circle1DownwardMovement;

        float popupMinY = popupY - dpToPx(0);
        float popupMaxY = popupY + popupSize;

        minY = Math.min(circle1MinY, popupMinY);
        maxY = Math.max(circle1MaxY, popupMaxY);

        float padding = dpToPx(1f);
        minY -= padding + dpToPx(40f);
        maxY += padding;

        drawingAreaTop = minY;
        drawingAreaBottom = maxY;
        drawingAreaHeight = drawingAreaBottom - drawingAreaTop;

        bitmap = Bitmap.createBitmap(w, (int) drawingAreaHeight, Bitmap.Config.ARGB_8888);
        tempCanvas = new Canvas(bitmap);

        bitmapWidth = bitmap.getWidth();
        bitmapHeight = bitmap.getHeight();
        pixels = new int[bitmapWidth * bitmapHeight];
    }

    boolean skipForBitmap;

    private Bitmap captureParentView() {
        skipForBitmap = true;
        float scaleFactor = .1f;
        int scaledWidth = (int) (tooltipRect.width() * scaleFactor);
        int scaledHeight = (int) (tooltipRect.height() * scaleFactor);

        Bitmap bitmap = Bitmap.createBitmap(
                scaledWidth,
                scaledHeight,
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        canvas.translate(-tooltipRect.left * scaleFactor, -tooltipRect.top * scaleFactor - dpToPx(87) * scaleFactor);
        canvas.scale(scaleFactor, scaleFactor);

        parentView.draw(canvas);
        skipForBitmap = false;

        return bitmap;
    }

    private int getAdjustedColor(Bitmap bitmap, int prevColor) {
        int color = getAverageColorSampled(bitmap, 10);
        float lightThreshold = 200f;
        float darkThreshold = 55f;
        float darkenFactor = 0.8f;
        float lightenFactor = 1.2f;

        if (isColorTooLight(color, lightThreshold)) {
            color = darkenColor(color, darkenFactor);
        } else if (isColorTooDark(color, darkThreshold)) {
            color = lightenColor(color, lightenFactor);
        }
        if (prevColor != 0) {
            color = AndroidUtilities.getAverageColor(prevColor, color);
        }
        return color;
    }

    private static int getAverageColor(Bitmap bitmap, int startX, int startY, int regionWidth, int regionHeight) {
        long redSum = 0;
        long greenSum = 0;
        long blueSum = 0;
        long pixelCount = 0;

        for (int x = startX; x < startX + regionWidth; x++) {
            for (int y = startY; y < startY + regionHeight; y++) {
                if (x >= bitmap.getWidth() || y >= bitmap.getHeight()) {
                    continue;
                }

                int pixel = bitmap.getPixel(x, y);
                redSum += Color.red(pixel);
                greenSum += Color.green(pixel);
                blueSum += Color.blue(pixel);
                pixelCount++;
            }
        }

        if (pixelCount == 0) {
            return Color.GRAY;
        }

        int avgRed = (int) (redSum / pixelCount);
        int avgGreen = (int) (greenSum / pixelCount);
        int avgBlue = (int) (blueSum / pixelCount);

        return Color.rgb(avgRed, avgGreen, avgBlue);
    }

    public static List<Integer> getTopTwoColors(Bitmap bitmap) {
        List<Integer> topColors = new ArrayList<>();

        if (bitmap == null || bitmap.isRecycled()) {
            return topColors;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int midX = width / 2;

        int leftAverageColor = getAverageColor(bitmap, 0, 0, midX, height);
        topColors.add(leftAverageColor);

        int rightAverageColor = getAverageColor(bitmap, midX, 0, width - midX, height);
        topColors.add(rightAverageColor);

        return topColors;
    }

    private int[] getAdjusted2Color(Bitmap bitmap, int[] prevColor) {
        List<Integer> topColors = getTopTwoColors(bitmap);
        int[] adjustedColors = new int[2];

        float lightThreshold = 200f;
        float darkThreshold = 55f;
        float darkenFactor = 0.8f;
        float lightenFactor = 1.2f;

        for (int i = 0; i < topColors.size(); i++) {
            int color = topColors.get(i);

            if (isColorTooLight(color, lightThreshold)) {
                color = darkenColor(color, darkenFactor);
            } else if (isColorTooDark(color, darkThreshold)) {
                color = lightenColor(color, lightenFactor);
            }
            adjustedColors[i] = color;
        }

        if (prevColor[0] != 0) {
            adjustedColors[0] = AndroidUtilities.getAverageColor(prevColor[0], adjustedColors[0]);
            adjustedColors[1] = AndroidUtilities.getAverageColor(prevColor[1], adjustedColors[1]);
        }

        return adjustedColors;
    }

    public static int lightenColor(int color, float factor) {
        if (isColorVeryDarkHSV(color, 55f)) return Color.GRAY;
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] * factor;
        hsv[2] = Math.min(hsv[2], 1);
        return Color.HSVToColor(Color.alpha(color), hsv);
    }

    public static boolean isColorVeryDarkHSV(int color, float valueThreshold) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv[2] < valueThreshold;
    }

    public static int darkenColor(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] * factor;
        hsv[2] = Math.max(hsv[2], 0);
        return Color.HSVToColor(Color.alpha(color), hsv);
    }

    public static boolean isColorTooLight(int color, float threshold) {
        double luminance = 0.299 * Color.red(color) +
                0.587 * Color.green(color) +
                0.114 * Color.blue(color);
        return luminance > threshold;
    }

    public static boolean isColorTooDark(int color, float threshold) {
        double luminance = 0.299 * Color.red(color) +
                0.587 * Color.green(color) +
                0.114 * Color.blue(color);
        return luminance < threshold;
    }

    public int getAverageColorSampled(Bitmap bitmap, int step) {
        long redSum = 0;
        long greenSum = 0;
        long blueSum = 0;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int count = 0;

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int color = bitmap.getPixel(x, y);
                redSum += Color.red(color);
                greenSum += Color.green(color);
                blueSum += Color.blue(color);
                count++;
            }
        }

        if (count == 0) {
            return Color.GRAY;
        }

        int avgRed = (int) (redSum / count);
        int avgGreen = (int) (greenSum / count);
        int avgBlue = (int) (blueSum / count);

        return Color.rgb(avgRed, avgGreen, avgBlue);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (skipForBitmap) return;
        Canvas currentCanvas = animationProgress < 1f ? tempCanvas : canvas;

        if (!animationFinished) {
            bitmap.eraseColor(0);

            tempCanvas.save();
            tempCanvas.translate(0, -drawingAreaTop);
        }

        if (!animationFinished) {
            drawCircle1(tempCanvas, canvas, circleX, circleY, circleSize);
        }
        if (animationProgress < 1f) {
            drawCircle2(currentCanvas, circleX, circleY, circleSize);
        }

        if (collapseAnimationProgress > 0) {
            int alpha = (int) (255 * (1f - collapseAnimationProgress));
            shadowPaint.setAlpha(alpha);
            popupPaint.setAlpha(alpha);
        }

        drawRoundedRect(currentCanvas, circleX, circleY, circleSize, canvas);

        if (!animationFinished) {
            tempCanvas.restore();

            applyThreshold();
            canvas.drawBitmap(bitmap, 0, drawingAreaTop, gradientPaint);
        }

    }

//    private void calculateColors() {
//        if (!colorsSet) {
//            for (int i = 0; i < imageReceivers.length; i++) {
//                ImageReceiver imageReceiver = imageReceivers[i];
//                if (imageReceiver.getStaticThumb() instanceof BitmapDrawable) {
//                    final Bitmap bitmap = ((BitmapDrawable) imageReceiver.getStaticThumb()).getBitmap();
//                    try {
//                        final int bitmapColor = bitmap.getPixel(bitmap.getWidth() / 2, bitmap.getHeight() - 2);
//                        float[] hsl = new float[3];
//                        ColorUtils.colorToHSL(bitmapColor, hsl);
//                        if (hsl[1] <= .05f || hsl[1] >= .95f || hsl[2] <= .02f || hsl[2] >= .98f) {
//                            hsl[1] = 0;
//                            hsl[2] = Theme.isCurrentThemeDark() ? .38f : .70f;
//                        } else {
//                            hsl[1] = .25f;
//                            hsl[2] = Theme.isCurrentThemeDark() ? .35f : .65f;
//                        }
//                        colors[i] = ColorUtils.HSLToColor(hsl);
//                    } catch (Exception e) {
//                        FileLog.e(e);
//                    }
//                } else if (!colorsSet && !colorSetFromThumb) {
//                    try {
//                        final int color = ColorUtils.blendARGB(((AvatarDrawable) imageReceiver.getStaticThumb()).getColor(), ((AvatarDrawable) imageReceiver.getStaticThumb()).getColor2(), .5f);
//                        float[] hsl = new float[3];
//                        ColorUtils.colorToHSL(color, hsl);
//                        if (hsl[1] <= .05f || hsl[1] >= .95f) {
//                            hsl[2] = Utilities.clamp(hsl[2] - .1f, .6f, .3f);
//                        } else {
//                            hsl[1] = Utilities.clamp(hsl[1] - .06f, .4f, 0);
//                            hsl[2] = Utilities.clamp(hsl[2] - .08f, .5f, .2f);
//                        }
//                        colors[i] = ColorUtils.HSLToColor(hsl);
//                    } catch (Exception e) {
//                        FileLog.e(e);
//                    }
//                    colorSetFromThumb = true;
//                } else {
//                    colors[i] = defaultAvatarColor;
//                }
//            }
//        }
//        colorsSet = true;
//    }

    float offset1 = dpToPx(12f);
    float offset2 = dpToPx(18f);
    float offset3 = dpToPx(6f);
    RadialGradient gradient1;

    private void drawCircle1(Canvas canvas, Canvas canvas1, float x, float y, float size) {
        float newY;

        float alfaProgress = 0f;

        if (animationProgress > 0.80f) {
            alfaProgress = (animationProgress - 0.8f) / 0.2f;
        }

        if (animationProgress <= 0.5f) {
            newY = y - animationProgress * 2 * offset1;
        } else if (animationProgress <= 0.9f) {
            float progress = (animationProgress - 0.5f) / 0.4f;
            newY = y - offset1 + progress * offset2;
        } else {
            float progress = (Math.min(1, animationProgress) - 0.9f) / 0.1f;
            newY = y - offset1 + offset2 - progress * offset3;
        }


        if (animationProgress <= 0.80f) {
            gradient1 = new RadialGradient(
                    x, newY, size / 1.2f,
                    new int[]{circleColor, 0x00000000},
                    new float[]{0.5f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            circlePaint1.setShader(gradient1);
            canvas.drawCircle(x, newY, size, circlePaint1);
            drawAnimatedIcon(canvas, x, newY, size);
        } else {
            circlePaint2.setAlpha(Math.max(targetIconAlfa, (int) (255 * (1 - Math.min(alfaProgress, 1)))));
            canvas1.drawCircle(x, newY, circleSize / 2, circlePaint2);
            drawAnimatedIcon(canvas1, x, newY, size);
        }
    }

    private void drawAnimatedIcon(Canvas canvas, float circleX, float circleY, float circleSize) {
        if (drawable == null) {
            return;
        }

        float iconSize = dpToPx(24f);
        float halfIconSize = iconSize / 2f;

        float iconX = circleX - halfIconSize;
        float iconY = circleY - halfIconSize;

        float rotationAngle = calculateRotationAngle((Math.min(1, animationProgress)));

        canvas.save();

        canvas.translate(circleX, circleY);
        canvas.rotate(rotationAngle);
        canvas.translate(-circleX, -circleY);

        drawable.setBounds((int) iconX, (int) iconY, (int) (iconX + iconSize), (int) (iconY + iconSize));

        drawable.draw(canvas);

        canvas.restore();
    }

    private float calculateRotationAngle(float progress) {
        if (progress <= 0.5f) {
            float normalizedProgress = progress / 0.5f;
            return -30f * normalizedProgress;
        } else if (progress <= 0.9f) {
            float normalizedProgress = (progress - 0.5f) / 0.4f;
            return -30f + 60f * normalizedProgress;
        } else {
            float normalizedProgress = (progress - 0.9f) / 0.1f;
            return 30f - 30f * normalizedProgress;
        }
    }

    RadialGradient gradient2;

    private void drawCircle2(Canvas canvas, float x, float y, float size) {
        float currentAnimationBorder = 0.6f;
        float currentAnimationProgress = Math.min(animationProgress / currentAnimationBorder, 1);

        float newY, newX, newSize;
        float xOffset = dpToPx(0);
        newX = x + (popupX - x) * currentAnimationProgress - xOffset;
        newY = y - Math.abs(y - popupY) * currentAnimationProgress;
        newSize = size + Math.abs(size - popupSize) * currentAnimationProgress;

        if (animationProgress > 0.4f) {
            float additionalProgress = Math.abs((animationProgress - 0.4f)) / 0.6f;
            float sizeDecrease = dpToPx(22f);
            newSize -= sizeDecrease * additionalProgress;
        }

        gradient2 = new RadialGradient(
                newX, newY, Math.max(newSize, 0.001f),
                new int[]{rectColor, 0x00000000},
                new float[]{0.5f, 1.0f},
                Shader.TileMode.CLAMP
        );
        gradientPaint.setShader(gradient2);

        canvas.drawCircle(newX, newY, Math.max(newSize, 0.001f), gradientPaint);
    }

    private RectF rect = new RectF();
    private RectF shadowRect = new RectF();

    private void drawRoundedRect(Canvas canvas, float x, float y, float size, Canvas canvas1) {
        float currentAnimationExpandBorder = 0.4f;
        float currentAnimationBorder = 0.6f;
        float currentAnimationProgress = Math.min(animationProgress / currentAnimationBorder, 1);
        float offset = 0;

        float newY, newSize;
        newY = y - Math.abs(y - popupY) * currentAnimationProgress;
        if (animationProgress > currentAnimationProgress) {
            newSize = size + Math.abs(size - popupSize) * (animationProgress - currentAnimationBorder) / (1 - currentAnimationBorder);
        } else {
            newSize = size + Math.abs(size - popupSize) * currentAnimationProgress;
        }

        float rectWidth = newSize;
        if (animationProgress > currentAnimationExpandBorder) {
            float localProgress = (animationProgress - currentAnimationExpandBorder) / (1 - currentAnimationExpandBorder);
            rectWidth = popupSize + (targetWidth - popupSize) * localProgress;
            if (dpToPx(4) + popupX + targetWidth / 2 > w) {
                offset = ((popupX + targetWidth / 2 + dpToPx(4)) - w) * localProgress;
            }
        }
        float rectHeight = newSize;
        float cornerRadius = popupSize;

        float leftBound = x - rectWidth / 2 - offset;
        float rightBound = x + rectWidth / 2 - offset;

//        if (leftBound < dpToPx(4)) {
//            float diff = dpToPx(4) - leftBound;
//            leftBound += diff;
//            rightBound += diff;
//        }
//        if (rightBound > w - dpToPx(4)) {
//            float diff = rightBound - (w - dpToPx(4));
//            leftBound -= diff;
//            rightBound -= diff;
//        }

        rect.set(
                leftBound,
                newY - rectHeight / 2 - dpToPx(4),
                rightBound,
                newY + rectHeight / 2 - dpToPx(4)
        );
        shadowRect.set(
                rect.left + dpToPx(20),
                rect.top + dpToPx(20),
                rect.right - dpToPx(20),
                rect.bottom - dpToPx(20)
        );
        canvas1.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, popupPaint);

        drawAvatars(canvas, rect, canvas1);
    }


    Path path = new Path();
    PathMeasure pathMeasure;
    float pathLength;
    float[] pos = new float[2];
    RectF bounds = new RectF();
    RectF tooltipRect = new RectF();
    //    int[] colors = new int[5];
    int[][] colors2 = new int[5][2];
    float avatarsPadding = dpToPx(8f);

    private void drawAvatars(Canvas canvas, RectF rect, Canvas canvas1) {
        int avatarCount = dialogs.size();
        float avatarDiameter = rect.height() - 2 * avatarsPadding;
        float totalAvatarWidth = avatarCount * avatarDiameter + (avatarCount - 1) * avatarsPadding;
        float startX = rect.left + (rect.width() - totalAvatarWidth) / 2;
        float centerY = rect.centerY();

        for (int i = 0; i < avatarCount; i++) {
            float avatarX = startX + i * (avatarDiameter + avatarsPadding);
            float avatarSize = 0f;

            float animationBorder2 = 0.60f;
            float animationBorder3 = 0.89f;
            float animationBorder1 = 0.42f;
            int centerIndex = avatarCount / 2;
            int leftNeighbor = centerIndex - 1;
            int rightNeighbor = centerIndex + 1;


            if ((i == 0 || i == avatarCount - 1)) {
                if (animationProgress > animationBorder3) {
                    float progress = (animationProgress - animationBorder3) / (1.0f - animationBorder3);
                    avatarSize = avatarDiameter * Math.min(progress, 1);
                }
            } else if ((i == leftNeighbor || i == rightNeighbor)) {
                if (animationProgress > animationBorder2) {
                    float progress = (animationProgress - animationBorder2) / (animationBorder3 - animationBorder2) * 0.7f;
                    avatarSize = avatarDiameter * Math.min(progress, 1);
                }
            } else if (i == centerIndex) {
                if (animationProgress > animationBorder1) {
                    float progress = (animationProgress - animationBorder1) / (animationBorder2 - animationBorder1) * 0.3f;
                    avatarSize = avatarDiameter * Math.min(progress, 1);
                }
            }

            if (avatarSize > 0) {
                ImageReceiver imageReceiver = imageReceivers[i];
                imageReceiver.setRoundRadius((int) popupSize);
                if (animationProgress >= 1f) {
                    final float alfa;
                    float scaleFactor;

                    if (highlightedAvatarIndex == -1 && previousHighlightedAvatarIndex != -1) {
                        if (i == previousHighlightedAvatarIndex) {
                            scaleFactor = 2f * Math.max(previousTooltipProgress - tooltipProgress, 0);
                            alfa = .6f + .4f * Math.max(previousTooltipProgress, tooltipProgress);
                        } else {
                            scaleFactor = 0f;
                            alfa = .6f + .4f * tooltipProgress;
                        }
                    } else if (highlightedAvatarIndex == -1 && finalAvatarIndex == -1) {
                        scaleFactor = 0f;
                        alfa = 1f;
                    } else if ((highlightedAvatarIndex == i) || (i == previousHighlightedAvatarIndex)) {
                        if (previousHighlightedAvatarIndex == -1) {
                            alfa = 1f;
                            scaleFactor = 2f * tooltipProgress;
                        } else {
                            float currentSelectProgress = (i == previousHighlightedAvatarIndex) ? Math.max(previousTooltipProgress - tooltipProgress, 0) : tooltipProgress;
                            alfa = .6f + .4f * currentSelectProgress;
                            scaleFactor = 2f * currentSelectProgress;
                        }
                    } else if (previousHighlightedAvatarIndex == -1) {
                        float currentSelectProgress = tooltipProgress;
                        alfa = 1f - .4f * currentSelectProgress;
                        scaleFactor = 0f;
                    } else {
                        alfa = .6f;
                        scaleFactor = 0f;
                    }
                    if (collapseAnimationProgress != 0 && finalAvatarIndex == i) {
                        float scale = 10 * (collapseAnimationProgress);
                        imageReceiver.setAlpha(1);
                        pathMeasure.getPosTan(pathLength * collapseAnimationProgress, pos, null);
                        bounds.set(
                                -dpToPx(2) + dpToPx(scale) + pos[0] + avatarDiameter / 2 - avatarSize / 2,
                                -dpToPx(2) + dpToPx(scale) + pos[1] - avatarSize / 2,
                                -dpToPx(2) - dpToPx(scale) + pos[0] + avatarDiameter / 2 - avatarSize / 2 + avatarSize,
                                -dpToPx(2) - dpToPx(scale) + pos[1] - avatarSize / 2 + avatarSize
                        );
                    } else if (collapseAnimationProgress != 0) {
                        imageReceiver.setAlpha(alfa * (1 - collapseAnimationProgress));
                        bounds.set(
                                -dpToPx(scaleFactor) + avatarX + avatarDiameter / 2 - avatarSize / 2,
                                -dpToPx(scaleFactor) + centerY - avatarSize / 2,
                                dpToPx(scaleFactor) + avatarX + avatarDiameter / 2 - avatarSize / 2 + avatarSize,
                                dpToPx(scaleFactor) + centerY - avatarSize / 2 + avatarSize
                        );
                    } else {
                        imageReceiver.setAlpha(alfa);
                        bounds.set(
                                -dpToPx(scaleFactor) + avatarX + avatarDiameter / 2 - avatarSize / 2,
                                -dpToPx(scaleFactor) + centerY - avatarSize / 2,
                                dpToPx(scaleFactor) + avatarX + avatarDiameter / 2 - avatarSize / 2 + avatarSize,
                                dpToPx(scaleFactor) + centerY - avatarSize / 2 + avatarSize
                        );
                    }
                    if (avatarRects.size() != dialogs.size()) {
                        avatarRects.add(
                                new RectF(
                                        -(avatarsPadding / 2 + dpToPx(1)) - dpToPx(scaleFactor) + avatarX + avatarDiameter / 2 - avatarSize / 2,
                                        -avatarsPadding - dpToPx(scaleFactor) + centerY - avatarSize / 2,
                                        (avatarsPadding / 2 + dpToPx(1)) + dpToPx(scaleFactor) + avatarX + avatarDiameter / 2 - avatarSize / 2 + avatarSize,
                                        avatarsPadding + dpToPx(scaleFactor) + centerY - avatarSize / 2 + avatarSize
                                )
                        );
                    }
                    imageReceiver.setImageCoords(bounds);
                    imageReceiver.draw(canvas);

                    if ((highlightedAvatarIndex != -1 && i == highlightedAvatarIndex) || (previousHighlightedAvatarIndex != -1 && i == previousHighlightedAvatarIndex)) {
//                        int color = colors[i];
                        String name = TextUtils.ellipsize(names[i].replace('\n', ' '), textPaint, dpToPx(82), TextUtils.TruncateAt.END).toString();
                        Rect textBounds = new Rect();
                        textPaint.getTextBounds(name, 0, name.length(), textBounds);

                        float paddingHorizontal = dpToPx(7f);
                        float paddingVertical = dpToPx(5f);

                        float textWidth = textBounds.width();
                        float textHeight = textBounds.height();
                        float tooltipWidth = textWidth + 2 * paddingHorizontal;
                        float tooltipHeight = textHeight + 2 * paddingVertical;

                        float left = (bounds.left + bounds.right) / 2 - tooltipWidth / 2;
                        float top = bounds.top - tooltipHeight - dpToPx(9) - avatarsPadding;
                        float right = left + tooltipWidth;
                        float bottom = top + tooltipHeight;

                        if (left < dpToPx(4)) {
                            float diff = dpToPx(4) - left;
                            left += diff;
                            right += diff;
                        }
                        if (right > w - dpToPx(4)) {
                            float diff = right - (w - dpToPx(4));
                            left -= diff;
                            right -= diff;
                        }
                        float currentSelectProgress = (i == previousHighlightedAvatarIndex) ? Math.max(previousTooltipProgress - tooltipProgress, 0) : tooltipProgress;
                        if (currentSelectProgress != 0) {
                            tooltipPaint.setStrokeWidth(dpToPx(1));
                            tooltipPaint.setAlpha((int) (currentSelectProgress * 255));
                            textPaint.setAlpha((int) (currentSelectProgress * 255));
                            tooltipRect.set(left, top, right, bottom);

                            Bitmap capturedBitmap = captureParentView();
//                            int tooltipPaintColor = getAdjustedColor(capturedBitmap, colors[i]);

                            int[] tooltipPaintColor2 = getAdjusted2Color(capturedBitmap, colors2[i]);
                            colors2[i] = tooltipPaintColor2;
                            Shader linearGradient = new LinearGradient(
                                    tooltipRect.left, tooltipRect.top, tooltipRect.right, tooltipRect.bottom,
                                    tooltipPaintColor2[0], tooltipPaintColor2[1],
                                    Shader.TileMode.CLAMP
                            );
                            tooltipPaint.setShader(linearGradient);
//                            colors[i] = tooltipPaintColor;
//                            tooltipPaint.setColor(tooltipPaintColor);

                            canvas1.drawRoundRect(tooltipRect, tooltipHeight, tooltipHeight, tooltipPaint);
                            canvas1.drawText(name, (left + right) / 2, (top + bottom) / 2 + dpToPx(4), textPaint);
                            invalidate();
                        }
                    }
                } else {
                    bounds.set(
                            avatarX + avatarDiameter / 2 - avatarSize / 2,
                            centerY - avatarSize / 2,
                            avatarX + avatarDiameter / 2 - avatarSize / 2 + avatarSize,
                            centerY - avatarSize / 2 + avatarSize
                    );
                    imageReceiver.setImageCoords(bounds);
                    imageReceiver.draw(canvas);
                }
            }
        }
    }

//    private final Executor executor = Executors.newSingleThreadExecutor();
//    private final Handler mainHandler = new Handler(Looper.getMainLooper());
//
//    private void applyThresholdAsync() {
//        executor.execute(() -> {
//            applyThreshold();
//            mainHandler.post(() -> invalidate());
//        });
//    }

    int bitmapWidth;
    int bitmapHeight;
    int[] pixels;

    private void applyThreshold() {
        bitmap.getPixels(pixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int alpha = pixel & 0xFF000000;
            if ((alpha >>> 24) < THRESHOLD) {
                pixels[i] = 0x01000000;
            }
        }

        bitmap.setPixels(pixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);
    }


    private int highlightedAvatarIndex = -1;
    private int finalAvatarIndex = -1;
    private int previousHighlightedAvatarIndex = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (animationProgress >= 1f) {
                    int previousHighlightedIndex = highlightedAvatarIndex;
                    highlightedAvatarIndex = getTouchedAvatarIndex(touchX, touchY - getY());
                    if (previousHighlightedIndex != highlightedAvatarIndex) {
                        previousHighlightedAvatarIndex = previousHighlightedIndex;
                        animateTooltip();
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (animationFinished) {
                    collapseAnimation();
                    highlightedAvatarIndex = -1;
                    previousHighlightedAvatarIndex = -1;
                } else {
                    reverseAnimation();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (animationProgress >= 1f) {
                    if (highlightedAvatarIndex != -1) {
                        sendInternal(highlightedAvatarIndex);
                        RectF rectF = avatarRects.get(highlightedAvatarIndex);
                        path.reset();
                        path.moveTo(rectF.left + rectF.width() / 2 - dpToPx(20), rectF.top + rectF.height() / 2);
                        if (AccountInstance.getInstance(currentAccount).getUserConfig().isPremium()
                                && DialogObject.isUserDialog(dialogs.get(highlightedAvatarIndex).first)
                                && UserObject.isUserSelf(MessagesController.getInstance(currentAccount).getUser(dialogs.get(highlightedAvatarIndex).first))) {
                            float y2 = dpToPx(44) + contentPaddingTop;
                            path.quadTo(0, Math.min(rectF.bottom + dpToPx(50), h), dpToPx(20), y2);
                        } else {
                            float y2 = h - dpToPx(174);
                            path.quadTo(0, Math.max(rectF.top - dpToPx(50), 0), dpToPx(20), y2);
                        }
                        pathMeasure = new PathMeasure(path, false);
                        pathLength = pathMeasure.getLength();
                    }
                    collapseAnimation();
                    highlightedAvatarIndex = -1;
                    previousHighlightedAvatarIndex = -1;
                } else {
                    reverseAnimation();
                }
                return true;
        }
        return false;
    }

    private List<RectF> avatarRects = new ArrayList<>();

    private int getTouchedAvatarIndex(float x, float y) {
        for (int i = 0; i < avatarRects.size(); i++) {
            RectF rect = avatarRects.get(i);
            if (rect.left <= x && x <= rect.right && rect.top - dpToPx(80) <= y && y <= rect.bottom + dpToPx(80)) {
                return i;
            }
        }
        return -1;
    }

    private float animationProgress = 0f;
    private boolean animationFinished = false;
    private float collapseAnimationProgress = 0f;
    private float tooltipProgress = 0f;
    private float previousTooltipProgress = 0f;

    private boolean isPointInsideCircle(float x, float y, float centerX, float centerY, float radius) {
        return Math.hypot(x - centerX, y - centerY) <= radius;
    }

    private float dpToPx(float dp) {
        return AndroidUtilities.dp2(dp);
    }

    public void init(float x, ArrayList<MessageObject> messages, float finalY) {
        circleX = x + dpToPx(16);
        circleY = finalY;
        popupX = circleX;
        popupY = circleY - dpToPx(56);
        sendingMessageObjects = messages;

        fetchDialogs();
        float avatarCount = dialogs.size();
        targetWidth = (popupSize - avatarsPadding * 2) * avatarCount + (avatarCount + 1) * avatarsPadding;
        calculateDrawingArea();
        addAvatars();

    }

    private ArrayList<Pair<Long, TLRPC.Dialog>> dialogs = new ArrayList<>();

    public void fetchDialogs() {
        dialogs.clear();
        long selfUserId = UserConfig.getInstance(currentAccount).clientUserId;
        if (!MessagesController.getInstance(currentAccount).dialogsForward.isEmpty()) {
            TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogsForward.get(0);
            dialogs.add(new Pair<>(dialog.id, dialog));
        }
        ArrayList<TLRPC.Dialog> allDialogs = MessagesController.getInstance(currentAccount).getAllDialogs();
        for (int a = 0; a < allDialogs.size(); a++) {
            TLRPC.Dialog dialog = allDialogs.get(a);
            if (!(dialog instanceof TLRPC.TL_dialog)) {
                continue;
            }
            if (dialog.id == selfUserId) {
                continue;
            }
            if (!DialogObject.isEncryptedDialog(dialog.id)) {
                if (DialogObject.isUserDialog(dialog.id)) {
                    dialogs.add(new Pair<>(dialog.id, dialog));
                } else {
                    TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
                    if (!(chat == null || ChatObject.isNotInChat(chat) || chat.gigagroup && !ChatObject.hasAdminRights(chat) || ChatObject.isChannel(chat) && !chat.creator && (chat.admin_rights == null || !chat.admin_rights.post_messages) && !chat.megagroup)) {
                        dialogs.add(new Pair<>(dialog.id, dialog));
                    }
                }
            }
        }
        try {
            List<Pair<Long, TLRPC.Dialog>> sublist = new ArrayList<>(dialogs.subList(0, Math.min(5, dialogs.size())));
            dialogs.clear();
            dialogs.addAll(sublist);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        for (ImageReceiver imageReceiver : imageReceivers) {
            imageReceiver.onAttachedToWindow();
        }
//        calculateColors();
        startAnimation();
//        startFrameCallback();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        for (ImageReceiver imageReceiver : imageReceivers) {
            imageReceiver.onDetachedFromWindow();
        }
//        stopFrameCallback();
        if (animator != null) {
            animator.cancel();
        }
        if (collapseAnimator != null) {
            collapseAnimator.cancel();
        }
        if (tooltipAnimator != null) {
            tooltipAnimator.cancel();
        }

        animator = null;
        collapseAnimator = null;
        tooltipAnimator = null;
        setVisibility(GONE);
    }

    ImageReceiver[] imageReceivers = {new ImageReceiver(this), new ImageReceiver(this), new ImageReceiver(this), new ImageReceiver(this), new ImageReceiver(this)};
    String[] names = new String[5];
//    boolean colorsSet = false;
//    boolean colorSetFromThumb = false;

    public void addAvatars() {
        for (int i = 0; i < Math.min(dialogs.size(), 5); i++) {
            AvatarDrawable avatarDrawable = new AvatarDrawable();
            ImageReceiver imageReceiver = imageReceivers[i];
            imageReceiver.setInvalidateAll(true);
            imageReceiver.setCrossfadeDuration(0);
            TLRPC.Dialog dialog = dialogs.get(i).second;
            long dialogId = dialog.id;
            if (DialogObject.isUserDialog(dialogId)) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
                avatarDrawable.setInfo(currentAccount, user);
                names[i] = UserObject.getFirstName(user);
                if (UserObject.isReplyUser(user)) {
                    avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_REPLIES);
                    imageReceiver.setForUserOrChat(user, avatarDrawable);
                } else if (UserObject.isUserSelf(user)) {
                    avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_SAVED);
                    names[i] = LocaleController.getString(R.string.SavedMessages);
                    imageReceiver.setImage(null, null, null, null, avatarDrawable, 0, null, user, 0);
                } else {
                    imageReceiver.setForUserOrChat(user, avatarDrawable);
                }
            } else {
                TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
                avatarDrawable.setInfo(currentAccount, chat);
                names[i] = chat.title;
                imageReceiver.setForUserOrChat(chat, avatarDrawable);
            }
        }
    }

    protected void sendInternal(int index) {
        if (index != -1) {
            finalAvatarIndex = index;
            boolean withSound = true;
            long key = dialogs.get(index).first;
            if (AlertsCreator.checkSlowMode(getContext(), currentAccount, key, false)) {
                return;
            }
            if (sendingMessageObjects != null) {
                int result = SendMessagesHelper.getInstance(currentAccount).sendMessage(sendingMessageObjects, key, true, false, withSound, 0, null);
                AlertsCreator.showSendMediaAlert(result, chatActivity, null);
                onSend(dialogs.get(index).second, sendingMessageObjects.size(), null);
            }
        }
    }

    protected void onSend(TLRPC.Dialog did, int count, TLRPC.TL_forumTopic topic) {

    }

    protected void onAnimationFinished() {

    }

    protected void onCollapseAnimationFinished(long popupStartTime) {
        setVisibility(View.GONE);
    }


    private ValueAnimator animator;
    private ValueAnimator collapseAnimator;
    private ValueAnimator tooltipAnimator;

    public void startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(500);
        animator.setInterpolator(new OvershootInterpolator(1.01f));

        animator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            postInvalidateOnAnimation();
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animationProgress = 1f;
                animationFinished = true;
                onAnimationFinished();
                postInvalidateOnAnimation();
            }
        });

        animator.start();
    }

    private void collapseAnimation() {
        collapseAnimator = ValueAnimator.ofFloat(collapseAnimationProgress, 1f);
        collapseAnimator.setDuration(300);
        collapseAnimator.setInterpolator(new LinearInterpolator());
        collapseAnimator.addUpdateListener(animation -> {
            collapseAnimationProgress = (float) animation.getAnimatedValue();
            postInvalidateOnAnimation();
        });

        collapseAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                collapseAnimationProgress = 1f;
                highlightedAvatarIndex = -1;
                previousHighlightedAvatarIndex = -1;
                postInvalidateOnAnimation();
                onCollapseAnimationFinished(popupStartTime);
            }
        });

        collapseAnimator.start();
    }

    private void reverseAnimation() {
        animator = ValueAnimator.ofFloat(animationProgress, 0f);
        animator.setDuration(150);
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            postInvalidateOnAnimation();
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animationProgress = 0f;
                onAnimationFinished();
                onCollapseAnimationFinished(popupStartTime);
                postInvalidateOnAnimation();
            }
        });

        animator.start();
    }

    private void animateTooltip() {
        previousTooltipProgress = tooltipProgress;
        tooltipAnimator = ValueAnimator.ofFloat(0, 1);
        tooltipAnimator.setDuration(150);
        tooltipAnimator.addUpdateListener(animation -> {
            tooltipProgress = (float) animation.getAnimatedValue();
            postInvalidateOnAnimation();
        });
        tooltipAnimator.start();
    }

    protected int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }


//    private Choreographer choreographer;
//    private Choreographer.FrameCallback frameCallback;

//    private int frameCount = 0;
//    private long lastTime = 0L;
//
//    private void startFrameCallback() {
//        frameCallback = new Choreographer.FrameCallback() {
//            @Override
//            public void doFrame(long frameTimeNanos) {
//                if (lastTime == 0L) {
//                    lastTime = frameTimeNanos;
//                }
//                frameCount++;
//
//                long delta = frameTimeNanos - lastTime;
//                if (delta >= 1_000_000_000L) {
//                    double fps = frameCount * 1_000_000_000.0 / delta;
//                    Log.d("Performance", "FPS: " + fps);
//                    frameCount = 0;
//                    lastTime = frameTimeNanos;
//                }
//
//
//                choreographer.postFrameCallback(this);
//            }
//        };
//        choreographer.postFrameCallback(frameCallback);
//    }
//
//    private void stopFrameCallback() {
//        if (frameCallback != null) {
//            choreographer.removeFrameCallback(frameCallback);
//            frameCallback = null;
//        }
//    }
}