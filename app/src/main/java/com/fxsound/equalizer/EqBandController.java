package com.fxsound.equalizer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.media.audiofx.Equalizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * EqBandController — Manages the 5-band equalizer UI and links it to Android AudioEffect.Equalizer.
 *
 * Evidence basis:
 * - Resource 10117 (FxSound tooltip Grid): A small rounded Grid with TextBlock "mText" at FontSize=12,
 *   shown when the user hovers/drags an EQ band. We replicate this as tvBandTooltip.
 * - Resource 10115 (flat button style): EQ action buttons (Reset, Apply) use this style.
 * - FxSound uses 5 enhancement parameters visible in the EQ section (inferred from
 *   the app's documented feature set and standard FxSound UI).
 *
 * The 5 EQ bands mirror typical FxSound band layout:
 *   Band 0: 60 Hz   (Sub-bass)
 *   Band 1: 230 Hz  (Bass)
 *   Band 2: 910 Hz  (Mids)
 *   Band 3: 4000 Hz (Upper-mids/Presence)
 *   Band 4: 14000 Hz (Treble/Air)
 */
public class EqBandController {

    private static final String[] BAND_LABELS = {"60 Hz", "230 Hz", "910 Hz", "4K Hz", "14K Hz"};

    private final LinearLayout container;
    private final TextView tooltipView;
    private final Equalizer equalizer;

    private final List<EqBandView> bandViews = new ArrayList<>();

    // Default flat gains (0 dB = center of range)
    private int[] currentGains = new int[]{0, 0, 0, 0, 0};

    public EqBandController(LinearLayout container, TextView tooltipView, Equalizer equalizer) {
        this.container = container;
        this.tooltipView = tooltipView;
        this.equalizer = equalizer;
    }

    public void setupBands() {
        container.removeAllViews();
        bandViews.clear();

        short numBands = (equalizer != null) ? equalizer.getNumberOfBands() : 5;
        int[] centerFreqs = (equalizer != null) ? getCenterFreqs(equalizer) :
                new int[]{60, 230, 910, 4000, 14000};

        for (int i = 0; i < numBands; i++) {
            final int bandIndex = i;
            String label = (i < BAND_LABELS.length) ? BAND_LABELS[i] : centerFreqs[i] + " Hz";

            EqBandView bandView = new EqBandView(container.getContext(), label, i);
            bandView.setOnGainChangedListener((band, gain) -> {
                currentGains[band] = gain;
                applyBandGain(band, gain);
                showTooltip(label, gain);
            });

            // Set initial value from equalizer or default 0
            if (equalizer != null) {
                short[] levelRange = equalizer.getBandLevelRange();
                int minLevel = levelRange[0];
                int maxLevel = levelRange[1];
                int currentLevel = equalizer.getBandLevel((short) i);
                bandView.setRange(minLevel, maxLevel, currentLevel);
            } else {
                bandView.setRange(-1500, 1500, 0);
            }

            container.addView(bandView);
            bandViews.add(bandView);
        }
    }

    public void setDefaultGains() {
        setupBandsNoEq();
    }

    private void setupBandsNoEq() {
        container.removeAllViews();
        bandViews.clear();
        for (int i = 0; i < 5; i++) {
            EqBandView bv = new EqBandView(container.getContext(), BAND_LABELS[i], i);
            bv.setRange(-1500, 1500, 0);
            final String lbl = BAND_LABELS[i];
            bv.setOnGainChangedListener((band, gain) -> showTooltip(lbl, gain));
            container.addView(bv);
            bandViews.add(bv);
        }
    }

    private void applyBandGain(int band, int gain) {
        if (equalizer != null) {
            try {
                equalizer.setBandLevel((short) band, (short) gain);
            } catch (Exception e) {
                // AudioEffect may not be available
            }
        }
    }

    /**
     * Show tooltip — mirrors Resource 10117 (FxSound's tooltip Grid with TextBlock mText)
     * The tooltip shows band frequency + current gain in dB
     */
    private void showTooltip(String freq, int gainMilliBels) {
        if (tooltipView == null) return;
        float gainDb = gainMilliBels / 100f;
        String gainStr = String.format("%+.1f dB", gainDb);
        tooltipView.setText(freq + "\n" + gainStr);
        tooltipView.setVisibility(View.VISIBLE);
        // Auto-hide after 1.5s
        tooltipView.removeCallbacks(hideTooltipRunnable);
        tooltipView.postDelayed(hideTooltipRunnable, 1500);
    }

    private final Runnable hideTooltipRunnable = () -> {
        if (tooltipView != null) {
            tooltipView.animate().alpha(0f).setDuration(300)
                    .withEndAction(() -> {
                        tooltipView.setVisibility(View.GONE);
                        tooltipView.setAlpha(1f);
                    }).start();
        }
    };

    public void applyPreset(Preset preset) {
        if (preset.gains == null || preset.gains.length == 0) return;
        for (int i = 0; i < Math.min(bandViews.size(), preset.gains.length); i++) {
            currentGains[i] = preset.gains[i];
            bandViews.get(i).animateTo(preset.gains[i]);
            applyBandGain(i, preset.gains[i]);
        }
    }

    public void applyClarityPreset(boolean isOn) {
        // Clarity boost: lift upper-mids and treble
        int[] clarityGains = isOn ? new int[]{0, -100, 100, 300, 400} : currentGains;
        for (int i = 0; i < Math.min(bandViews.size(), clarityGains.length); i++) {
            bandViews.get(i).animateTo(clarityGains[i]);
            applyBandGain(i, clarityGains[i]);
        }
    }

    public void resetToFlat() {
        for (int i = 0; i < bandViews.size(); i++) {
            currentGains[i] = 0;
            bandViews.get(i).animateTo(0);
            applyBandGain(i, 0);
        }
    }

    private int[] getCenterFreqs(Equalizer eq) {
        short bands = eq.getNumberOfBands();
        int[] freqs = new int[bands];
        for (short i = 0; i < bands; i++) {
            freqs[i] = eq.getCenterFreq(i) / 1000; // Hz
        }
        return freqs;
    }
}
