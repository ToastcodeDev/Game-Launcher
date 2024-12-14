package com.tcd.gamelauncher;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {
  private SharedPreferences preferences;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.settings, container, false);

    preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

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
              if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();

                mainActivity.resetGameTime();
                mainActivity.showToast(getString(R.string.operation_success));
                    closeSettings();
              }
            })
        .setNegativeButton(getString(R.string.dialog_negative), null)
        .show();
}

  private void closeSettings() {
    requireActivity()
        .getSupportFragmentManager()
        .beginTransaction()
        .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
        .remove(this)
        .commit();
  }
}

