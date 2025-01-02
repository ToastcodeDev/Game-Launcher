package com.tcd.gamelauncher.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.tcd.gamelauncher.R;
import com.tcd.gamelauncher.utils.AppInfoHelper;

public class GameFragment extends Fragment {
    private String packageName;
    private String gameTitle;
    private long totalTimePlayed;
    private String gameVersion = "1.0";
    private String gameSize = "0 MB";
    private OnGameActionListener gameActionListener;
    private AppInfoHelper appInfoHelper;

    private void openAppSettings(String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.game_activity, container, false);
        appInfoHelper = new AppInfoHelper(requireContext());

        if (getArguments() != null) {
            packageName = getArguments().getString("PACKAGE_NAME");
            gameTitle = getArguments().getString("GAME_TITLE");
            totalTimePlayed = getArguments().getLong("TOTAL_TIME_PLAYED", 0);
        }

        Drawable gameIcon = appInfoHelper.getAppIcon(packageName);
        fetchGameInfo();
        updateUI(view, gameIcon);
        setupBtnListeners(view);

        View gameTimeAdapter = view.findViewById(R.id.Game_Time_Adapter);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean isHideTime = preferences.getBoolean("hide_time", false);
        gameTimeAdapter.setVisibility(isHideTime ? View.GONE : View.VISIBLE);

        return view;
    }

    private void fetchGameInfo() {
        gameVersion = appInfoHelper.getAppVersion(packageName);
        gameSize = appInfoHelper.getAppSize(packageName);
    }

    private void updateUI(View view, Drawable gameIcon) {
        ImageView iconImageView = view.findViewById(R.id.Game_Icon);
        TextView titleTextView = view.findViewById(R.id.Game_Title);
        TextView packageTextView = view.findViewById(R.id.Game_Package);
        TextView totalTimeTextView = view.findViewById(R.id.total_time);
        TextView versionTextView = view.findViewById(R.id.Game_version);
        TextView sizeTextView = view.findViewById(R.id.Game_size);

        titleTextView.setText(gameTitle);
        packageTextView.setText(packageName);
        iconImageView.setImageDrawable(gameIcon);
        totalTimeTextView.setText(formatTime(totalTimePlayed));
        versionTextView.setText(gameVersion);
        sizeTextView.setText(gameSize);

        packageTextView.setOnLongClickListener(v -> {
            Context context = getContext();
            if (context != null) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Package Name", packageName);
                    clipboard.setPrimaryClip(clip);
                }
            } else {
                Log.e("GameFragment", "Null context on onLongClickListener");
            }
            return true;
        });
    }


    private String formatTime(long totalTime) {
        if (totalTime <= 0) {
            return getString(R.string.no_time_measured);
        }

        long seconds = (totalTime / 1000) % 60;
        long minutes = (totalTime / (1000 * 60)) % 60;
        long hours = (totalTime / (1000 * 60 * 60)) % 24;
        long days = totalTime / (1000 * 60 * 60 * 24);

        StringBuilder timeBuilder = new StringBuilder();
        if (days > 0) timeBuilder.append(days).append("d ");
        if (hours > 0 || days > 0) timeBuilder.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) timeBuilder.append(minutes).append("m ");
        if (seconds > 0 || timeBuilder.length() == 0) timeBuilder.append(seconds).append("s");

        return timeBuilder.toString().trim();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnGameActionListener) {
            gameActionListener = (OnGameActionListener) context;
        } else {
            throw new ClassCastException(context + " must implement OnGameActionListener");
        }
    }

    private void setupBtnListeners(View view) {
        ImageView closeMenuButton = view.findViewById(R.id.Close_Menu);
        closeMenuButton.setOnClickListener(v -> requireActivityMethod());

        ImageView removeGameButton = view.findViewById(R.id.Remove_Game);
        removeGameButton.setOnClickListener(v -> {
            if (gameActionListener != null) {
                gameActionListener.onGameRemove(packageName);
            }
            requireActivityMethod();
        });

        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab_show_in_settings);
        fab.setOnClickListener(v -> openAppSettings(packageName));
    }

    private void requireActivityMethod() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                .remove(this)
                .commit();
    }

    private boolean isAppInstalled(String packageName) {
        PackageManager pm = requireActivity().getPackageManager();
        try {
            pm.getApplicationInfo(packageName, 0);
            return true; // Game Installed
        } catch (PackageManager.NameNotFoundException e) {
            return false; // Game not installed
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isAppInstalled(packageName)) {
            requireActivityMethod();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public interface OnGameActionListener {
        void onGameRemove(String packageName);
    }
}