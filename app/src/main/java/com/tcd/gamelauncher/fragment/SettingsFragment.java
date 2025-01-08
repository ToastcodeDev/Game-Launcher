package com.tcd.gamelauncher.fragment;

import android.content.SharedPreferences;
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
    private static final String PREF_HIDE_TIME = "hide_time";
    private SharedPreferences preferences;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        View view = inflater.inflate(R.layout.settings, container, false);
        setupHideTimeSwitch(view);
        setupClearTimeButton(view);
        setupCloseButton(view);
        return view;
    }

    private void setupHideTimeSwitch(View view) {
        MaterialSwitch hideTimeSwitch = view.findViewById(R.id.Htime_switch);
        hideTimeSwitch.setChecked(preferences.getBoolean(PREF_HIDE_TIME, false));
        hideTimeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferences.edit().putBoolean(PREF_HIDE_TIME, isChecked).apply());
    }

    private void setupClearTimeButton(View view) {
        MaterialButton clearTimeBtn = view.findViewById(R.id.clear_btn);
        clearTimeBtn.setOnClickListener(v -> resetAllGameTimes());
    }

    private void setupCloseButton(View view) {
        ImageView closeButton = view.findViewById(R.id.Close_Btn);
        closeButton.setOnClickListener(v -> closeSettings());
    }

    private void resetAllGameTimes() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.time_dialog_title))
                .setMessage(getString(R.string.time_dialog_message))
                .setPositiveButton(getString(R.string.dialog_positive), (dialog, which) -> {
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
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
                .remove(this)
                .commit();
    }
}