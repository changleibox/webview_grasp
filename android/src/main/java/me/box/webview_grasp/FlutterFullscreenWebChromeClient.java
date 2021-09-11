package me.box.webview_grasp;

import android.content.pm.ActivityInfo;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

/**
 * Verifies that a url opened by `Window.open` has a secure url.
 */
public class FlutterFullscreenWebChromeClient extends FlutterWebChromeClient implements View.OnKeyListener {
    private View fullscreenView;
    private CustomViewCallback customViewCallback;
    private int requestedOrientation;
    private int systemUiVisibility;

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        super.onShowCustomView(view, callback);
        if (fullscreenView != null || activity == null) {
            callback.onCustomViewHidden();
            return;
        }
        fullscreenView = view;
        fullscreenView.setOnKeyListener(this);
        customViewCallback = callback;
        requestedOrientation = activity.getRequestedOrientation();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        final Window window = activity.getWindow();
        final ViewGroup decorView = (ViewGroup) window.getDecorView();
        systemUiVisibility = decorView.getWindowSystemUiVisibility();
        decorView.addView(view);
        webView.setVisibility(View.GONE);
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onHideCustomView() {
        super.onHideCustomView();
        if (fullscreenView == null || activity == null) {
            return;
        }
        final Window window = activity.getWindow();
        final ViewGroup decorView = (ViewGroup) window.getDecorView();
        decorView.removeView(fullscreenView);
        webView.setVisibility(View.VISIBLE);
        activity.setRequestedOrientation(requestedOrientation);
        decorView.setSystemUiVisibility(systemUiVisibility);
        fullscreenView = null;
        customViewCallback = null;
    }

    /**
     * 判断是否可以退出全屏
     *
     * @return true – 可以退出，false – 不可以退出
     */
    public boolean canGoBack() {
        return customViewCallback != null;
    }

    /**
     * 退出全屏
     */
    public void goBack() {
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && canGoBack()) {
            goBack();
            return true;
        }
        return false;
    }
}