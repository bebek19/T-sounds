package com.fxsound.equalizer;

/**
 * Preset — Audio preset data model.
 *
 * Mirrors FxSound's preset system where each preset has:
 * - A name (bound to TreeView item "Content" property — Resource 10110)
 * - A category (bound to TreeView tooltip "ToolTip" property — Resource 10112)
 * - EQ band gains in millibels (the actual DSP values)
 */
public class Preset {
    public final String name;
    public final String category;
    public final int[] gains; // millibels per band (e.g., 300 = +3.0dB)

    public Preset(String name, String category, int[] gains) {
        this.name = name;
        this.category = category;
        this.gains = gains;
    }
}
