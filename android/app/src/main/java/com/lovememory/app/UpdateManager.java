package com.lovememory.app;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UpdateManager {

    private static final String TAG = "UpdateManager";
    private static final String PREF_NAME = "lovememory_update";
    private static final String KEY_WGT_VERSION = "wgt_version";
    private static final String UPDATE_CONFIG_URL =
            "https://raw.githubusercontent.com/susje86777-prog/love-memory/main/update.json";
    private static final String HOT_UPDATE_DIR = "hot-update";

    private final Context context;
    private final Handler mainHandler;
    private final SharedPreferences prefs;
    private final String currentVersion;

    public interface UpdateCallback {
        default void onNoUpdate() {}
        default void onWgtUpdateAvailable(String version, String url, String changelog) {}
        default void onApkUpdateAvailable(String version, String url, String changelog) {}
        default void onWgtDownloaded() {}
        default void onWgtError(String error) {}
        default void onApkDownloaded(File apkFile) {}
        default void onError(String error) {}
    }

    public UpdateManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.currentVersion = getAppVersion();
    }

    /**
     * Get current APK version name
     */
    private String getAppVersion() {
        try {
            PackageInfo pi = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    /**
     * Get current APK version code
     */
    private int getAppVersionCode() {
        try {
            PackageInfo pi = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return pi.versionCode;
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * Get hot-updated WGT version (if any)
     */
    private String getWgtVersion() {
        return prefs.getString(KEY_WGT_VERSION, currentVersion);
    }

    /**
     * Check for updates — runs on background thread, callbacks on main thread
     */
    public void checkForUpdates(UpdateCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(UPDATE_CONFIG_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "LoveMemory-App/" + currentVersion);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                conn.disconnect();

                JSONObject config = new JSONObject(sb.toString());
                JSONObject apk = config.optJSONObject("apk");
                JSONObject wgt = config.optJSONObject("wgt");
                String changelog = config.optString("changelog", "");

                // Check WGT update first (hot update, no reinstall)
                if (wgt != null) {
                    String wgtVer = wgt.optString("version");
                    if (compareVersion(wgtVer, getWgtVersion()) > 0) {
                        mainHandler.post(() -> callback.onWgtUpdateAvailable(
                                wgtVer, wgt.optString("url"), changelog));
                        return;
                    }
                }

                // Check APK update (full package)
                if (apk != null) {
                    int apkVerCode = apk.optInt("versionCode", 1);
                    if (apkVerCode > getAppVersionCode()) {
                        mainHandler.post(() -> callback.onApkUpdateAvailable(
                                apk.optString("version"), apk.optString("url"), changelog));
                        return;
                    }
                }

                mainHandler.post(callback::onNoUpdate);
            } catch (Exception e) {
                Log.e(TAG, "Update check failed", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    /**
     * Download WGT hot-update package and extract it
     */
    public void downloadWgt(String downloadUrl, String newVersion, UpdateCallback callback) {
        new Thread(() -> {
            try {
                File zipFile = new File(context.getFilesDir(), "update.wgt");
                downloadFile(downloadUrl, zipFile);

                // Extract WGT to hot-update directory
                File hotUpdateDir = new File(context.getFilesDir(), HOT_UPDATE_DIR);
                if (hotUpdateDir.exists()) {
                    deleteRecursive(hotUpdateDir);
                }
                hotUpdateDir.mkdirs();

                extractZip(zipFile, hotUpdateDir);
                zipFile.delete(); // clean up zip

                // Save WGT version (use the actual remote version, not a made-up string)
                prefs.edit().putString(KEY_WGT_VERSION, newVersion).apply();

                mainHandler.post(callback::onWgtDownloaded);
            } catch (Exception e) {
                Log.e(TAG, "WGT download failed", e);
                mainHandler.post(() -> callback.onWgtError(e.getMessage()));
            }
        }).start();
    }

    /**
     * Download APK and trigger system install
     */
    public void downloadAndInstallApk(String downloadUrl, UpdateCallback callback) {
        new Thread(() -> {
            try {
                File apkDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (apkDir == null) {
                    apkDir = context.getFilesDir();
                }
                File apkFile = new File(apkDir, "love-memory-update.apk");
                if (apkFile.exists()) apkFile.delete();

                downloadFile(downloadUrl, apkFile);

                mainHandler.post(() -> callback.onApkDownloaded(apkFile));
            } catch (Exception e) {
                Log.e(TAG, "APK download failed", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    /**
     * Install APK using system installer (FileProvider)
     */
    public void installApk(File apkFile) {
        try {
            Uri apkUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    apkFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "APK install failed", e);
            mainHandler.post(() ->
                    Toast.makeText(context, "安装失败，请手动安装: " + apkFile.getPath(),
                            Toast.LENGTH_LONG).show());
        }
    }

    /**
     * Get hot-update index.html path (for custom loading)
     */
    public static File getHotUpdateDir(Context context) {
        File dir = new File(context.getFilesDir(), HOT_UPDATE_DIR);
        if (dir.exists() && new File(dir, "index.html").exists()) {
            return dir;
        }
        return null;
    }

    // ──────── Utility methods ────────

    private void downloadFile(String urlStr, File dest) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);

        try (InputStream is = new BufferedInputStream(conn.getInputStream());
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
        }
        conn.disconnect();
    }

    private void extractZip(File zipFile, File destDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new java.io.FileInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                // Skip __MACOSX and hidden files
                String name = entry.getName();
                if (name.contains("__MACOSX") || name.startsWith(".")) continue;
                // Strip top-level directory if present
                if (name.contains("/")) {
                    name = name.substring(name.indexOf('/') + 1);
                }
                if (name.isEmpty()) continue;

                File outFile = new File(destDir, name);
                outFile.getParentFile().mkdirs();

                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = zis.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    /**
     * Compare version strings (e.g. "1.0.0" vs "1.0.1")
     * Returns >0 if v1 > v2, <0 if v1 < v2, 0 if equal
     */
    private int compareVersion(String v1, String v2) {
        try {
            String[] p1 = v1.split("\\.");
            String[] p2 = v2.split("\\.");
            int len = Math.max(p1.length, p2.length);
            for (int i = 0; i < len; i++) {
                int n1 = i < p1.length ? Integer.parseInt(p1[i]) : 0;
                int n2 = i < p2.length ? Integer.parseInt(p2[i]) : 0;
                if (n1 != n2) return n1 - n2;
            }
            return 0;
        } catch (Exception e) {
            return v1.compareTo(v2);
        }
    }
}
