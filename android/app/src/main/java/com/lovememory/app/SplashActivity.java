package com.lovememory.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 1500;
    private static final float BLUR_START_RADIUS = 20f;

    private boolean updateCheckDone = false;
    private boolean splashDone = false;
    private boolean dialogShowing = false;

    private UpdateManager updateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        updateManager = new UpdateManager(this);

        TextView title = findViewById(R.id.splash_title);
        TextView subtitle = findViewById(R.id.splash_subtitle);

        // Start blur-to-clear + fade-in animation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            animateWithBlur(title, subtitle);
        } else {
            animateWithScale(title, subtitle);
        }

        // Start update check in parallel (doesn't block splash)
        checkForUpdate();

        // After splash duration, proceed (or wait if dialog is showing)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            splashDone = true;
            proceedIfReady();
        }, SPLASH_DURATION_MS);
    }

    private void checkForUpdate() {
        updateManager.checkForUpdates(new UpdateManager.UpdateCallback() {
            @Override
            public void onNoUpdate() {
                updateCheckDone = true;
                proceedIfReady();
            }

            @Override
            public void onWgtUpdateAvailable(String version, String url, String changelog) {
                // WGT hot-update: download in background, no dialog needed
                Toast.makeText(SplashActivity.this,
                        "正在下载热更新 " + version + "...", Toast.LENGTH_SHORT).show();

                updateManager.downloadWgt(url, version, new UpdateManager.UpdateCallback() {
                    @Override
                    public void onWgtDownloaded() {
                        Toast.makeText(SplashActivity.this,
                                "热更新完成，重启后生效", Toast.LENGTH_SHORT).show();
                        updateCheckDone = true;
                        proceedIfReady();
                    }

                    @Override
                    public void onWgtError(String error) {
                        Toast.makeText(SplashActivity.this,
                                "热更新失败: " + error, Toast.LENGTH_SHORT).show();
                        updateCheckDone = true;
                        proceedIfReady();
                    }

                    @Override
                    public void onError(String error) {
                        updateCheckDone = true;
                        proceedIfReady();
                    }

                    @Override public void onNoUpdate() {}
                    @Override public void onApkUpdateAvailable(String v, String u, String c) {}
                    @Override public void onApkDownloaded(File f) {}
                });
            }

            @Override
            public void onApkUpdateAvailable(String version, String url, String changelog) {
                // APK full update: show dialog
                showApkUpdateDialog(version, url, changelog);
            }

            @Override
            public void onError(String error) {
                updateCheckDone = true;
                proceedIfReady();
            }

            @Override
            public void onApkDownloaded(File apkFile) {
                updateCheckDone = true;
                dialogShowing = false;
                // Trigger system install
                updateManager.installApk(apkFile);
                proceedIfReady();
            }

            @Override public void onWgtDownloaded() {}
            @Override public void onWgtError(String err) {}
        });
    }

    private void showApkUpdateDialog(String version, String url, String changelog) {
        dialogShowing = true;

        String message = "发现新版本 " + version + "\n\n" + changelog;
        new AlertDialog.Builder(this)
                .setTitle("更新可用")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("立即更新", (dialog, which) -> {
                    Toast.makeText(SplashActivity.this,
                            "正在下载更新...", Toast.LENGTH_SHORT).show();
                    updateManager.downloadAndInstallApk(url,
                            new UpdateManager.UpdateCallback() {
                                @Override
                                public void onApkDownloaded(File apkFile) {
                                    updateCheckDone = true;
                                    dialogShowing = false;
                                    updateManager.installApk(apkFile);
                                    proceedIfReady();
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(SplashActivity.this,
                                            "下载失败: " + error, Toast.LENGTH_LONG).show();
                                    updateCheckDone = true;
                                    dialogShowing = false;
                                    proceedIfReady();
                                }

                                @Override public void onNoUpdate() {}
                                @Override public void onWgtUpdateAvailable(String v, String u, String c) {}
                                @Override public void onApkUpdateAvailable(String v, String u, String c) {}
                                @Override public void onWgtDownloaded() {}
                                @Override public void onWgtError(String e) {}
                            });
                })
                .setNegativeButton("稍后", (dialog, which) -> {
                    dialogShowing = false;
                    proceedIfReady();
                })
                .show();
    }

    /**
     * Only proceed to main when both splash animation AND update check are done
     * (and no dialog is showing)
     */
    private void proceedIfReady() {
        if (!splashDone) return;
        if (!updateCheckDone) return;
        if (dialogShowing) return;

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ═══════════════════════════════════════════
    //  Splash Animation (unchanged from original)
    // ═══════════════════════════════════════════

    private void animateWithBlur(TextView title, TextView subtitle) {
        ValueAnimator blurAnimator = ValueAnimator.ofFloat(BLUR_START_RADIUS, 0f);
        blurAnimator.setDuration(SPLASH_DURATION_MS);
        blurAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        blurAnimator.addUpdateListener(animation -> {
            float radius = (float) animation.getAnimatedValue();
            RenderEffect blur = RenderEffect.createBlurEffect(
                    radius, radius, Shader.TileMode.CLAMP);
            title.setRenderEffect(blur);
            if (radius < 3f) {
                subtitle.setRenderEffect(blur);
            }
        });

        title.setAlpha(0f);
        subtitle.setAlpha(0f);

        title.animate()
                .alpha(1f)
                .setDuration(SPLASH_DURATION_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        subtitle.animate()
                .alpha(1f)
                .setDuration(SPLASH_DURATION_MS)
                .setStartDelay(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        blurAnimator.start();
    }

    private void animateWithScale(TextView title, TextView subtitle) {
        title.setAlpha(0f);
        title.setScaleX(0.92f);
        title.setScaleY(0.92f);
        subtitle.setAlpha(0f);

        title.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(SPLASH_DURATION_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        subtitle.animate()
                .alpha(1f)
                .setDuration(SPLASH_DURATION_MS)
                .setStartDelay(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }
}
