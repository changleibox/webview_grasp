package me.box.webview_grasp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Message;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

/**
 * Verifies that a url opened by `Window.open` has a secure url.
 */
public class FlutterWebChromeClient extends WebChromeClient {
    protected Activity activity;
    protected WebView webView;
    private FlutterWebViewClient flutterWebViewClient;

    @Override
    public boolean onCreateWindow(final WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        final WebViewClient webViewClient = new PrivateFlutterWebViewClient();

        final WebView newWebView = new WebView(view.getContext());
        newWebView.setWebViewClient(webViewClient);

        final WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(newWebView);
        resultMsg.sendToTarget();
        return true;
    }

    @Override
    public void onProgressChanged(WebView view, int progress) {
        flutterWebViewClient.onLoadingProgress(progress);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    public void setFlutterWebViewClient(FlutterWebViewClient flutterWebViewClient) {
        this.flutterWebViewClient = flutterWebViewClient;
    }

    private class PrivateFlutterWebViewClient extends WebViewClient {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull WebResourceRequest request) {
            final String url = request.getUrl().toString();
            if (!flutterWebViewClient.shouldOverrideUrlLoading(FlutterWebChromeClient.this.webView, request)) {
                webView.loadUrl(url);
            }
            return true;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (!flutterWebViewClient.shouldOverrideUrlLoading(FlutterWebChromeClient.this.webView, url)) {
                webView.loadUrl(url);
            }
            return true;
        }
    }
}