package com.fxsound.equalizer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fxsound.equalizer.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity — FxSound-inspired Audio Equalizer & Preset Manager
 *
 * Features mirrored from FxSound (evidence from fxsound_setup.exe resources):
 * 1. Sprite-sheet-animated toggle buttons (Resource 10109 pattern → SpriteSheetButton custom view)
 * 2. Audio preset selector (Resource 10111 ComboBox → PresetAdapter RecyclerView)
 * 3. Preset category browser (Resource 10110 TreeView → expandable RecyclerView)
 * 4. EQ band sliders with tooltip panel (Resource 10117 tooltip Grid)
 * 5. Bass Boost & Virtualizer toggles (Resource 10108 CheckBox pattern)
 * 6. Real-time spectrum visualizer (extends FxSound's core audio pipeline concept)
 */
public class MainActivity extends AppCompatActivity
        implements PresetAdapter.OnPresetClickListener, EqualizerManager.OnEqReadyListener {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private ActivityMainBinding binding;
    private EqualizerManager equalizerManager;
    private PresetAdapter presetAdapter;
    private EqBandController eqBandController;
    private SpectrumVisualizerView spectrumView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestPermissions();
        initUI();
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.MODIFY_AUDIO_SETTINGS},
                    PERMISSION_REQUEST_CODE);
        } else {
            initAudioEngine();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initAudioEngine();
            } else {
                // Run without audio effects — UI still functional
                initAudioEngineNoPermission();
            }
        }
    }

    private void initUI() {
        // --- Power toggle (sprite-sheet animated button pattern from Resource 10109) ---
        binding.btnPower.setOnStateChangedListener(isOn -> {
            if (equalizerManager != null) {
                equalizerManager.setEnabled(isOn);
            }
            updatePowerState(isOn);
        });

        // --- Preset RecyclerView (mirrors Resource 10111 ComboBox + Resource 10110 TreeView) ---
        presetAdapter = new PresetAdapter(buildPresets(), this);
        binding.rvPresets.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvPresets.setAdapter(presetAdapter);

        // --- EQ band sliders ---
        eqBandController = new EqBandController(
                binding.eqLayout,
                binding.tvBandTooltip,
                null
        );

        // --- Enhancement toggles (CheckBox pattern from Resource 10108) ---
        binding.btnBassBoost.setOnStateChangedListener(isOn -> {
            if (equalizerManager != null) equalizerManager.setBassBoost(isOn);
        });
        binding.btnSurround.setOnStateChangedListener(isOn -> {
            if (equalizerManager != null) equalizerManager.setVirtualizer(isOn);
        });
        binding.btnClarity.setOnStateChangedListener(isOn -> {
            eqBandController.applyClarityPreset(isOn);
        });

        // --- Spectrum Visualizer ---
        spectrumView = binding.spectrumView;
        spectrumView.startAnimation();

        // --- Reset / Save buttons (Resource 10115 button style) ---
        binding.btnReset.setOnClickListener(v -> resetToDefault());
        binding.btnSave.setOnClickListener(v -> saveCustomPreset());
    }

    private void initAudioEngine() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        equalizerManager = new EqualizerManager(this);
        equalizerManager.init();
        equalizerManager.setOnEqReadyListener(this);
    }

    private void initAudioEngineNoPermission() {
        equalizerManager = null;
        eqBandController = new EqBandController(
                binding.eqLayout,
                binding.tvBandTooltip,
                null
        );
        // Simulate default values
        eqBandController.setDefaultGains();
    }

    @Override
    public void onEqReady(Equalizer eq, BassBoost bassBoost, Virtualizer virtualizer) {
        runOnUiThread(() -> {
            eqBandController = new EqBandController(
                    binding.eqLayout,
                    binding.tvBandTooltip,
                    eq
            );
            eqBandController.setupBands();
            spectrumView.attachEqualizer(eq);
        });
    }

    @Override
    public void onPresetSelected(Preset preset) {
        if (eqBandController != null) {
            eqBandController.applyPreset(preset);
        }
        // Animate selection feedback
        binding.tvCurrentPreset.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    binding.tvCurrentPreset.setText(preset.name);
                    binding.tvCurrentPreset.animate().alpha(1f).setDuration(300).start();
                }).start();
    }

    private List<Preset> buildPresets() {
        List<Preset> presets = new ArrayList<>();
        // Category: Music
        presets.add(new Preset("Flat", "Music", new int[]{0, 0, 0, 0, 0}));
        presets.add(new Preset("Rock", "Music", new int[]{400, 200, -100, 200, 400}));
        presets.add(new Preset("Pop", "Music", new int[]{-100, 200, 400, 200, -100}));
        presets.add(new Preset("Jazz", "Music", new int[]{200, 100, 0, 200, 300}));
        presets.add(new Preset("Classical", "Music", new int[]{500, 300, -100, 200, 400}));
        presets.add(new Preset("Hip-Hop", "Music", new int[]{500, 400, 0, -100, 200}));
        // Category: Gaming
        presets.add(new Preset("FPS", "Gaming", new int[]{300, 100, -100, 200, 400}));
        presets.add(new Preset("RPG", "Gaming", new int[]{200, 300, 100, 100, 200}));
        presets.add(new Preset("Racing", "Gaming", new int[]{400, 200, 0, 100, 300}));
        // Category: Movie
        presets.add(new Preset("Cinema", "Movie", new int[]{300, 200, -100, 200, 400}));
        presets.add(new Preset("Action", "Movie", new int[]{500, 300, 0, 200, 500}));
        // Category: Voice
        presets.add(new Preset("Podcast", "Voice", new int[]{-200, 100, 400, 300, 100}));
        presets.add(new Preset("Speech", "Voice", new int[]{-300, 0, 500, 300, 100}));

        return presets;
    }

    private void updatePowerState(boolean isOn) {
        float targetAlpha = isOn ? 1.0f : 0.4f;
        binding.eqLayout.animate().alpha(targetAlpha).setDuration(400).start();
        binding.spectrumView.setActive(isOn);
    }

    private void resetToDefault() {
        if (eqBandController != null) {
            eqBandController.resetToFlat();
        }
        presetAdapter.clearSelection();
        binding.tvCurrentPreset.setText("Flat");
    }

    private void saveCustomPreset() {
        // TODO: Show dialog to name and save custom preset
        Toast.makeText(this, "Custom preset saved!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (equalizerManager != null) {
            equalizerManager.release();
        }
        if (spectrumView != null) {
            spectrumView.stopAnimation();
        }
    }
}
