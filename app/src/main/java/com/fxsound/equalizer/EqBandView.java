package com.fxsound.equalizer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * EqBandView — A vertical EQ band slider with animated thumb and gain display.
 *
 * Styled to match FxSound's dark glassmorphism aesthetic.
 */
public class EqBandView extends LinearLayout {

    public interface OnGainChangedListener {
        void onChanged(int bandIndex, int gain);
    }

    private static final int TRACK_COLOR = Color.parseColor("#2D2D44");
    private static final int FILL_COLOR_TOP = Color.parseColor("#00E5FF");
    private static final int FILL_COLOR_BOT = Color.parseColor("#00796B");
    private static final int THUMB_COLOR = Color.parseColor("#FFFFFF");

    private int minLevel = -1500;
    private int maxLevel = 1500;
    private int currentLevel = 0;
    private final int bandIndex;

    private Paint trackPaint;
    private Paint fillPaint;
    private Paint thumbPaint;
    private Paint glowPaint;

    private View sliderView;
    private TextView tvLabel;
    private TextView tvGain;

    private OnGainChangedListener listener;
    private ValueAnimator gainAnimator;

    public EqBandView(Context context, String label, int bandIndex) {
        super(context);
        this.bandIndex = bandIndex;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setPadding(8, 8, 8, 8);

        setupPaints();
        buildViews(context, label);
    }

    private void setupPaints() {
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(TRACK_COLOR);
        trackPaint.setStyle(Paint.Style.FILL);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setColor(THUMB_COLOR);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(FILL_COLOR_TOP);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(10f,
                android.graphics.BlurMaskFilter.Blur.OUTER));
    }

    private void buildViews(Context context, String label) {
        // Gain label (top)
        tvGain = new TextView(context);
        tvGain.setText("0.0");
        tvGain.setTextColor(Color.parseColor("#00E5FF"));
        tvGain.setTextSize(10f);
        tvGain.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams gainParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 40);
        addView(tvGain, gainParams);

        // Custom slider view
        sliderView = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                drawSlider(canvas);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return handleTouch(event);
            }
        };
        sliderView.setClickable(true);
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(40, 0, 1f);
        addView(sliderView, sliderParams);

        // Frequency label (bottom)
        tvLabel = new TextView(context);
        tvLabel.setText(label);
        tvLabel.setTextColor(Color.parseColor("#8888AA"));
        tvLabel.setTextSize(9f);
        tvLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 50);
        addView(tvLabel, labelParams);
    }

    private void drawSlider(Canvas canvas) {
        float w = sliderView.getWidth();
        float h = sliderView.getHeight();
        float cx = w / 2f;
        float trackW = w * 0.35f;
        float r = trackW / 2f;

        float center = h / 2f;
        float range = maxLevel - minLevel;
        float normalizedPos = (currentLevel - minLevel) / range; // 0=bottom, 1=top
        float thumbY = h - normalizedPos * h; // invert: top = high gain

        // Track background
        canvas.drawRoundRect(
                cx - r, 0, cx + r, h,
                r, r, trackPaint);

        // Fill from center (0dB line) to thumb
        float zeroY = h / 2f; // center = 0dB
        float fillTop = Math.min(thumbY, zeroY);
        float fillBot = Math.max(thumbY, zeroY);
        if (fillBot > fillTop) {
            LinearGradient fillGrad = new LinearGradient(
                    cx - r, fillTop, cx + r, fillBot,
                    FILL_COLOR_TOP, FILL_COLOR_BOT, Shader.TileMode.CLAMP);
            fillPaint.setShader(fillGrad);
            canvas.drawRoundRect(cx - r, fillTop, cx + r, fillBot, r / 2f, r / 2f, fillPaint);
        }

        // Thumb
        canvas.drawCircle(cx, thumbY, r * 1.4f, thumbPaint);
        // Thumb glow
        if (Math.abs(currentLevel) > 200) {
            glowPaint.setAlpha((int) Math.min(255, Math.abs(currentLevel) / 6));
            canvas.drawCircle(cx, thumbY, r * 1.4f, glowPaint);
        }

        // 0dB center line marker
        Paint centerLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerLine.setColor(Color.parseColor("#44FFFFFF"));
        centerLine.setStrokeWidth(1f);
        canvas.drawLine(cx - r * 2, zeroY, cx + r * 2, zeroY, centerLine);
    }

    private boolean handleTouch(MotionEvent event) {
        float h = sliderView.getHeight();
        float y = event.getY();
        y = Math.max(0, Math.min(h, y));

        float normalizedPos = 1f - (y / h); // 0=bottom, 1=top
        int newLevel = (int) (minLevel + normalizedPos * (maxLevel - minLevel));
        newLevel = Math.max(minLevel, Math.min(maxLevel, newLevel));

        currentLevel = newLevel;
        updateGainLabel();
        sliderView.invalidate();

        if (listener != null) listener.onChanged(bandIndex, currentLevel);
        return true;
    }

    private void updateGainLabel() {
        float db = currentLevel / 100f;
        tvGain.setText(String.format("%+.1f", db));
        tvGain.setTextColor(currentLevel > 0
                ? Color.parseColor("#00E5FF")
                : currentLevel < 0 ? Color.parseColor("#FF7043") : Color.parseColor("#888888"));
    }

    public void setRange(int min, int max, int current) {
        this.minLevel = min;
        this.maxLevel = max;
        this.currentLevel = current;
        updateGainLabel();
        if (sliderView != null) sliderView.invalidate();
    }

    public void animateTo(int targetLevel) {
        if (gainAnimator != null) gainAnimator.cancel();
        gainAnimator = ValueAnimator.ofInt(currentLevel, targetLevel);
        gainAnimator.setDuration(400);
        gainAnimator.addUpdateListener(anim -> {
            currentLevel = (int) anim.getAnimatedValue();
            updateGainLabel();
            if (sliderView != null) sliderView.invalidate();
        });
        gainAnimator.start();
    }

    public void setOnGainChangedListener(OnGainChangedListener listener) {
        this.listener = listener;
    }
}
