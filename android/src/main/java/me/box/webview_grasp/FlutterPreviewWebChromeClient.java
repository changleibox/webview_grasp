package me.box.webview_grasp;

import android.content.pm.ActivityInfo;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Verifies that a url opened by `Window.open` has a secure url.
 */
public class FlutterPreviewWebChromeClient extends FlutterWebChromeClient implements View.OnKeyListener {
    private ViewGroup fullscreenView;
    private CustomViewCallback customViewCallback;
    private int requestedOrientation;
    private int windowSystemUiVisibility;

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        super.onShowCustomView(view, callback);
        customViewCallback = callback;
        if (fullscreenView != null || activity == null) {
            callback.onCustomViewHidden();
            return;
        }
        fullscreenView = new FrameLayout(view.getContext());
        fullscreenView.setOnKeyListener(this);
        fullscreenView.addView(view);
        requestedOrientation = activity.getRequestedOrientation();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        final ViewGroup decorView = getDecorView();
        windowSystemUiVisibility = decorView.getSystemUiVisibility();
        decorView.setSystemUiVisibility(getFullscreenSystemUiVisibility());
        decorView.addView(fullscreenView);
    }

    @Override
    public void onHideCustomView() {
        super.onHideCustomView();
        customViewCallback = null;
        if (fullscreenView == null || activity == null) {
            return;
        }
        activity.setRequestedOrientation(requestedOrientation);
        final ViewGroup decorView = getDecorView();
        decorView.setSystemUiVisibility(windowSystemUiVisibility);
        decorView.removeView(fullscreenView);
        fullscreenView.setOnKeyListener(null);
        fullscreenView = null;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            return true;
        }
        return false;
    }

    private ViewGroup getDecorView() {
        return (ViewGroup) activity.getWindow().getDecorView();
    }

    private static int getFullscreenSystemUiVisibility() {
        return View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
    }
}