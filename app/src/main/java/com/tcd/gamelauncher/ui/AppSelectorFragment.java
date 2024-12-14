package com.tcd.gamelauncher;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppSelectorFragment extends Fragment {

  private List<ApplicationInfo> filteredApps;
  private PackageManager pm;

  public AppSelectorFragment(List<ApplicationInfo> filteredApps, PackageManager pm) {
    this.filteredApps = filteredApps;
    this.pm = pm;
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.game_browser, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    RecyclerView appListView = view.findViewById(R.id.app_selector_recycler_view);
    appListView.setLayoutManager(new LinearLayoutManager(requireContext()));

    appListView.setAdapter(
        new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
          @NonNull
          @Override
          public RecyclerView.ViewHolder onCreateViewHolder(
              @NonNull ViewGroup parent, int viewType) {
            View itemView =
                LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.app_selector_item, parent, false);
            return new RecyclerView.ViewHolder(itemView) {};
          }

          @Override
          public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ApplicationInfo appInfo = filteredApps.get(position);
            LinearLayout layout = (LinearLayout) holder.itemView;
            ImageView icon = layout.findViewById(R.id.app_icon);
            TextView name = layout.findViewById(R.id.app_name);

            icon.setImageDrawable(appInfo.loadIcon(pm));
            name.setText(appInfo.loadLabel(pm));

            layout.setOnClickListener(
                v -> {
                  if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity())
                        .addGame(
                            appInfo.packageName,
                            appInfo.loadLabel(pm).toString(),
                            appInfo.loadIcon(pm));
                  }
                  requireActivity().getSupportFragmentManager().popBackStack();
                });
          }

          @Override
          public int getItemCount() {
            return filteredApps.size();
          }
        });

    TextView cancelButton = view.findViewById(R.id.Cancel_Btn);
    cancelButton.setOnClickListener(
        v -> {
          requireActivity()
              .getSupportFragmentManager()
              .beginTransaction()
              .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
              .remove(this)
              .commit();
        });
  }

  @Override
  public void onResume() {
    super.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
  }
}

