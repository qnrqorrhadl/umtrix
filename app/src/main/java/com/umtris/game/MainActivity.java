package com.umtris.game;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 엄트리스 앱: 전체화면 웹뷰로 게임 주소를 열어 줌.
 * - 인터넷이 되면 항상 최신 버전을 불러옴 (게임을 고쳐도 앱을 다시 설치할 필요 없음)
 * - 인터넷이 없으면 저장해 둔 버전으로 실행하고, 그것도 없으면 앱에 내장된 버전으로 실행
 */
public class MainActivity extends Activity {
    // 게임이 올라가 있는 주소 (GitHub Pages)
    private static final String GAME_URL = "https://qnrqorrhadl.github.io/umtrix/";
    // 인터넷이 전혀 안 될 때 쓰는, 앱 안에 들어 있는 게임
    private static final String OFFLINE_URL = "file:///android_asset/offline/index.html";

    // ★ 앱을 새로 만들어 다시 설치해야 할 때 이 숫자를 올리세요 (게임 코드의 REQUIRED_APP 과 같은 숫자로)
    private static final int APP_VERSION = 2;

    private WebView web;
    private boolean showingOffline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // 게임 중 화면이 꺼지지 않게
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        web = new WebView(this);
        web.setBackgroundColor(0xFF14161C);
        setContentView(web);

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);   // 터치 없이도 배경음이 바로 나오게

        // 게임이 '앱 버전'을 물어보고, 다운로드 주소를 브라우저로 열 수 있게 해 주는 다리
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public int version() { return APP_VERSION; }
            @JavascriptInterface public void openUrl(String url) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) {}
            }
        }, "UmApp");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // 주소를 못 불러오면 앱에 내장된 게임으로 대신 실행
                if (request.isForMainFrame() && !showingOffline) {
                    showingOffline = true;
                    view.loadUrl(OFFLINE_URL);
                }
            }
        });

        loadGame();
    }

    private void loadGame() {
        WebSettings settings = web.getSettings();
        if (isOnline()) {
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            Map<String, String> headers = new HashMap<>();
            headers.put("Cache-Control", "no-cache");        // 항상 최신 버전인지 확인
            web.loadUrl(GAME_URL, headers);
        } else {
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);   // 저장해 둔 버전으로 실행
            web.loadUrl(GAME_URL);
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {   // 상단바와 하단 버튼을 숨기는 전체화면
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
