package com.tcd.gamelauncher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements GameFragment.OnGameActionListener {

  private static final String PREFS_NAME = "GameLauncherPrefs";
  private static final String GAMES_KEY = "games";
  private static final String BLACKLIST_KEY = "blacklist";
  public static final int MAX_GAMES = 100;
  private static final String TAG = "GameLauncher";
  private static final String LAST_RESET_TIME_KEY = "lastResetTime";

  private RecyclerView gameListRecyclerView;
  private GameAdapter gameAdapter;
  private List<Game> gameList = Collections.synchronizedList(new ArrayList<>());
  private Set<String> blacklist = Collections.synchronizedSet(new HashSet<>());
  private SharedPreferences sharedPreferences;
  private Gson gson;
  private PackageManager packageManager;
  private ExecutorService executorService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    executorService =
        new ThreadPoolExecutor(
            3, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    packageManager = getPackageManager();
    gson = new Gson();
    sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
    initViews();
    loadBlacklist();
    loadGamesAsync();
    setStatusBarColor();
    setupBackPressHandler();
  }

  private void initViews() {
    gameListRecyclerView = findViewById(R.id.Game_list);
    View toolbarMenu = findViewById(R.id.Toolbar_menu);

    gameListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    gameAdapter = new GameAdapter(gameList, this, this::onGameLaunch, this::removeGame);
    gameListRecyclerView.setAdapter(gameAdapter);

    toolbarMenu.setOnClickListener(this::showToolbarMenu);
  }

  private void loadBlacklist() {
    executorService.execute(
        () -> {
          String jsonBlacklist = sharedPreferences.getString(BLACKLIST_KEY, null);
          if (jsonBlacklist != null) {
            Type listType = new TypeToken<Set<String>>() {}.getType();
            blacklist = gson.fromJson(jsonBlacklist, listType);
          }
        });
  }

  private void loadGamesAsync() {
    executorService.execute(
        () -> {
            String jsonGames = sharedPreferences.getString(GAMES_KEY, null);
            if (jsonGames != null) {
                Type listType = new TypeToken<List<Game>>() {}.getType();
                List<Game> savedGames = gson.fromJson(jsonGames, listType);

                if (savedGames != null) {
                    synchronized (gameList) {
                        gameList.clear();
                        for (Game game : savedGames) {
                            try {
                                Drawable icon = packageManager.getApplicationIcon(game.packageName);
                                game.icon = icon;
                                gameList.add(game);
                            } catch (PackageManager.NameNotFoundException e) {
                                // App icon not found
                            }
                        }
                    }
                }
            }
            loadBlacklist(); 
            executorService.execute(() -> {
                autoDetectGames();
                runOnUiThread(() -> gameAdapter.notifyDataSetChanged());
            });
        });
}

  private void autoDetectGames() {
    if (gameList.size() >= MAX_GAMES) return;

    List<ApplicationInfo> apps =
        packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
    List<ApplicationInfo> gameApps =
        apps.stream()
            .filter(
                app ->
                    (app.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                        && isGameApp(app)
                        && !blacklist.contains(app.packageName.trim().toLowerCase())
                        && gameList.stream().noneMatch(g -> g.packageName.equals(app.packageName)))
            .sorted(
                (app1, app2) ->
                    app1.loadLabel(packageManager)
                        .toString()
                        .compareToIgnoreCase(app2.loadLabel(packageManager).toString()))
            .collect(Collectors.toList());

    synchronized (gameList) {
      for (ApplicationInfo app : gameApps) {
        if (gameList.size() >= MAX_GAMES) break;

        Game newGame =
            new Game(
                app.packageName,
                app.loadLabel(packageManager).toString(),
                app.loadIcon(packageManager));
        gameList.add(newGame);
      }
    }
  }

  private boolean isGameApp(ApplicationInfo app) {
    Intent launchIntent = packageManager.getLaunchIntentForPackage(app.packageName);
    return launchIntent != null
        && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
            || app.category == ApplicationInfo.CATEGORY_GAME);
  }

  private void saveBlacklist() {
    executorService.execute(
        () -> sharedPreferences.edit().putString(BLACKLIST_KEY, gson.toJson(blacklist)).apply());
}

  private void saveGame() {
    executorService.execute(
        () -> sharedPreferences.edit().putString(GAMES_KEY, gson.toJson(gameList)).apply());
  }

  private void showToolbarMenu(View view) {

    Context wrapper = new ContextThemeWrapper(this, R.style.PopupMenuStyle);
    PopupMenu popupMenu = new PopupMenu(wrapper, view);
    Menu menu = popupMenu.getMenu();

    menu.add(getString(R.string.add_games));
    menu.add(getString(R.string.detect_automatically));
    menu.add(getString(R.string.settings));
    menu.add(getString(R.string.about));

    popupMenu.setOnMenuItemClickListener(
        item -> {
          String title = item.getTitle().toString();

          if (title.equals(getString(R.string.add_games))) {
            openAppSelector();
            return true;
          } else if (title.equals(getString(R.string.detect_automatically))) {
            autoDetectErase();
            return true;
          } else if (title.equals(getString(R.string.settings))) {
            openSettings();
            return true;
          } else if (title.equals(getString(R.string.about))) {
            showAboutDialog();
            return true;
          } else {
            return false;
          }
        });

    popupMenu.show();
  }

  private void openSettings() {
    SettingsFragment settingsFragment = new SettingsFragment();
    getSupportFragmentManager()
        .beginTransaction()
        .setCustomAnimations(
            R.anim.enter_from_right,
            R.anim.exit_to_left,
            R.anim.enter_from_left,
            R.anim.exit_to_right)
        .replace(R.id.Framelay, settingsFragment)
        .addToBackStack(null)
        .commit();
  }

  private void autoDetectErase() {
    executorService.execute(
        () -> {
            synchronized (gameList) {
                gameList.clear();
            }
            autoDetectGames();
            saveGame();

            runOnUiThread(() -> {
                gameAdapter.notifyDataSetChanged();
                updateBasedList();
            });
        });
}

  private void setStatusBarColor() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Window window = getWindow();
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.setStatusBarColor(getResources().getColor(R.color.statusBarColor));
    }
  }

  private void openAppSelector() {
    PackageManager pm = getPackageManager();
    List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

    List<ApplicationInfo> filteredApps =
        apps.stream()
            .filter(app -> (app.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
            .sorted(
                (app1, app2) ->
                    app1.loadLabel(pm)
                        .toString()
                        .compareToIgnoreCase(app2.loadLabel(pm).toString()))
            .collect(Collectors.toList());

    AppSelectorFragment fragment = new AppSelectorFragment(filteredApps, pm);
    getSupportFragmentManager()
        .beginTransaction()
        .setCustomAnimations(
            R.anim.enter_from_right,
            R.anim.exit_to_left,
            R.anim.enter_from_left,
            R.anim.exit_to_right)
        .replace(R.id.Framelay, fragment)
        .addToBackStack(null)
        .commit();
  }

  public void addGame(String packageName, String title, Drawable icon) {
    Set<String> blacklist = lockAddOfAssets();

    if (blacklist.contains(packageName)) {
      showToast(getString(R.string.invalid_game_package));
      return;
    }

    if (gameList.stream().anyMatch(g -> g.packageName.equals(packageName))) {
      showToast(getString(R.string.game_list_exist));
      return;
    }

    Game newGame = new Game(packageName, title, icon);
    gameList.add(newGame);
    gameAdapter.notifyItemInserted(gameList.size() - 1);
    saveGame();
    updateBasedList();
  }

  private synchronized void removeGame(int position) {
    if (position >= 0 && position < gameList.size()) {
      Game removedGame = gameList.get(position);
      gameList.remove(position);
      gameAdapter.notifyItemRemoved(position);
      gameAdapter.notifyItemRangeChanged(position, gameList.size());
      saveGame();

      if (!blacklist.contains(removedGame.packageName)) {
        blacklist.add(removedGame.packageName);
        saveBlacklist();
      }

      runOnUiThread(
          () -> {
            showToast(getString(R.string.game_deleted_success));
            updateBasedList();
          });
    }
  }

  private Set<String> lockAddOfAssets() {
    Set<String> blacklist = new HashSet<>();
    try (InputStream inputStream = getAssets().open("blacklist.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = reader.readLine()) != null) {
        blacklist.add(line.trim());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return blacklist;
  }

  private void updateBasedList() {
    TextView blankStateTextView = findViewById(R.id.Blank_state);
    RecyclerView recyclerView = findViewById(R.id.Game_list);

    if (gameList.isEmpty()) {
      blankStateTextView.setVisibility(View.VISIBLE);
      recyclerView.setVisibility(View.GONE);
    } else {
      blankStateTextView.setVisibility(View.GONE);
      recyclerView.setVisibility(View.VISIBLE);
    }
  }

  private void onGameLaunch(String packageName) {
    PackageManager packageManager = getPackageManager();
    Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);

    if (launchIntent != null) {
      Game game =
          gameList.stream().filter(g -> g.packageName.equals(packageName)).findFirst().orElse(null);

      if (game != null) {
        game.lastStartTime = System.currentTimeMillis();
        startActivity(launchIntent);
      }

      saveGame();
    } else {
      showToast(getString(R.string.select_game_error));
    }
  }

  @Override
  public void onGameRemove(String packageName) {
    synchronized (gameList) {
      for (int i = 0; i < gameList.size(); i++) {
        if (gameList.get(i).packageName.equals(packageName)) {
          gameList.remove(i);
          gameAdapter.notifyItemRemoved(i);
          gameAdapter.notifyItemRangeChanged(i, gameList.size());
          saveGame();
          showToast(getString(R.string.game_deleted_success));
          break;
        }
      }
    }
  }

  private void showAboutDialog() {
    new MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.app_name))
        .setMessage(getString(R.string.developer) + ".\n\n© 2024 ToastcodeDev")
        .setPositiveButton("OK", null)
        .setCancelable(false)
        .show();
  }

  public void showToast(String message) {
    Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    boolean dataChanged = false;

    synchronized (gameList) {
      for (Game game : gameList) {
        if (game.lastStartTime > 0) {
          long elapsedTime = System.currentTimeMillis() - game.lastStartTime;
          game.totalTimePlayed += elapsedTime;
          game.lastStartTime = 0;
          dataChanged = true;
        }
      }
    }

    if (dataChanged) {
      saveGame();
      runOnUiThread(() -> gameAdapter.notifyDataSetChanged());
    }
  }

  private void setupBackPressHandler() {
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                  getSupportFragmentManager().popBackStack();
                } else {
                  finish();
                }
              }
            });
  }

  public void resetGameTime() {
    executorService.execute(
        () -> {
          synchronized (gameList) {
            for (Game game : gameList) {
              game.totalTimePlayed = 0;
              game.lastStartTime = 0;
            }
            saveGame();
          }
          runOnUiThread(
              () -> {
                gameAdapter.notifyDataSetChanged();
              });
        });
  }
}

