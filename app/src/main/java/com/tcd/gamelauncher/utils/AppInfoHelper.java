package com.tcd.gamelauncher.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.format.Formatter;

import java.io.File;

public class AppInfoHelper {

    private final Context context;

    public AppInfoHelper(Context context) {
        this.context = context;
    }

    public Drawable getAppIcon(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public String getAppVersion(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "N/A";
        }
    }

    public String getAppSize(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);

            long apkSize = new File(appInfo.sourceDir).length();
            long dataSize = calculateDirectorySize(new File(appInfo.dataDir));
            long cacheSize = calculateDirectorySize(new File(appInfo.dataDir, "cache"));
            long obbSize = calculateObbSize(packageName);

            long totalSize = apkSize + dataSize + cacheSize + obbSize;
            return formatSize(totalSize);

        } catch (Exception e) {
            return "Size unavailable";
        }
    }

    private long calculateDirectorySize(File directory) {
        if (directory == null || !directory.exists()) {
            return 0L;
        }

        long length = 0L;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                length += file.isDirectory() ? calculateDirectorySize(file) : file.length();
            }
        }
        return length;
    }

    private long calculateObbSize(String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File obbDir = context.getExternalFilesDir("Android/obb/" + packageName);
            return obbDir != null ? calculateDirectorySize(obbDir) : 0L;
        }
        return 0L;
    }

    public String formatSize(long bytes) {
        return Formatter.formatFileSize(context, bytes);
    }

    @SuppressLint("DefaultLocale")
    public String formatSize(long bytes, int unit) {
        final long KB = 1024L, MB = KB * 1024, GB = MB * 1024, TB = GB * 1024;

        return switch (unit) {
            case 1 -> String.format("%d B", bytes);
            case 2 -> String.format("%.2f KB", bytes / (double) KB);
            case 3 -> String.format("%.2f MB", bytes / (double) MB);
            case 4 -> String.format("%.2f GB", bytes / (double) GB);
            case 5 -> String.format("%.2f TB", bytes / (double) TB);
            default -> formatAutomatic(bytes);
        };
    }

    public String formatAutomatic(long bytes) {
        final long KB = 1024L, MB = KB * 1024, GB = MB * 1024, TB = GB * 1024;

        if (bytes < KB) return formatSize(bytes, 1);
        else if (bytes < MB) return formatSize(bytes, 2);
        else if (bytes < GB) return formatSize(bytes, 3);
        else if (bytes < TB) return formatSize(bytes, 4);
        else return formatSize(bytes, 5);
    }

}
