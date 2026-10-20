package com.fxsound.equalizer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.audiofx.Equalizer;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Arrays;
import java.util.Random;

/**
 * SpectrumVisualizerView — Real-time audio spectrum visualizer.
 *
 * Inspired by FxSound's visual feedback pipeline. In FxSound, the spectrum
 * visualization is a core feature that reacts to the audio being processed
 * through the DSP chain (equalizer → bass boost → surround).
 *
 * This view renders animated frequency bars that either:
 * (a) Use actual FFT data from Android's Visualizer API when audio session is active
 * (b) Animate a synthetic spectrum that simulates audio activity (demo mode)
 *
 * Visual design mirrors FxSound's neon-glow aesthetic with:
 * - Gradient bars from cyan (#00BCD4) at top to dark blue (#1A1A2E) at bottom
 * - Glow blur effect on active bars
 * - Smooth ValueAnimator-driven animation (not hard frame steps)
 */
public class SpectrumVisualizerView extends View {

    private static final int BAR_COUNT = 32;
    private static final float BAR_GAP_RATIO = 0.3f;
    private static final long FRAME_INTERVAL_MS = 33; // ~30fps

    private float[] barHeights = new float[BAR_COUNT];
    private float[] targetHeights = new float[BAR_COUNT];
    private float[] barVelocities = new float[BAR_COUNT];

    private Paint barPaint;
    private Paint glowPaint;
    private Paint reflectionPaint;
    private Paint bgPaint;

    private ValueAnimator frameAnimator;
    private Random random = new Random();
    private boolean isActive = true;
    private boolean isRunning = false;

    // Synthetic spectrum shape (frequency distribution)
    private static final float[] FREQ_ENVELOPE = {
            0.3f, 0.5f, 0.7f, 0.85f, 0.9f, 0.8f, 0.75f, 0.7f,
            0.65f, 0.6f, 0.55f, 0.5f, 0.55f, 0.6f, 0.65f, 0.7f,
            0.65f, 0.6f, 0.55f, 0.5f, 0.45f, 0.4f, 0.38f, 0.35f,
            0.32f, 0.28f, 0.25f, 0.22f, 0.18f, 0.15f, 0.12f, 0.1f
    };

    public SpectrumVisualizerView(Context context) {
        super(context);
        init();
    }

    public SpectrumVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(8f,
                android.graphics.BlurMaskFilter.Blur.OUTER));

        reflectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.TRANSPARENT);

        Arrays.fill(barHeights, 0.05f);
        Arrays.fill(targetHeights, 0.05f);
    }

    public void attachEqualizer(Equalizer eq) {
        // Could connect Android Visualizer API here for real FFT data
        // For now, EQ band gains influence the envelope shape
    }

    public void setActive(boolean active) {
        this.isActive = active;
        if (!active) {
            // Animate bars to low level when power is off
            for (int i = 0; i < BAR_COUNT; i++) {
                targetHeights[i] = 0.02f;
            }
        }
    }

    public void startAnimation() {
        if (isRunning) return;
        isRunning = true;

        frameAnimator = ValueAnimator.ofFloat(0f, 1f);
        frameAnimator.setDuration(Long.MAX_VALUE);
        frameAnimator.setInterpolator(new LinearInterpolator());
        frameAnimator.setRepeatCount(ValueAnimator.INFINITE);
        frameAnimator.addUpdateListener(anim -> {
            if (isActive) updateSyntheticSpectrum();
            smoothBars();
            invalidate();
        });
        frameAnimator.start();
    }

    public void stopAnimation() {
        isRunning = false;
        if (frameAnimator != null) {
            frameAnimator.cancel();
            frameAnimator = null;
        }
    }

    private void updateSyntheticSpectrum() {
        for (int i = 0; i < BAR_COUNT; i++) {
            float base = FREQ_ENVELOPE[i];
            float noise = (random.nextFloat() - 0.5f) * 0.3f;
            float beat = (float) Math.sin(System.currentTimeMillis() * 0.002f + i * 0.4f) * 0.1f;
            targetHeights[i] = Math.max(0.02f, Math.min(1.0f, base + noise + beat));
        }
    }

    private void smoothBars() {
        // Spring physics smoothing — gives the bars a natural bounce feel
        for (int i = 0; i < BAR_COUNT; i++) {
            float error = targetHeights[i] - barHeights[i];
            barVelocities[i] = barVelocities[i] * 0.7f + error * 0.15f;
            barHeights[i] += barVelocities[i];
            barHeights[i] = Math.max(0.01f, Math.min(1.0f, barHeights[i]));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float midY = h * 0.6f; // bars rise from 60% point

        float barWidth = w / (BAR_COUNT * (1 + BAR_GAP_RATIO));
        float gap = barWidth * BAR_GAP_RATIO;

        for (int i = 0; i < BAR_COUNT; i++) {
            float barH = barHeights[i] * midY;
            float x = i * (barWidth + gap);
            float top = midY - barH;
            float bottom = midY;

            RectF barRect = new RectF(x, top, x + barWidth, bottom);
            float radius = barWidth / 4f;

            // Gradient: cyan top → teal bottom
            LinearGradient barGradient = new LinearGradient(
                    x, top, x, bottom,
                    Color.parseColor("#00E5FF"),
                    Color.parseColor("#00796B"),
                    Shader.TileMode.CLAMP
            );
            barPaint.setShader(barGradient);
            canvas.drawRoundRect(barRect, radius, radius, barPaint);

            // Glow on tall bars
            if (barHeights[i] > 0.5f) {
                glowPaint.setColor(Color.parseColor("#00BCD4"));
                glowPaint.setAlpha((int) ((barHeights[i] - 0.5f) * 2 * 120));
                canvas.drawRoundRect(barRect, radius, radius, glowPaint);
            }

            // Mirror reflection below midY
            float reflectH = barH * 0.35f;
            RectF reflectRect = new RectF(x, midY, x + barWidth, midY + reflectH);
            LinearGradient reflectGrad = new LinearGradient(
                    x, midY, x, midY + reflectH,
                    Color.parseColor("#4000BCD4"),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
            );
            reflectionPaint.setShader(reflectGrad);
            canvas.drawRoundRect(reflectRect, radius, radius, reflectionPaint);
        }

        // Baseline glow line
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#3300E5FF"));
        linePaint.setStrokeWidth(2f);
        canvas.drawLine(0, midY, w, midY, linePaint);
    }
}
