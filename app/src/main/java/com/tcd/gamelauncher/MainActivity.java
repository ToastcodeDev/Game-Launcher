package com.tcd.gamelauncher;

import static java.util.stream.Collectors.toList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tcd.gamelauncher.fragment.AppSelectorFragment;
import com.tcd.gamelauncher.fragment.GameFragment;
import com.tcd.gamelauncher.fragment.SettingsFragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements GameFragment.OnGameActionListener {

    private static final String PREFS_NAME = "GameLauncherPrefs";
    private static final String GAMES_KEY = "games";
    private static final String BLACKLIST_KEY = "blacklist";
    private final List<Game> gameList = Collections.synchronizedList(new ArrayList<>());
    private final Set<String> blacklist = Collections.synchronizedSet(new HashSet<>());
    private GameAdapter gameAdapter;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private PackageManager packageManager;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executorService = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

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
        RecyclerView gameListRecyclerView = findViewById(R.id.Game_list);
        View toolbarMenu = findViewById(R.id.Toolbar_menu);
        gameListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        gameAdapter = new GameAdapter(gameList, this, this::onGameLaunch, this::removeGame);
        gameListRecyclerView.setAdapter(gameAdapter);
        toolbarMenu.setOnClickListener(this::showToolbarMenu);
        gameListRecyclerView.setHasFixedSize(true);
    }

    private void showToolbarMenu(View view) {
        Context wrapper = new ContextThemeWrapper(this, R.style.PopupMenuStyle);
        PopupMenu popupMenu = new PopupMenu(wrapper, view);
        popupMenu.getMenuInflater().inflate(R.menu.toolbar_menu, popupMenu.getMenu());

        try {
            @SuppressLint("DiscouragedPrivateApi") Field field = popupMenu.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuHelper = field.get(popupMenu);
            assert menuHelper != null;
            Class<?> classPopupHelper = Class.forName(menuHelper.getClass().getName());
            Method setForceIcons = classPopupHelper.getDeclaredMethod("setForceShowIcon", boolean.class);
            setForceIcons.setAccessible(true);
            setForceIcons.invoke(menuHelper, true);
        } catch (Exception e) {
            //Catch Exception
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.action_add_games) {
                openAppSelector();
                return true;
            } else if (id == R.id.action_detect_automatically) {
                autoDetectErase();
                return true;
            } else if (id == R.id.action_settings) {
                openSettings();
                return true;
            } else if (id == R.id.action_about) {
                showAboutDialog();
                return true;
            } else {
                return false;
            }
        });


        popupMenu.show();
    }


    @SuppressLint("NotifyDataSetChanged")
    private void checkInstalledApps() {
        List<Game> toRemove = new ArrayList<>();
        for (Game game : gameList) {
            try {
                packageManager.getApplicationInfo(game.packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                toRemove.add(game);
            }
        }

        for (Game game : toRemove) {
            gameList.remove(game);
        }
        saveGame();
        runOnUiThread(() -> gameAdapter.notifyDataSetChanged());
    }

    private void loadBlacklist() {
        executorService.execute(() -> {
            String jsonBlacklist = sharedPreferences.getString(BLACKLIST_KEY, null);
            if (jsonBlacklist != null) {
                Type setType = new TypeToken<Set<String>>() {
                }.getType();
                synchronized (blacklist) {
                    blacklist.clear();
                    blacklist.addAll(gson.fromJson(jsonBlacklist, setType));
                }
            }
        });
    }


    @SuppressLint("NotifyDataSetChanged")
    private void loadGamesAsync() {
        executorService.execute(() -> {
            String jsonGames = sharedPreferences.getString(GAMES_KEY, null);
            List<Game> savedGames = new ArrayList<>();

            if (jsonGames != null) {
                Type listType = new TypeToken<List<Game>>() {}.getType();
                savedGames = gson.fromJson(jsonGames, listType);
            }

            synchronized (gameList) {
                gameList.clear();
                for (Game game : savedGames) {
                    synchronized (blacklist) {
                        if (game.packageName != null && !blacklist.contains(game.packageName.trim().toLowerCase())) {
                            try {
                                game.icon = packageManager.getApplicationIcon(game.packageName);
                                gameList.add(game);
                            } catch (PackageManager.NameNotFoundException ignored) {
                            }
                        }
                    }
                }
            }

            runOnUiThread(() -> {
                gameAdapter.notifyDataSetChanged();
                autoDetectGames();
            });
        });
    }


    private void autoDetectGames() {
        List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        Set<String> blacklistFromAssets = lockAddOfAssets();
        synchronized (blacklist) {
            blacklist.addAll(blacklistFromAssets);
        }

        List<Game> detectedGames = new ArrayList<>();
        Set<String> currentGamePackages = new HashSet<>();
        synchronized (gameList) {
            for (Game game : gameList) {
                currentGamePackages.add(game.packageName);
            }
        }

        for (ApplicationInfo app : installedApps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                    && isGameApp(app)
                    && !blacklist.contains(app.packageName.trim().toLowerCase())
                    && !currentGamePackages.contains(app.packageName)) {
                detectedGames.add(new Game(
                        app.packageName,
                        app.loadLabel(packageManager).toString(),
                        app.loadIcon(packageManager)
                ));
            }
        }

        synchronized (gameList) {
            gameList.addAll(detectedGames);
        }

        saveGame();
    }


    private boolean isGameApp(ApplicationInfo app) {
        Intent launchIntent = packageManager.getLaunchIntentForPackage(app.packageName);
        return launchIntent != null && app.category == ApplicationInfo.CATEGORY_GAME;
    }

    private void saveBlacklist() {
        executorService.execute(
                () -> sharedPreferences.edit().putString(BLACKLIST_KEY, gson.toJson(blacklist)).apply());
    }

    private void saveGame() {
        executorService.execute(
                () -> sharedPreferences.edit().putString(GAMES_KEY, gson.toJson(gameList)).apply());
    }


    private void openSettings() {
        SettingsFragment settingsFragment = SettingsFragment.newInstance();
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

    @SuppressLint("NotifyDataSetChanged")
    private void autoDetectErase() {
        executorService.execute(() -> {
            autoDetectGames();
            saveGame();
            runOnUiThread(() -> {
                gameAdapter.notifyDataSetChanged();
                updateBasedList();
            });
        });
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        int color = ContextCompat.getColor(this, R.color.statusBarColor);
        window.setStatusBarColor(color);
    }

    private void openAppSelector() {
        PackageManager pm = getPackageManager();
        @SuppressLint("QueryPermissionsNeeded") List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        List<ApplicationInfo> filteredApps =
                apps.stream()
                        .filter(app -> (app.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
                        .sorted(
                                (app1, app2) ->
                                        app1.loadLabel(pm)
                                                .toString()
                                                .compareToIgnoreCase(app2.loadLabel(pm).toString()))
                        .collect(toList());

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
        synchronized (blacklist) {
            if (blacklist.contains(packageName.trim().toLowerCase())) {
                showToast(getString(R.string.invalid_game_package));
                return;
            }
        }

        synchronized (gameList) {
            if (gameList.stream().anyMatch(g -> g.packageName.equals(packageName))) {
                showToast(getString(R.string.game_list_exist));
                return;
            }
            Game newGame = new Game(packageName, title, icon);
            gameList.add(newGame);
        }

        runOnUiThread(() -> {
            gameAdapter.notifyItemInserted(gameList.size() - 1);
            saveGame();
            updateBasedList();
        });
    }


    private synchronized void removeGame(int position) {
        if (position >= 0 && position < gameList.size()) {
            Game removedGame = gameList.remove(position);
            gameAdapter.notifyItemRemoved(position);
            gameAdapter.notifyItemRangeChanged(position, gameList.size());
            saveGame();

            synchronized (blacklist) {
                blacklist.add(removedGame.packageName);
                saveBlacklist();
            }

            runOnUiThread(() -> {
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
                String trimmedLine = line.trim().toLowerCase();
                if (!trimmedLine.isEmpty()) {
                    blacklist.add(trimmedLine);
                }
            }
        } catch (IOException e) {
            Log.e("GameLauncher", "Error reading blacklist.txt: " + e.getMessage(), e);
        }

        if (blacklist.isEmpty()) {
            Log.w("GameLauncher", "Blacklist is empty or could not be loaded.");
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
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        @SuppressLint("InflateParams") View bottomSheetView = getLayoutInflater().inflate(R.layout.dialog_about, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        ImageView githubIcon = bottomSheetView.findViewById(R.id.github_icon);
        githubIcon.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ToastcodeDev/Game-Launcher"));
            startActivity(browserIntent);
        });

        bottomSheetDialog.show();
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

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        executorService.execute(() -> {
            checkInstalledApps();
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
        });
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

    @SuppressLint("NotifyDataSetChanged")
    public void resetGameTime() {
        executorService.execute(() -> {
            synchronized (gameList) {
                for (Game game : gameList) {
                    game.totalTimePlayed = 0;
                    game.lastStartTime = 0;
                }
                saveGame();
            }
            runOnUiThread(() -> gameAdapter.notifyDataSetChanged());
        });
    }
}