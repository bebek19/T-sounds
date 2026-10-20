package com.fxsound.equalizer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * PresetAdapter — Horizontal list of audio presets.
 *
 * Evidence source: Resource 10111 (FxSound's custom ComboBox with SplitOpenThemeAnimation)
 * and Resource 10110 (TreeView with multi-select and 25px item height).
 *
 * FxSound uses:
 *   - A ComboBox (dropdown) for selecting preset categories
 *   - A TreeView with items bound to "Content" + "ToolTip" properties
 *   - RevealBrush hover effects
 *   - Selection state changes background: TreeViewItemBackgroundSelected
 *
 * Android implementation:
 *   - Horizontal RecyclerView (replaces the TreeView/ComboBox combination)
 *   - Cards with animated selection state using ViewPropertyAnimator
 *   - Category chips above the preset list
 */
public class PresetAdapter extends RecyclerView.Adapter<PresetAdapter.PresetViewHolder> {

    public interface OnPresetClickListener {
        void onPresetSelected(Preset preset);
    }

    private final List<Preset> presets;
    private final OnPresetClickListener listener;
    private int selectedPosition = -1;

    // Colors matching FxSound dark theme
    private static final int COLOR_SELECTED_BG  = Color.parseColor("#00BCD4");
    private static final int COLOR_NORMAL_BG    = Color.parseColor("#1E1E3A");
    private static final int COLOR_SELECTED_TEXT = Color.WHITE;
    private static final int COLOR_NORMAL_TEXT   = Color.parseColor("#8888AA");
    private static final int COLOR_CATEGORY      = Color.parseColor("#00E5FF");

    public PresetAdapter(List<Preset> presets, OnPresetClickListener listener) {
        this.presets = presets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PresetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create card programmatically (mirrors FxSound TreeViewItem with 25px height)
        CardView card = new CardView(parent.getContext());
        card.setRadius(16f);
        card.setCardElevation(4f);
        card.setUseCompatPadding(true);

        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 8, 8);
        card.setLayoutParams(params);
        card.setPadding(32, 24, 32, 24);

        View innerView = new View(parent.getContext()) {
            // Placeholder — real content is in ViewHolder
        };

        return new PresetViewHolder(card);
    }

    @Override
    public void onBindViewHolder(@NonNull PresetViewHolder holder, int position) {
        Preset preset = presets.get(position);
        boolean isSelected = position == selectedPosition;

        // Card background (mirrors TreeViewItemBackgroundSelected)
        holder.card.setCardBackgroundColor(isSelected ? COLOR_SELECTED_BG : COLOR_NORMAL_BG);

        holder.tvName.setTextColor(isSelected ? COLOR_SELECTED_TEXT : COLOR_NORMAL_TEXT);
        holder.tvCategory.setTextColor(isSelected ? Color.parseColor("#CCFFFFFF") : COLOR_CATEGORY);
        holder.tvName.setText(preset.name);
        holder.tvCategory.setText(preset.category);

        // Scale animation on selection (mirrors WinUI Reveal hover/selection)
        float targetScale = isSelected ? 1.05f : 1.0f;
        holder.card.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(200)
                .start();

        holder.card.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (prev >= 0) notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            if (listener != null) listener.onPresetSelected(preset);
        });
    }

    @Override
    public int getItemCount() {
        return presets.size();
    }

    public void clearSelection() {
        int prev = selectedPosition;
        selectedPosition = -1;
        if (prev >= 0) notifyItemChanged(prev);
    }

    static class PresetViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvName;
        TextView tvCategory;

        PresetViewHolder(CardView card) {
            super(card);
            this.card = card;

            // Build inner layout
            android.widget.LinearLayout ll = new android.widget.LinearLayout(card.getContext());
            ll.setOrientation(android.widget.LinearLayout.VERTICAL);
            ll.setGravity(android.view.Gravity.CENTER);

            tvCategory = new TextView(card.getContext());
            tvCategory.setTextSize(9f);
            tvCategory.setGravity(android.view.Gravity.CENTER);
            tvCategory.setLetterSpacing(0.1f);

            tvName = new TextView(card.getContext());
            tvName.setTextSize(13f);
            tvName.setGravity(android.view.Gravity.CENTER);
            tvName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            ll.addView(tvCategory);
            ll.addView(tvName);
            card.addView(ll);
        }
    }
}
