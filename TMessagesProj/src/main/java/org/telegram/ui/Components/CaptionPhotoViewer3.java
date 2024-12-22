package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;
import static org.telegram.messenger.AndroidUtilities.lerp;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.ActionBar.Theme.RIPPLE_MASK_CIRCLE_20DP;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedTextView.AnimatedTextDrawable;
import org.telegram.ui.PhotoViewerRoundVideoRecorder;
import org.telegram.ui.Stories.recorder.CaptionContainerView;
import org.telegram.ui.Stories.recorder.HintView2;

public class CaptionPhotoViewer3 extends CaptionContainerView {

    private boolean addPhotoVisible;
    private ImageView addPhotoButton;

    private final HintView2 hint;
    private final Runnable applyCaption;

    private final RectF moveButtonBounds = new RectF();
    private Drawable moveButtonIcon;
    private final AnimatedTextDrawable moveButtonText = new AnimatedTextDrawable();
    private final ButtonBounce moveButtonBounce = new ButtonBounce(this);

    public ButtonBounce roundButtonBounce;
    public ImageView roundButton;
    private boolean hasRoundVideo;

    private Drawable flipButton;

    private PhotoViewerRoundVideoRecorder currentRecorder;
    private float fromX, fromY;
    private final AnimatedFloat cancelT = new AnimatedFloat(this, 0, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
    private final AnimatedFloat cancel2T = new AnimatedFloat(this::invalidateDrawOver2, 0, 420, CubicBezierInterpolator.EASE_OUT_QUINT);
    private final AnimatedFloat lockT = new AnimatedFloat(this, 0, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
    private final AnimatedFloat lock2T = new AnimatedFloat(this::invalidateDrawOver2, 0, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
    private boolean cancelling, stopping, locked;
    private boolean recordTouch;
    private boolean recording;

    private float slideProgress;
    private float lockProgress;
    private long startTime;

    private float amplitude;
    private final AnimatedFloat animatedAmplitude = new AnimatedFloat(this::invalidateDrawOver2, 0, 200, CubicBezierInterpolator.DEFAULT);

    private final CaptionPhotoViewer3.RecordDot recordPaint = new CaptionPhotoViewer3.RecordDot(this);
    private final AnimatedTextDrawable timerTextDrawable = new AnimatedTextDrawable(false, true, true);

    private Text slideToCancelText;
    private Path slideToCancelArrowPath;
    private Paint slideToCancelArrowPaint;
    private Text cancelText;

    private final Paint whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint roundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final BlobDrawable tinyWaveDrawable = new BlobDrawable(11, LiteMode.FLAGS_CHAT);
    private final BlobDrawable bigWaveDrawable = new BlobDrawable(12, LiteMode.FLAGS_CHAT);
    private final Drawable roundDrawable;

    private final Path circlePath = new Path();
    private final Path boundsPath = new Path();

    private final Paint lockBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lockShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lockHandlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final AnimatedFloat lockCancelledT = new AnimatedFloat(this::invalidateDrawOver2, 350, CubicBezierInterpolator.EASE_OUT_QUINT);

    private final RectF lockBounds = new RectF();
    private final RectF cancelBounds = new RectF();
    private final RectF lockRect = new RectF();
    private final Path lockHandle = new Path();

    private boolean moveButtonVisible;
    private boolean moveButtonExpanded;
    private final AnimatedFloat moveButtonAnimated = new AnimatedFloat(this, 0, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
    private final AnimatedFloat moveButtonExpandedAnimated = new AnimatedFloat(this, 0, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
    private final Runnable collapseMoveButton = () -> {
        if (moveButtonExpanded) {
            moveButtonExpanded = false;
            invalidate();
        }
    };

    public CaptionPhotoViewer3(
            Context context,
            FrameLayout rootView,
            SizeNotifierFrameLayout sizeNotifierFrameLayout,
            FrameLayout containerView,
            Theme.ResourcesProvider resourcesProvider,
            BlurringShader.BlurManager blurManager,
            Runnable applyCaption
    ) {
        super(context, rootView, sizeNotifierFrameLayout, containerView, resourcesProvider, blurManager);
        this.applyCaption = applyCaption;

        moveButtonText.setTextSize(dp(14));
        moveButtonText.setOverrideFullWidth(AndroidUtilities.displaySize.x);
        moveButtonText.setTextColor(0xFFFFFFFF);
        if (isAtTop()) {
            moveButtonText.setText(getString(R.string.MoveCaptionDown));
            moveButtonIcon = context.getResources().getDrawable(R.drawable.menu_link_below);
        } else {
            moveButtonText.setText(getString(R.string.MoveCaptionUp));
            moveButtonIcon = context.getResources().getDrawable(R.drawable.menu_link_above);
        }

        addPhotoButton = new ImageView(context);
        addPhotoButton.setImageResource(R.drawable.filled_add_photo);
        addPhotoButton.setScaleType(ImageView.ScaleType.CENTER);
        addPhotoButton.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        addPhotoButton.setBackground(Theme.createSelectorDrawable(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR, RIPPLE_MASK_CIRCLE_20DP, dp(18)));
        setAddPhotoVisible(false, false);
        addView(addPhotoButton, LayoutHelper.createFrame(44, 44, Gravity.LEFT | (isAtTop() ? Gravity.TOP : Gravity.BOTTOM), 14, isAtTop() ? 10 : 0, 0, isAtTop() ? 0 : 10));




        hint = new HintView2(context, isAtTop() ? HintView2.DIRECTION_TOP : HintView2.DIRECTION_BOTTOM);
        hint.setRounding(12);
        hint.setPadding(dp(12), dp(isAtTop() ? 8 : 0), dp(12), dp(isAtTop() ? 0 : 8));
        hint.setJoint(1, -21);
        hint.setMultilineText(true);
        addView(hint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 80, Gravity.RIGHT | (isAtTop() ? Gravity.TOP : Gravity.BOTTOM)));


        roundButton = new ImageView(context);
        roundButtonBounce = new ButtonBounce(roundButton);
        roundButton.setImageResource(R.drawable.input_video_story);
        roundButton.setBackground(Theme.createSelectorDrawable(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR, RIPPLE_MASK_CIRCLE_20DP, dp(18)));
        roundButton.setScaleType(ImageView.ScaleType.CENTER);
        addView(roundButton, LayoutHelper.createFrame(44, 44, Gravity.RIGHT | Gravity.BOTTOM, 0, 0, 11, 10));
        roundButton.setOnClickListener(e -> showRemoveRoundAlert());


        timerTextDrawable.setAnimationProperties(.16f, 0, 50, CubicBezierInterpolator.DEFAULT);
        timerTextDrawable.setTextSize(AndroidUtilities.dp(15));
        timerTextDrawable.setTypeface(AndroidUtilities.bold());
        timerTextDrawable.setText("0:00.0");
        timerTextDrawable.setTextColor(Color.WHITE);

        whitePaint.setColor(0xFFFFFFFF);
        roundPaint.setColor(0xFF1A9CFF);

        tinyWaveDrawable.minRadius = dp(47);
        tinyWaveDrawable.maxRadius = dp(55);
        tinyWaveDrawable.generateBlob();

        bigWaveDrawable.minRadius = dp(47);
        bigWaveDrawable.maxRadius = dp(55);
        bigWaveDrawable.generateBlob();

        roundDrawable = getContext().getResources().getDrawable(R.drawable.input_video_pressed).mutate();
        lockHandlePaint.setStyle(Paint.Style.STROKE);
    }


    public void setTimerVisible(boolean visible, boolean animated) {

    }

    public boolean hasTimer() {
        return false;
    }

    public void setTimer(int value) {

    }

    public void setOnTimerChange(Utilities.Callback<Integer> onTTLChange) {

    }

    public void setIsVideo(boolean isVideo) {

    }


    public void setPeriod(int period) {

    }

    public void setPeriodVisible(boolean visible) {

    }

    public void hidePeriodPopup() {

    }

    public void setOnPeriodUpdate(Utilities.Callback<Integer> listener) {

    }

    public void setOnPremiumHint(Utilities.Callback<Integer> listener) {

    }

    public void setHasRoundVideo(boolean hasRoundVideo) {
        roundButton.setImageResource(hasRoundVideo ? R.drawable.input_video_story_remove : R.drawable.input_video_story);
        this.hasRoundVideo = hasRoundVideo;
    }

    @Override
    protected int getEditTextStyle() {
        return EditTextEmoji.STYLE_PHOTOVIEWER;
    }

    @Override
    protected void onTextChange() {
        if (applyCaption != null) {
            applyCaption.run();
        }
    }

    public void setOnAddPhotoClick(View.OnClickListener listener) {
        addPhotoButton.setOnClickListener(listener);
    }

    public void setAddPhotoVisible(boolean visible, boolean animated) {
        addPhotoVisible = visible;
        addPhotoButton.animate().cancel();
        if (animated) {
            addPhotoButton.setVisibility(View.VISIBLE);
            addPhotoButton.animate().alpha(visible ? 1f : 0f).translationX(visible ? 0 : dp(-8)).withEndAction(() -> {
                if (!visible) {
                    addPhotoButton.setVisibility(View.GONE);
                }
            }).start();
        } else {
            addPhotoButton.setVisibility(visible ? View.VISIBLE : View.GONE);
            addPhotoButton.setAlpha(visible ? 1f : 0f);
            addPhotoButton.setTranslationX(visible ? 0 : dp(-8));
        }
        updateEditTextLeft();

        MarginLayoutParams lp = (MarginLayoutParams) editText.getLayoutParams();
        lp.rightMargin = dp(12);
        editText.setLayoutParams(lp);
    }

    @Override
    protected int getEditTextLeft() {
        return addPhotoVisible ? dp(31) : 0;
    }

    @Override
    protected void onEditHeightChange(int height) {
        hint.setTranslationY((-Math.min(dp(34), height) - dp(10)) * (isAtTop() ? -1.0f : 1.0f));
    }

    @Override
    protected boolean clipChild(View child) {
        return child != hint;
    }

    @Override
    public int getEditTextHeight() {
        return super.getEditTextHeight();
    }


    protected boolean showMoveButton() {
        return false;
    }

    public void setShowMoveButtonVisible(boolean visible, boolean animated) {
        if (moveButtonVisible == visible && animated) return;
        moveButtonVisible = visible;
        if (!animated) {
            moveButtonAnimated.set(visible, true);
        }
        invalidate();
    }

    protected void onMoveButtonClick() {

    }

    public void expandMoveButton() {
        AndroidUtilities.cancelRunOnUIThread(collapseMoveButton);
        moveButtonExpanded = MessagesController.getInstance(currentAccount).shouldShowMoveCaptionHint();
        if (moveButtonExpanded) {
            MessagesController.getInstance(currentAccount).incrementMoveCaptionHint();
            invalidate();
            AndroidUtilities.runOnUIThread(collapseMoveButton, 5000);
        }
    }

    protected void openedKeyboard() {
        expandMoveButton();
    }

    @Override
    public void updateKeyboard(int keyboardHeight) {
        final boolean wasOpen = super.toKeyboardShow;
        super.updateKeyboard(keyboardHeight);
        if (!wasOpen && keyboardNotifier.keyboardVisible()) {
            openedKeyboard();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        final float moveButtonAlpha = moveButtonAnimated.set(moveButtonVisible, !showMoveButton());
        final float moveButtonExpanded = moveButtonExpandedAnimated.set(this.moveButtonExpanded);
        if (moveButtonAlpha > 0.0f) {
            float s = moveButtonBounce.getScale(.03f);
            if (isAtTop()) {
                moveButtonBounds.set(dp(10), bounds.bottom + dp(10), dp(10 + 34) + (moveButtonText.getCurrentWidth() + dp(11)) * moveButtonExpanded, bounds.bottom + dp(10 + 32));
            } else {
                moveButtonBounds.set(dp(10), bounds.top - dp(32 + 10), dp(10 + 34) + (moveButtonText.getCurrentWidth() + dp(11)) * moveButtonExpanded, bounds.top - dp(10));
            }
            if (moveButtonAlpha < 1) {
                canvas.saveLayerAlpha(moveButtonBounds, (int) (0xFF * moveButtonAlpha), Canvas.ALL_SAVE_FLAG);
            } else {
                canvas.save();
            }
            canvas.scale(s, s, moveButtonBounds.centerX(), moveButtonBounds.centerY());
            canvas.clipRect(moveButtonBounds);
            float r = dpf2(8.33f);
            if (customBlur()) {
                drawBlur(backgroundBlur, canvas, moveButtonBounds, r, false, 0, 0, true, 1.0f);
                backgroundPaint.setAlpha((int) (lerp(0, 0x40, moveButtonAlpha)));
                canvas.drawRoundRect(moveButtonBounds, r, r, backgroundPaint);
            } else {
                Paint[] blurPaints = backgroundBlur.getPaints(moveButtonAlpha, 0, 0);
                if (blurPaints == null || blurPaints[1] == null) {
                    backgroundPaint.setAlpha(lerp(0, 0x80, moveButtonAlpha));
                    canvas.drawRoundRect(moveButtonBounds, r, r, backgroundPaint);
                } else {
                    if (blurPaints[0] != null) {
                        canvas.drawRoundRect(moveButtonBounds, r, r, blurPaints[0]);
                    }
                    if (blurPaints[1] != null) {
                        canvas.drawRoundRect(moveButtonBounds, r, r, blurPaints[1]);
                    }
                    backgroundPaint.setAlpha(lerp(0, 0x33, moveButtonAlpha));
                    canvas.drawRoundRect(moveButtonBounds, r, r, backgroundPaint);
                }
            }
            moveButtonIcon.setBounds((int) (moveButtonBounds.left + dp(9)), (int) (moveButtonBounds.centerY() - dp(9)), (int) (moveButtonBounds.left + dp(9 + 18)), (int) (moveButtonBounds.centerY() + dp(9)));
            moveButtonIcon.draw(canvas);
            moveButtonText.setBounds(moveButtonBounds.left + dp(34), moveButtonBounds.top, moveButtonBounds.right, moveButtonBounds.bottom);
            moveButtonText.setAlpha((int) (0xFF * moveButtonExpanded));
            moveButtonText.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    protected void beforeUpdateShownKeyboard(boolean show) {
        if (!show) {
            addPhotoButton.setVisibility(addPhotoVisible ? View.VISIBLE : View.GONE);
            roundButton.setVisibility(View.VISIBLE);
        }
        if (hint != null) {
            hint.hide();
        }
    }

    @Override
    protected void onUpdateShowKeyboard(float keyboardT) {
        addPhotoButton.setAlpha(1f - keyboardT);
        roundButton.setAlpha(1f - keyboardT);
    }

    @Override
    protected void afterUpdateShownKeyboard(boolean show) {
        addPhotoButton.setVisibility(!show && addPhotoVisible ? View.VISIBLE : View.GONE);
        roundButton.setVisibility(!show ? View.VISIBLE : View.GONE);
        if (show) {
            addPhotoButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected int additionalKeyboardHeight() {
        return 0;
    }

    @Override
    public void updateColors(Theme.ResourcesProvider resourcesProvider) {
        super.updateColors(resourcesProvider);

    }

    @Override
    protected void setupMentionContainer() {

    }

    @Override
    protected int getCaptionLimit() {
        return UserConfig.getInstance(currentAccount).isPremium() ? getCaptionPremiumLimit() : getCaptionDefaultLimit();
    }

    @Override
    protected int getCaptionDefaultLimit() {

        return MessagesController.getInstance(currentAccount).captionLengthLimitDefault;
    }

    @Override
    protected int getCaptionPremiumLimit() {

        return MessagesController.getInstance(currentAccount).captionLengthLimitPremium;
    }


    public boolean isRecording() {
        return recording;
    }

    public boolean stopRecording() {
        if (recording) {
            recordTouch = false;
            releaseRecord(false, false);
            return true;
        }
        return false;
    }

    public boolean canRecord() {
        return false;
    }

    public void putRecorder(PhotoViewerRoundVideoRecorder recorder) {

    }

    public void showRemoveRoundAlert() {
        if (!hasRoundVideo) return;
        AlertDialog d = new AlertDialog.Builder(getContext(), resourcesProvider)
                .setTitle(LocaleController.getString(R.string.StoryRemoveRoundTitle))
                .setMessage(LocaleController.getString(R.string.StoryRemoveRoundMessage))
                .setPositiveButton(LocaleController.getString(R.string.Remove), (di, w) -> removeRound())
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .show();
        TextView button = (TextView) d.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold, resourcesProvider));
        }
    }

    public void removeRound() {

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        if (recording && currentRecorder != null && currentRecorder.cameraView != null && flipButton != null) {
            AndroidUtilities.rectTmp.set(flipButton.getBounds());
            AndroidUtilities.rectTmp.inset(-dp(12), -dp(12));
            for (int i = 0; i < ev.getPointerCount(); ++i) {
                if (AndroidUtilities.rectTmp.contains(ev.getX(i), ev.getY(i))) {
                    if (ev.getAction() == MotionEvent.ACTION_DOWN || ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                        currentRecorder.cameraView.switchCamera();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && flipButton instanceof AnimatedVectorDrawable) {
                            ((AnimatedVectorDrawable) flipButton).start();
                        }
                    }
                    if (!recordTouch) {
                        return true;
                    }
                    break;
                }
            }
        }

        AndroidUtilities.rectTmp.set(roundButton.getX(), roundButton.getY(), roundButton.getX() + roundButton.getMeasuredWidth(), roundButton.getY() + roundButton.getMeasuredHeight());
        if (recordTouch || !hasRoundVideo && !keyboardShown && AndroidUtilities.rectTmp.contains(ev.getX(), ev.getY())) {
            return roundButtonTouchEvent(ev);
        }
        if (recording && locked && cancelBounds.contains(ev.getX(), ev.getY())) {
            releaseRecord(false, true);
            recordTouch = false;
            return true;
        }
        if (recording && (lockBounds.contains(ev.getX(), ev.getY()) || getBounds().contains(ev.getX(), ev.getY()))) {
            releaseRecord(false, false);
            recordTouch = false;
            return true;
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            moveButtonBounce.setPressed(moveButtonBounds.contains(ev.getX(), ev.getY()));
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            if (moveButtonBounce.isPressed() && !moveButtonBounds.contains(ev.getX(), ev.getY())) {
                moveButtonBounce.setPressed(false);
            }
        } else if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            if (moveButtonBounce.isPressed()) {
                if (ev.getAction() == MotionEvent.ACTION_UP) {
                    onMoveButtonClick();
                    moveButtonText.setText(getString(isAtTop() ? R.string.MoveCaptionDown : R.string.MoveCaptionUp), true);
                }
                moveButtonBounce.setPressed(false);
                return true;
            }
        }
        return moveButtonBounce.isPressed() || super.dispatchTouchEvent(ev);
    }

    private boolean roundButtonTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (stopRecording()) {
                return true;
            }
            recordTouch = true;
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            if (!canRecord()) {
                return true;
            }
            AndroidUtilities.cancelRunOnUIThread(doneCancel);
            fromX = ev.getX();
            fromY = ev.getY();
            amplitude = 0;
            slideProgress = 0f;
            cancelT.set(0, true);
            cancel2T.set(0, true);
            cancelling = false;
            stopping = false;
            locked = false;
            recordPaint.reset();
            recording = true;
            startTime = System.currentTimeMillis();
            setCollapsed(true, Integer.MAX_VALUE);
            invalidateDrawOver2();

            putRecorder(currentRecorder = new PhotoViewerRoundVideoRecorder(getContext()) {
                @Override
                protected void receivedAmplitude(double amplitude) {
                    setAmplitude(amplitude);
                }

                @Override
                public void stop() {
                    super.stop();
                    if (recording) {
                        releaseRecord(true, false);
                    }
                }
            });
            return true;
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            if (!cancelling) {
                slideProgress = Utilities.clamp((fromX - ev.getX()) / (getWidth() * .35f), 1, 0);
                lockProgress = Utilities.clamp((fromY - ev.getY()) / (getWidth() * .3f), 1, 0);

                if (!locked && !cancelling && slideProgress >= 1) {
                    cancelling = true;
                    recording = false;
                    roundButton.setVisibility(INVISIBLE);
                    recordPaint.playDeleteAnimation();

                    if (currentRecorder != null) {
                        currentRecorder.cancel();
                    }

                    AndroidUtilities.runOnUIThread(doneCancel, 800);
                } else if (!locked && !cancelling && lockProgress >= 1 && slideProgress < .4f) {
                    locked = true;

                    try {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                    } catch (Exception ignore) {
                    }
                }
                invalidate();
                invalidateDrawOver2();
            }
        } else if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            if (!cancelling && !locked) {
                releaseRecord(false, false);
            }
            recordTouch = false;
        }
        return recordTouch;
    }

    private final Runnable doneCancel = () -> {
        setCollapsed(false, Integer.MIN_VALUE);
        roundButton.setVisibility(VISIBLE);
        addPhotoButton.setVisibility(addPhotoVisible ? VISIBLE : GONE);
    };

    private void releaseRecord(boolean byRecorder, boolean cancel) {
        AndroidUtilities.cancelRunOnUIThread(doneCancel);

        stopping = true;
        recording = false;
        setCollapsed(false, (int) (getBounds().right - dp(20) - (getWidth() * .35f) * slideProgress));

        if (currentRecorder != null) {
            if (!byRecorder) {
                if (cancel) {
                    currentRecorder.cancel();
                } else {
                    currentRecorder.stop();
                }
            }
            currentRecorder = null;
        }
        invalidateDrawOver2();
    }

    public void setAmplitude(double value) {
        amplitude = (float) (Math.min(WaveDrawable.MAX_AMPLITUDE, value) / WaveDrawable.MAX_AMPLITUDE);
        invalidate();
    }

    private void checkFlipButton() {
        if (flipButton != null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            flipButton = (AnimatedVectorDrawable) ContextCompat.getDrawable(getContext(), R.drawable.avd_flip);
        } else {
            flipButton = getContext().getResources().getDrawable(R.drawable.vd_flip).mutate();
        }
    }

    @Override
    public void drawOver(Canvas canvas, RectF bounds) {
        if (currentRecorder != null) {
            final float cancel = cancelT.set(cancelling);
            final float lock = lockT.set(locked);

            if (startTime <= 0) startTime = System.currentTimeMillis();
            final float wobble = (1f + (float) Math.sin((System.currentTimeMillis() - startTime) / 900f * Math.PI)) / 2f;

            final float rcx = bounds.left + dp(21), rcy = bounds.bottom - dp(20);
            recordPaint.setBounds(
                    (int) (rcx - dp(12)),
                    (int) (rcy - dp(12)),
                    (int) (rcx + dp(12)),
                    (int) (rcy + dp(12))
            );
            recordPaint.draw(canvas);

            timerTextDrawable.setBounds((int) (bounds.left + dp(33.3f) - dp(10) * cancel), (int) (bounds.bottom - dp(20) - dp(9)), (int) (bounds.left + dp(33.3f + 100)), (int) (bounds.bottom - dp(20) + dp(9)));
            if (currentRecorder != null) {
                timerTextDrawable.setText(currentRecorder.sinceRecordingText());
            }
            timerTextDrawable.setAlpha((int) (0xFF * (1f - cancel)));
            timerTextDrawable.draw(canvas);

            final float slideToCancelAlpha = (1f - slideProgress) * (1f - lock);
            final float cancelAlpha = lock;

            final Paint blurPaint = captionBlur.getPaint(1f);
            if (blurPaint != null) {
                canvas.saveLayerAlpha(bounds.left, bounds.top, bounds.right, bounds.bottom, 0xff, Canvas.ALL_SAVE_FLAG);
            }

            if (slideToCancelAlpha > 0) {
                if (slideToCancelText == null) {
                    slideToCancelText = new Text(LocaleController.getString(R.string.SlideToCancel2), 15);
                }
                if (slideToCancelArrowPath == null) {
                    slideToCancelArrowPath = new Path();
                    slideToCancelArrowPath.moveTo(dp(3.83f), 0);
                    slideToCancelArrowPath.lineTo(0, dp(5));
                    slideToCancelArrowPath.lineTo(dp(3.83f), dp(10));

                    slideToCancelArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    slideToCancelArrowPaint.setStyle(Paint.Style.STROKE);
                    slideToCancelArrowPaint.setStrokeCap(Paint.Cap.ROUND);
                    slideToCancelArrowPaint.setStrokeJoin(Paint.Join.ROUND);
                }

                slideToCancelArrowPaint.setStrokeWidth(dp(1.33f));

                slideToCancelText.ellipsize((int) (bounds.width() - dp(5 + 21 + 16 + 10 + 64) - timerTextDrawable.getCurrentWidth()));
                final float width = dp(5 + 6.33f) + slideToCancelText.getWidth();
                final float x = bounds.centerX() - width / 2f - (bounds.width() / 6f) * lerp(slideProgress, 1f, lock) - wobble * dp(6) * (1f - slideProgress);

                int color = blurPaint != null ? 0xffffffff : 0x80ffffff;
                color = Theme.multAlpha(color, slideToCancelAlpha);

                canvas.save();
                canvas.translate(x, bounds.centerY() - dp(5));
                slideToCancelArrowPaint.setColor(color);
                canvas.drawPath(slideToCancelArrowPath, slideToCancelArrowPaint);
                canvas.restore();
                slideToCancelText.draw(canvas, x + dp(5 + 6.33f), bounds.centerY(), color, 1f);
            }

            if (cancelAlpha > 0) {
                if (cancelText == null) {
                    cancelText = new Text(LocaleController.getString(R.string.CancelRound), 15, AndroidUtilities.bold());
                }

                cancelText.ellipsize((int) (bounds.width() - dp(5 + 21 + 16 + 10 + 64) - timerTextDrawable.getCurrentWidth()));
                final float x = bounds.centerX() - cancelText.getWidth() / 2f + (bounds.width() / 4f) * (1f - cancelAlpha);

                int color = blurPaint != null ? 0xffffffff : 0x80ffffff;
                color = Theme.multAlpha(color, cancelAlpha);
                cancelText.draw(canvas, x, bounds.centerY(), color, 1f);
                cancelBounds.set(x - dp(12), bounds.top, x + cancelText.getWidth() + dp(12), bounds.bottom);
            }

            if (blurPaint != null) {
                canvas.drawRect(bounds, blurPaint);
                canvas.restore();
            }

            invalidate();
        }
    }

    @Override
    public void drawOver2(Canvas canvas, RectF bounds, float alpha) {
        if (alpha <= 0) {
            return;
        }

        final float cancel = cancel2T.set(cancelling);
        final float lock = lock2T.set(locked);
        final float amplitude = animatedAmplitude.set(this.amplitude);

        final float radius = (dp(41) + dp(30) * amplitude * (1f - slideProgress)) * (1f - cancel) * alpha;
        final float cx = lerp(bounds.right - dp(20) - (getWidth() * .35f) * slideProgress * (1f - lock), bounds.left + dp(20), cancel);
        final float cy = bounds.bottom - dp(20);

        if (LiteMode.isEnabled(LiteMode.FLAGS_CHAT)) {
            tinyWaveDrawable.minRadius = dp(47);
            tinyWaveDrawable.maxRadius = dp(47) + dp(15) * BlobDrawable.FORM_SMALL_MAX;

            bigWaveDrawable.minRadius = dp(50);
            bigWaveDrawable.maxRadius = dp(50) + dp(12) * BlobDrawable.FORM_BIG_MAX;

            bigWaveDrawable.update(amplitude, 1.01f);
            tinyWaveDrawable.update(amplitude, 1.02f);

            bigWaveDrawable.paint.setColor(Theme.multAlpha(roundPaint.getColor(), WaveDrawable.CIRCLE_ALPHA_2 * alpha));
            canvas.save();
            final float s1 = radius / bigWaveDrawable.minRadius;
            canvas.scale(s1, s1, cx, cy);
            bigWaveDrawable.draw(cx, cy, canvas, bigWaveDrawable.paint);
            canvas.restore();

            tinyWaveDrawable.paint.setColor(Theme.multAlpha(roundPaint.getColor(), WaveDrawable.CIRCLE_ALPHA_1 * alpha));
            canvas.save();
            final float s2 = radius / tinyWaveDrawable.minRadius;
            canvas.scale(s2, s2, cx, cy);
            tinyWaveDrawable.draw(cx, cy, canvas, tinyWaveDrawable.paint);
            canvas.restore();
        }

        final float R = Math.min(radius, dp(41 + 14));
        roundPaint.setAlpha((int) (0xFF * alpha));
        canvas.drawCircle(cx, cy, R, roundPaint);

        canvas.save();
        circlePath.rewind();
        circlePath.addCircle(cx, cy, R, Path.Direction.CW);
        canvas.clipPath(circlePath);
        roundDrawable.setBounds(
                (int) (cx - roundDrawable.getIntrinsicWidth() / 2f * (1f - cancel) * (stopping ? alpha : 1f)),
                (int) (cy - roundDrawable.getIntrinsicHeight() / 2f * (1f - cancel) * (stopping ? alpha : 1f)),
                (int) (cx + roundDrawable.getIntrinsicWidth() / 2f * (1f - cancel) * (stopping ? alpha : 1f)),
                (int) (cy + roundDrawable.getIntrinsicHeight() / 2f * (1f - cancel) * (stopping ? alpha : 1f))
        );
        roundDrawable.setAlpha((int) (0xFF * (1f - cancel) * (stopping ? alpha : 1f)));
        roundDrawable.draw(canvas);
        if (lock > 0) {
            final float sz = dpf2(19.33f) / 2f * lock * alpha;
            AndroidUtilities.rectTmp.set(cx - sz, cy - sz, cx + sz, cy + sz);
            canvas.drawRoundRect(AndroidUtilities.rectTmp, dp(5.33f), dp(5.33f), whitePaint);
        }
        canvas.restore();

        drawLock(canvas, bounds, alpha);

        checkFlipButton();
        flipButton.setAlpha((int) (0xFF * alpha * (1f - cancel)));
        final int timelineHeight = getTimelineHeight();
        flipButton.setBounds((int) bounds.left + dp(4), (int) (bounds.top - timelineHeight - dp(36 + 12)), (int) (bounds.left + dp(4 + 36)), (int) (bounds.top - timelineHeight - dp(12)));
        flipButton.draw(canvas);
    }

    public int getTimelineHeight() {
        return 0;
    }

    private void drawLock(Canvas canvas, RectF bounds, float alpha) {
        final float cancel = cancel2T.get();
        final float lock = lock2T.get();

        final float scale = lerp(lockCancelledT.set(slideProgress < .4f), 0f, lock) * (1f - cancel) * alpha;

        final float w = scale * dp(36), h = scale * lerp(dp(50), dp(36), lock);
        final float cx = bounds.right - dp(20);
        final float cy = lerp(
                bounds.bottom - dp(20 + 60) - h / 2f - dp(120) * lockProgress * (1f - lock),
                bounds.bottom - dp(20),
                1f - scale
        );
        lockBounds.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);

        final float r = lerp(dp(18), dp(14), lock);
        lockShadowPaint.setShadowLayer(dp(1), 0, dp(.66f), Theme.multAlpha(0x20000000, scale));
        lockShadowPaint.setColor(0);
        canvas.drawRoundRect(lockBounds, r, r, lockShadowPaint);

        Paint backgroundBlurPaint = backgroundBlur.getPaint(scale);
        if (backgroundBlurPaint == null) {
            lockBackgroundPaint.setColor(0x40000000);
            lockBackgroundPaint.setAlpha((int) (0x40 * scale));
            canvas.drawRoundRect(lockBounds, r, r, lockBackgroundPaint);
        } else {
            canvas.drawRoundRect(lockBounds, r, r, backgroundBlurPaint);
            backgroundPaint.setAlpha((int) (0x33 * scale));
            canvas.drawRoundRect(lockBounds, r, r, backgroundPaint);
        }

        canvas.save();
        canvas.scale(scale, scale, cx, cy);

        lockPaint.setColor(Theme.multAlpha(0xFFFFFFFF, scale));
        lockHandlePaint.setColor(Theme.multAlpha(0xFFFFFFFF, scale * (1f - lock)));

        final float lockRectW = lerp(dp(15.33f), dp(13), lock);
        final float lockRectH = lerp(dp(12.66f), dp(13), lock);
        final float lockRectY = cy + dp(4) * (1f - lock);
        canvas.rotate(12 * lockProgress * (1f - lock), cx, lockRectY);

        lockRect.set(cx - lockRectW / 2f, lockRectY - lockRectH / 2f, cx + lockRectW / 2f, lockRectY + lockRectH / 2f);
        canvas.drawRoundRect(lockRect, dp(3.66f), dp(3.66f), lockPaint);

        if (lock < 1) {
            canvas.save();
            canvas.rotate(12 * lockProgress * (1f - lock), cx, lockRectY - lockRectH / 2f);
            canvas.translate(0, lockRectH / 2f * lock);
            canvas.scale(1f - lock, 1f - lock, cx, lockRectY - lockRectH / 2f);

            lockHandle.rewind();
            final float radius = dp(4.33f);
            final float y = lockRectY - lockRectH / 2f - dp(3.66f);
            lockHandle.moveTo(cx + radius, y + dp(3.66f));
            lockHandle.lineTo(cx + radius, y);
            AndroidUtilities.rectTmp.set(cx - radius, y - radius, cx + radius, y + radius);
            lockHandle.arcTo(AndroidUtilities.rectTmp, 0, -180, false);
            lockHandle.lineTo(cx - radius, y + dp(3.66f) * lerp(lerp(.4f, 0, lockProgress), 1f, lock));

            lockHandlePaint.setStrokeWidth(dp(2));
            canvas.drawPath(lockHandle, lockHandlePaint);
            canvas.restore();
        }

        canvas.restore();
    }

    private void setAmplitude(float value) {
        amplitude = value;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        recordPaint.attach();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        recordPaint.detach();
    }

    private class RecordDot extends Drawable {

        private final Paint redDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float alpha;
        private float alpha2 = 1f;
        private long lastUpdateTime;
        private boolean isIncr;
        boolean attachedToWindow;
        boolean playing;
        RLottieDrawable drawable;
        private boolean enterAnimation;

        private final View parent;

        public void attach() {
            attachedToWindow = true;
            if (playing) {
                drawable.start();
            }
            drawable.setMasterParent(parent);
        }

        public void detach() {
            attachedToWindow = false;
            drawable.stop();
            drawable.setMasterParent(null);
        }

        public RecordDot(View parent) {
            this.parent = parent;
            int resId = R.raw.chat_audio_record_delete_3;
            drawable = new RLottieDrawable(resId, "" + resId, AndroidUtilities.dp(28), AndroidUtilities.dp(28), false, null);
            drawable.setCurrentParentView(parent);
            drawable.setInvalidateOnProgressSet(true);
            updateColors();
        }

        public void updateColors() {
            int dotColor = 0xffDB4646;
            redDotPaint.setColor(dotColor);
            drawable.beginApplyLayerColors();
            drawable.setLayerColor("Cup Red.**", dotColor);
            drawable.setLayerColor("Box.**", dotColor);
            drawable.commitApplyLayerColors();
        }

        public void resetAlpha() {
            alpha = 1.0f;
            lastUpdateTime = System.currentTimeMillis();
            isIncr = false;
            playing = false;
            drawable.stop();
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            if (playing) {
                drawable.setAlpha((int) (255 * alpha * alpha2));
            }
            redDotPaint.setAlpha((int) (255 * alpha * alpha2));

            long dt = (System.currentTimeMillis() - lastUpdateTime);
            if (enterAnimation) {
                alpha = 1;
            } else {
                if (!isIncr && !playing) {
                    alpha -= dt / 600.0f;
                    if (alpha <= 0) {
                        alpha = 0;
                        isIncr = true;
                    }
                } else {
                    alpha += dt / 600.0f;
                    if (alpha >= 1) {
                        alpha = 1;
                        isIncr = false;
                    }
                }
            }
            lastUpdateTime = System.currentTimeMillis();
            drawable.setBounds(getBounds());
            if (playing) {
                drawable.draw(canvas);
            }
            if (!playing || !drawable.hasBitmap()) {
                canvas.drawCircle(getBounds().centerX(), getBounds().centerY(), dp(5), redDotPaint);
            }
            invalidateSelf();
        }

        @Override
        public void setAlpha(int alpha) {
            alpha2 = (float) alpha / 0xFF;
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSPARENT;
        }

        public void playDeleteAnimation() {
            playing = true;
            drawable.setProgress(0);
            if (attachedToWindow) {
                drawable.start();
            }
        }

        public void reset() {
            playing = false;
            drawable.stop();
            drawable.setProgress(0);
        }
    }
}
