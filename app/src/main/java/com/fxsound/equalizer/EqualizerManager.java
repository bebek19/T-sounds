package com.fxsound.equalizer;

import android.content.Context;
import android.media.AudioManager;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;

/**
 * EqualizerManager — Wraps Android AudioEffect APIs for the FxSound feature set.
 *
 * FxSound enhancement pipeline (inferred from WinUI resources and feature documentation):
 * 1. Equalizer (5-band parametric EQ)
 * 2. Bass Boost (sub-bass enhancement)
 * 3. Surround / Virtualizer (3D spatial audio)
 *
 * On Android, these map to:
 * - android.media.audiofx.Equalizer
 * - android.media.audiofx.BassBoost
 * - android.media.audiofx.Virtualizer
 *
 * All effects attach to the global audio session (0) so they affect system-wide audio,
 * which is the closest equivalent to FxSound's kernel-level audio driver interception.
 */
public class EqualizerManager {

    public interface OnEqReadyListener {
        void onEqReady(Equalizer eq, BassBoost bassBoost, Virtualizer virtualizer);
    }

    private final Context context;
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;
    private OnEqReadyListener listener;
    private boolean isEnabled = true;

    public EqualizerManager(Context context) {
        this.context = context;
    }

    public void init() {
        try {
            // Attach to global audio session (session 0 = applies to all audio)
            equalizer = new Equalizer(0, 0);
            equalizer.setEnabled(true);

            bassBoost = new BassBoost(0, 0);
            bassBoost.setEnabled(false);

            virtualizer = new Virtualizer(0, 0);
            virtualizer.setEnabled(false);

            if (listener != null) {
                listener.onEqReady(equalizer, bassBoost, virtualizer);
            }
        } catch (Exception e) {
            // AudioEffect may not be available on all devices/emulators
        }
    }

    public void setOnEqReadyListener(OnEqReadyListener l) {
        this.listener = l;
        // If already initialized, call immediately
        if (equalizer != null && l != null) {
            l.onEqReady(equalizer, bassBoost, virtualizer);
        }
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (equalizer != null) equalizer.setEnabled(enabled);
        if (bassBoost != null && bassBoost.getEnabled()) bassBoost.setEnabled(enabled);
        if (virtualizer != null && virtualizer.getEnabled()) virtualizer.setEnabled(enabled);
    }

    public void setBassBoost(boolean enabled) {
        if (bassBoost != null) {
            bassBoost.setEnabled(enabled && isEnabled);
            if (enabled) {
                bassBoost.setStrength((short) 800); // 80% strength
            }
        }
    }

    public void setVirtualizer(boolean enabled) {
        if (virtualizer != null) {
            virtualizer.setEnabled(enabled && isEnabled);
            if (enabled) {
                virtualizer.setStrength((short) 700); // 70% strength
            }
        }
    }

    public void release() {
        if (equalizer != null) { equalizer.release(); equalizer = null; }
        if (bassBoost != null) { bassBoost.release(); bassBoost = null; }
        if (virtualizer != null) { virtualizer.release(); virtualizer = null; }
    }
}
