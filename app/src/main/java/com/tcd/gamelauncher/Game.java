package com.tcd.gamelauncher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tcd.gamelauncher.fragment.GameFragment;

import java.util.List;

public class Game {
    String packageName;
    String title;
    transient Drawable icon;
    long totalTimePlayed;
    long lastStartTime;

    Game(String packageName, String title, Drawable icon) {
        this.packageName = packageName;
        this.title = title;
        this.icon = icon;
        this.totalTimePlayed = 0;
        this.lastStartTime = 0;
    }
}

class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {
    private final List<Game> gameList;
    private final Context context;
    private final GameLaunchCallback launchCallback;
    private final GameRemoveCallback removeCallback;

    public GameAdapter(
            List<Game> gameList,
            Context context,
            GameLaunchCallback launchCallback,
            GameRemoveCallback removeCallback) {
        this.gameList = gameList;
        this.context = context;
        this.launchCallback = launchCallback;
        this.removeCallback = removeCallback;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.game_container, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = gameList.get(position);
        holder.gameTitle.setText(game.title);
        holder.gameIcon.setImageDrawable(game.icon);
        holder.openGame.setOnClickListener(v -> launchCallback.onLaunch(game.packageName));

        holder.itemView.setOnClickListener(
                v -> {
                    GameFragment gameFragment = new GameFragment();
                    Bundle args = new Bundle();
                    args.putString("PACKAGE_NAME", game.packageName);
                    args.putString("GAME_TITLE", game.title);
                    args.putLong("TOTAL_TIME_PLAYED", game.totalTimePlayed);
                    gameFragment.setArguments(args);

                    ((MainActivity) context)
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(
                                    R.anim.enter_from_left,
                                    R.anim.exit_to_right,
                                    R.anim.enter_from_right,
                                    R.anim.exit_to_left)
                            .replace(R.id.Framelay, gameFragment)
                            .addToBackStack(null)
                            .commit();
                });

        View removeButton = holder.itemView.findViewById(R.id.Remove_Game);
        if (removeButton != null) {
            removeButton.setOnClickListener(v -> {
                if (removeCallback != null) {
                    removeCallback.onRemove(position);
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return gameList.size();
    }

    interface GameLaunchCallback {
        void onLaunch(String packageName);
    }

    interface GameRemoveCallback {
        void onRemove(int position);
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        TextView gameTitle;
        ImageView gameIcon, openGame;

        GameViewHolder(@NonNull View itemView) {
            super(itemView);
            gameTitle = itemView.findViewById(R.id.Game_Title);
            gameIcon = itemView.findViewById(R.id.Game_Icon);
            openGame = itemView.findViewById(R.id.Open_Game);
        }
    }
}
