package com.fxsound.equalizer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * SpriteSheetButton — Android implementation of FxSound's sprite-sheet animated toggle.
 *
 * Evidence source: Resources 10109 (Button template) & 10108 (CheckBox template)
 * from fxsound_setup.exe .rsrc section.
 *
 * FxSound mechanism (observed in WinUI XAML):
 *  - Two overlapping Rectangle views filled with ImageBrush (the sprite sheet).
 *  - TranslateTransform shifts X offset to reveal the correct state frame.
 *  - OverlayRectangle cross-fades from opacity 0→1 in 500ms to dissolve between states.
 *
 * Android translation:
 *  - Two Paint objects representing base and overlay state, cross-faded via ValueAnimator.
 *  - Gradient rectangles simulate state colors (Normal, Hover/Pressed, Checked).
 *  - 500ms alpha animation matches the WinUI DoubleAnimation duration exactly.
 *  - Touch scale feedback (subtle 95% scale on press) replicates PointerOver/Pressed feel.
 */
public class SpriteSheetButton extends View {

    // Animation duration matching WinUI "Duration=0:0:0.5" (500ms)
    private static final long ANIMATION_DURATION_MS = 500;

    public interface OnStateChangedListener {
        void onStateChanged(boolean isOn);
    }

    // State
    private boolean isChecked = false;
    private boolean isPressed = false;
    private float overlayAlpha = 0f; // mirrors WinUI OverlayRectangle.Opacity

    // Animators
    private ValueAnimator overlayAnimator;
    private ValueAnimator scaleAnimator;
    private float currentScale = 1.0f;

    // Colors (FxSound dark theme)
    private static final int COLOR_OFF_BASE     = Color.parseColor("#1A1A2E");
    private static final int COLOR_OFF_GRADIENT = Color.parseColor("#16213E");
    private static final int COLOR_ON_BASE      = Color.parseColor("#00BCD4");
    private static final int COLOR_ON_GRADIENT  = Color.parseColor("#0097A7");
    private static final int COLOR_HOVER        = Color.parseColor("#26C6DA");
    private static final int COLOR_BORDER_OFF   = Color.parseColor("#2D2D44");
    private static final int COLOR_BORDER_ON    = Color.parseColor("#00E5FF");
    private static final int COLOR_TEXT         = Color.WHITE;

    private Paint basePaint;
    private Paint overlayPaint;
    private Paint borderPaint;
    private Paint textPaint;
    private Paint glowPaint;

    private String labelText = "";
    private OnStateChangedListener listener;

    public SpriteSheetButton(Context context) {
        super(context);
        init();
    }

    public SpriteSheetButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        // Read custom attributes if any
    }

    public SpriteSheetButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(COLOR_TEXT);
        textPaint.setTextSize(36f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(4f);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(12f,
                android.graphics.BlurMaskFilter.Blur.OUTER));

        setClickable(true);
    }

    public void setLabel(String label) {
        this.labelText = label;
        invalidate();
    }

    public void setChecked(boolean checked) {
        boolean wasChecked = isChecked;
        isChecked = checked;
        if (wasChecked != checked) {
            animateStateTransition(checked);
        }
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        this.listener = listener;
    }

    /**
     * Animate the sprite-sheet dissolve transition.
     * Mirrors: DoubleAnimation on OverlayRectangle.Opacity From=0 To=1, Duration=0:0:0.5
     */
    private void animateStateTransition(boolean toChecked) {
        if (overlayAnimator != null) overlayAnimator.cancel();

        float startAlpha = overlayAlpha;
        float endAlpha = toChecked ? 1f : 0f;

        overlayAnimator = ValueAnimator.ofFloat(startAlpha, endAlpha);
        overlayAnimator.setDuration(ANIMATION_DURATION_MS);
        overlayAnimator.setInterpolator(new DecelerateInterpolator());
        overlayAnimator.addUpdateListener(anim -> {
            overlayAlpha = (float) anim.getAnimatedValue();
            invalidate();
        });
        overlayAnimator.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                animateScale(0.95f);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                isPressed = false;
                animateScale(1.0f);
                // Toggle state
                isChecked = !isChecked;
                animateStateTransition(isChecked);
                if (listener != null) listener.onStateChanged(isChecked);
                return true;

            case MotionEvent.ACTION_CANCEL:
                isPressed = false;
                animateScale(1.0f);
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void animateScale(float toScale) {
        if (scaleAnimator != null) scaleAnimator.cancel();
        scaleAnimator = ValueAnimator.ofFloat(currentScale, toScale);
        scaleAnimator.setDuration(120);
        scaleAnimator.addUpdateListener(anim -> {
            currentScale = (float) anim.getAnimatedValue();
            setScaleX(currentScale);
            setScaleY(currentScale);
        });
        scaleAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float radius = h / 3f;
        float padding = 4f;

        // --- Layer 1: Base state (OFF / unchecked frame — mirrors SpriteSheetOffset base position) ---
        LinearGradient baseGradient = new LinearGradient(0, 0, w, h,
                COLOR_OFF_BASE, COLOR_OFF_GRADIENT, Shader.TileMode.CLAMP);
        basePaint.setShader(baseGradient);
        canvas.drawRoundRect(padding, padding, w - padding, h - padding, radius, radius, basePaint);

        // --- Layer 2: Overlay state (ON / checked frame — mirrors OverlayRectangle) ---
        // overlayAlpha is animated from 0→1 exactly as WinUI's Opacity animation
        if (overlayAlpha > 0f) {
            LinearGradient overlayGradient = new LinearGradient(0, 0, w, h,
                    COLOR_ON_BASE, COLOR_ON_GRADIENT, Shader.TileMode.CLAMP);
            overlayPaint.setShader(overlayGradient);
            overlayPaint.setAlpha((int) (overlayAlpha * 255));
            canvas.drawRoundRect(padding, padding, w - padding, h - padding, radius, radius, overlayPaint);
        }

        // --- Border (changes with state, like WinUI BorderBrush animation) ---
        if (isPressed) {
            borderPaint.setColor(COLOR_HOVER);
            borderPaint.setAlpha(255);
        } else if (isChecked) {
            int borderAlpha = (int) (overlayAlpha * 255);
            borderPaint.setColor(COLOR_BORDER_ON);
            borderPaint.setAlpha(borderAlpha);
            // Glow effect when ON
            glowPaint.setColor(COLOR_BORDER_ON);
            glowPaint.setAlpha((int) (overlayAlpha * 180));
            canvas.drawRoundRect(padding, padding, w - padding, h - padding, radius, radius, glowPaint);
        } else {
            borderPaint.setColor(COLOR_BORDER_OFF);
            borderPaint.setAlpha(255);
        }
        canvas.drawRoundRect(padding, padding, w - padding, h - padding, radius, radius, borderPaint);

        // --- Label text ---
        if (!labelText.isEmpty()) {
            textPaint.setAlpha(isChecked ? 255 : 180);
            canvas.drawText(labelText, w / 2f, h / 2f + textPaint.getTextSize() / 3f, textPaint);
        }
    }
}
