package com.lovememory.app;

import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.getcapacitor.BridgeActivity;

import java.io.File;
import java.io.FileInputStream;

public class MainActivity extends BridgeActivity {

    private File hotUpdateDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for hot-updated web assets
        hotUpdateDir = UpdateManager.getHotUpdateDir(this);

        if (hotUpdateDir != null) {
            // Stop any in-flight page load (it's loading from built-in assets)
            if (bridge != null && bridge.getWebView() != null) {
                bridge.getWebView().stopLoading();
            }
            // Register hot-update asset loader (chains existing Capacitor client)
            registerHotUpdateAssetLoader();
            // Reload so all requests go through our interceptor
            if (bridge != null && bridge.getWebView() != null) {
                bridge.getWebView().reload();
            }
        }
    }

    /**
     * Intercept webview requests to serve hot-updated files from local storage.
     * CRITICAL: Chains Capacitor's original WebViewClient so bridge stays intact.
     */
    private void registerHotUpdateAssetLoader() {
        if (bridge == null || bridge.getWebView() == null) return;

        WebView webView = bridge.getWebView();
        // Save Capacitor's own WebViewClient — must NOT lose it
        final WebViewClient capacitorClient = webView.getWebViewClient();
        final File hd = hotUpdateDir;

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                               WebResourceRequest request) {
                // 1) Try to serve from hot-update directory first
                String path = request.getUrl().getPath();
                if (path != null) {
                    if (path.startsWith("/")) path = path.substring(1);
                    if (path.isEmpty()) path = "index.html";

                    File file = new File(hd, path);
                    if (file.exists() && file.isFile()) {
                        try {
                            String mime = getMimeType(path);
                            return new WebResourceResponse(
                                    mime, "UTF-8",
                                    new FileInputStream(file));
                        } catch (Exception ignored) {}
                    }
                }

                // 2) Fall back to Capacitor's built-in handler (bridged JS, etc.)
                if (capacitorClient != null) {
                    return capacitorClient.shouldInterceptRequest(view, request);
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
    }

    private String getMimeType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".woff")) return "font/woff";
        if (path.endsWith(".woff2")) return "font/woff2";
        if (path.endsWith(".mp3")) return "audio/mpeg";
        if (path.endsWith(".mp4")) return "video/mp4";
        if (path.endsWith(".webm")) return "video/webm";
        return "application/octet-stream";
    }
}
