package com.tcd.gamelauncher.fragment;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.tcd.gamelauncher.MainActivity;
import com.tcd.gamelauncher.R;

public class SettingsFragment extends Fragment {
    private SharedPreferences preferences;
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        View view = inflater.inflate(R.layout.settings, container, false);
        setupHideTimeSwitch(view);
        setupClearTimeButton(view);
        setupCloseButton(view);
        return view;
    }

    private void setupHideTimeSwitch(View view) {
        MaterialSwitch hideTimeSwitch = view.findViewById(R.id.Htime_switch);
        hideTimeSwitch.setChecked(preferences.getBoolean("hide_time", false));
        hideTimeSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> preferences.edit().putBoolean("hide_time", isChecked).apply());
    }

    private void setupClearTimeButton(View view) {
        MaterialButton clearTimeBtn = view.findViewById(R.id.clear_btn);
        clearTimeBtn.setOnClickListener(v -> resetAllGameTimes());
        adjustButtonTextColor(clearTimeBtn);
    }

    private void adjustButtonTextColor(MaterialButton button) {
        int[] attrs = new int[]{android.R.attr.colorBackground};
        TypedArray ta = requireContext().obtainStyledAttributes(attrs);
        int backgroundColor = ta.getColor(0, Color.TRANSPARENT);
        ta.recycle();

        if (isColorBright(backgroundColor)) {
            button.setTextColor(Color.BLACK);
        } else {
            button.setTextColor(Color.WHITE);
        }
    }

    private boolean isColorBright(int color) {
        double brightness = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color));
        return brightness > 186;
    }

    private void setupCloseButton(View view) {
        ImageView closeButton = view.findViewById(R.id.Close_Btn);
        closeButton.setOnClickListener(v -> closeSettings());
    }

    private void resetAllGameTimes() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.time_dialog_title))
                .setMessage(getString(R.string.time_dialog_message))
                .setPositiveButton(
                        getString(R.string.dialog_positive),
                        (dialog, which) -> {
                            if (getActivity() instanceof MainActivity mainActivity) {
                                mainActivity.resetGameTime();
                                mainActivity.showToast(getString(R.string.operation_success));
                                closeSettings();
                            }
                        })
                .setNegativeButton(getString(R.string.dialog_negative), null)
                .show();
    }

    private void closeSettings() {
        requireActivityMethod();
    }

    private void requireActivityMethod() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
                .remove(this)
                .commit();
    }
}