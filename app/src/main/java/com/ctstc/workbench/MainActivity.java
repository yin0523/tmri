package com.ctstc.workbench;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    /** ▼▼ 你的工作台地址 ▼▼ */
    private static final String START_URL = "https://workbuddy.link/p/sAgnY7f18u334jFjIDPnib";

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingPermissionRequest;

    private ActivityResultLauncher<Intent> filePicker;
    private ActivityResultLauncher<String> cameraPermission;
    private ActivityResultLauncher<String> storagePermission;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView.setWebContentsDebuggingEnabled(false);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setSupportZoom(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        CookieManager.getInstance().setAcceptCookie(true);

        filePicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (fileCallback == null) return;
            Uri[] uris = null;
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                if (data.getDataString() != null) {
                    uris = new Uri[]{Uri.parse(data.getDataString())};
                } else if (data.getClipData() != null) {
                    int n = data.getClipData().getItemCount();
                    uris = new Uri[n];
                    for (int i = 0; i < n; i++) uris[i] = data.getClipData().getItemAt(i).getUri();
                }
            }
            fileCallback.onReceiveValue(uris);
            fileCallback = null;
        });

        cameraPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (pendingPermissionRequest != null) {
                pendingPermissionRequest.grant(pendingPermissionRequest.getResources());
                pendingPermissionRequest = null;
            }
            if (!granted) Toast.makeText(this, "没有相机权限，拍照会失败", Toast.LENGTH_SHORT).show();
        });

        storagePermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String host = u.getHost() == null ? "" : u.getHost();
                /* v1.1 修复白屏：短链 302 到 www.workbuddy.link，改为后缀匹配 */
                boolean inApp = host.endsWith("workbuddy.link")
                        || host.endsWith("workbuddy.cn")
                        || host.endsWith("codebuddy.work")
                        || host.endsWith("codebuddy.cn");
                if (inApp) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { }
                return true;
            }

            @Override
            public void onReceivedSslError(WebView v, android.webkit.SslErrorHandler h, android.net.http.SslError e) {
                h.proceed();
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                try {
                    filePicker.launch(params.createIntent());
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "打不开文件选择器", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                boolean needCam = false;
                for (String r : request.getResources()) {
                    if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) needCam = true;
                }
                if (needCam && ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    pendingPermissionRequest = request;
                    cameraPermission.launch(android.Manifest.permission.CAMERA);
                } else {
                    request.grant(request.getResources());
                }
            }
        });

        web.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                try {
                    DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                    req.setMimeType(mimeType);
                    req.allowScanningByMediaScanner();
                    req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    String name = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType);
                    req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) dm.enqueue(req);
                    Toast.makeText(MainActivity.this, "开始下载：" + name, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (web.canGoBack()) web.goBack();
                else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            storagePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (savedInstanceState == null) web.loadUrl(START_URL);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onRestoreInstanceState(Bundle in) {
        super.onRestoreInstanceState(in);
        web.restoreState(in);
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
