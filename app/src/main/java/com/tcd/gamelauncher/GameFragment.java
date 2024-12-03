package com.tcd.gamelauncher;

import android.preference.PreferenceManager;
import com.tcd.gamelauncher.R;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.SharedPreferences;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import java.io.File;

public class GameFragment extends Fragment {
  private String packageName;
  private String gameTitle;
  private long totalTimePlayed;
  private String gameVersion = "Unknown";
  private String gameSize = "Unknown";
  private View gameTimeAdapter;

  public interface OnGameActionListener {
    void onGameRemove(String packageName);
  }

  private OnGameActionListener gameActionListener;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.game_activity, container, false);

    if (getArguments() != null) {
      packageName = getArguments().getString("PACKAGE_NAME");
      gameTitle = getArguments().getString("GAME_TITLE");
      totalTimePlayed = getArguments().getLong("TOTAL_TIME_PLAYED", 0);
    }

    Drawable gameIcon = getAppIcon(packageName);
    fetchGameInfo();
    updateUI(view, gameIcon);
    setupButtonListeners(view);

    gameTimeAdapter = view.findViewById(R.id.Game_Time_Adapter);

    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
    boolean isHideTime = preferences.getBoolean("hide_time", false);

    // Ocultar/mostrar adaptador según el estado
    gameTimeAdapter.setVisibility(isHideTime ? View.GONE : View.VISIBLE);

    return view;
  }

  private void fetchGameInfo() {
    try {
      gameVersion = getAppVersion(packageName);
      gameSize = getAppSize(packageName);
    } catch (PackageManager.NameNotFoundException e) {
      showToast(getString(R.string.fragment_info_error));
    }
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
  }

  private Drawable getAppIcon(String packageName) {
    try {
      PackageManager pm = requireActivity().getPackageManager();
      return pm.getApplicationIcon(packageName);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
      showToast(getString(R.string.fragment_icon_error));
      return null;
    }
  }

  private String getAppVersion(String packageName) throws PackageManager.NameNotFoundException {
    PackageManager pm = requireActivity().getPackageManager();
    return pm.getPackageInfo(packageName, 0).versionName;
  }

  private String getAppSize(String packageName) throws PackageManager.NameNotFoundException {
    PackageManager pm = requireActivity().getPackageManager();
    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
    long appSizeBytes = new File(appInfo.sourceDir).length();

    return formatFileSize(appSizeBytes);
  }

  private String formatFileSize(long sizeBytes) {
    double sizeMB = sizeBytes / (1024.0 * 1024.0);
    if (sizeMB < 1024) {
      return String.format("%.2f MB", sizeMB);
    }
    double sizeGB = sizeMB / 1024.0;
    if (sizeGB < 1024) {
      return String.format("%.2f GB", sizeGB);
    }
    return String.format("%.2f TB", sizeGB / 1024.0);
  }

  private String formatTime(long totalTime) {
    if (totalTime == 0) {
      return getString(R.string.no_time_measured);
    }

    long seconds = (totalTime / 1000) % 60;
    long minutes = (totalTime / (1000 * 60)) % 60;
    long hours = (totalTime / (1000 * 60 * 60)) % 24;
    long days = totalTime / (1000 * 60 * 60 * 24);

    StringBuilder formattedTime = new StringBuilder();

    if (days > 0) {
      formattedTime.append(days).append("d ");
    }
    if (hours > 0 || days > 0) {
      formattedTime.append(hours).append("h ");
    }
    if (minutes > 0 || hours > 0 || days > 0) {
      formattedTime.append(minutes).append("m ");
    }
    if (seconds > 0 || formattedTime.length() == 0) {
      formattedTime.append(seconds).append("s");
    }

    return formattedTime.toString().trim();
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    if (context instanceof OnGameActionListener) {
      gameActionListener = (OnGameActionListener) context;
    } else {
      throw new ClassCastException(context.toString() + " must implement OnGameActionListener");
    }
  }

  private void setupButtonListeners(View view) {
    ImageView closeMenuButton = view.findViewById(R.id.Close_Menu);
    closeMenuButton.setOnClickListener(
        v ->
            requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                .remove(this)
                .commit());

    ImageView removeGameButton = view.findViewById(R.id.Remove_Game);
    removeGameButton.setOnClickListener(
        v -> {
          if (gameActionListener != null) {
            gameActionListener.onGameRemove(packageName);
          }
          requireActivity()
              .getSupportFragmentManager()
              .beginTransaction()
              .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
              .remove(this)
              .commit();
        });
  }

  private void showToast(String message) {
    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
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
