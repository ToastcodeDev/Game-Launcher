package com.tcd.gamelauncher;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.materialswitch.MaterialSwitch;

import android.content.SharedPreferences;

public class SettingsFragment extends Fragment {
    private MaterialSwitch hideTimeSwitch;
    private ImageView closeButton;

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings, container, false);

        hideTimeSwitch = view.findViewById(R.id.hide_time_switch);
        closeButton = view.findViewById(R.id.Close_Btn);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean isHideTime = preferences.getBoolean("hide_time", false);
        hideTimeSwitch.setChecked(isHideTime);

        closeButton.setOnClickListener(v -> closeSettings());

        hideTimeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("hide_time", isChecked);
            editor.apply();
        });

        return view;
    }

    private void closeSettings() {
        requireActivity()
            .getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .remove(this)
            .commit();
    }
    
    @Override
public void onResume() {
    super.onResume();
    ((MainActivity) requireActivity()).setAllowPopupMenu(false);
}

@Override
public void onPause() {
    super.onPause();
    ((MainActivity) requireActivity()).setAllowPopupMenu(true);
}
}