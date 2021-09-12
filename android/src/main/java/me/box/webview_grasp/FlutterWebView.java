// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package me.box.webview_grasp;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.platform.PlatformView;

public class FlutterWebView implements PlatformView, MethodCallHandler {
    private static final String JS_CHANNEL_NAMES_FIELD = "javascriptChannelNames";

    private final WebView webView;
    private final MethodChannel methodChannel;
    private final FlutterWebChromeClient flutterWebChromeClient;
    private final FlutterWebViewClient flutterWebViewClient;
    private final Handler platformThreadHandler;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressWarnings("unchecked")
    FlutterWebView(final Context context, MethodChannel methodChannel, Map<String, Object> params, View containerView) {
        DisplayListenerProxy displayListenerProxy = new DisplayListenerProxy();
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        displayListenerProxy.onPreWebViewInitialization(displayManager);

        final boolean supportFullscreen = Boolean.TRUE.equals(params.get("supportFullscreen"));
        if (supportFullscreen) {
            flutterWebChromeClient = new FlutterPreviewWebChromeClient();
        } else {
            flutterWebChromeClient = new FlutterWebChromeClient();
        }
        webView = createWebView(new WebViewBuilder(context, containerView), params, flutterWebChromeClient);

        displayListenerProxy.onPostWebViewInitialization(displayManager);

        platformThreadHandler = new Handler(context.getMainLooper());

        this.methodChannel = methodChannel;
        this.methodChannel.setMethodCallHandler(this);

        flutterWebViewClient = new FlutterWebViewClient(methodChannel);
        flutterWebChromeClient.setWebView(webView);
        flutterWebChromeClient.setFlutterWebViewClient(flutterWebViewClient);

        Map<String, Object> settings = (Map<String, Object>) params.get("settings");
        if (settings != null) {
            applySettings(settings);
        }

        if (params.containsKey(JS_CHANNEL_NAMES_FIELD)) {
            List<String> names = (List<String>) params.get(JS_CHANNEL_NAMES_FIELD);
            if (names != null) {
                registerJavaScriptChannelNames(names);
            }
        }

        Integer autoMediaPlaybackPolicy = (Integer) params.get("autoMediaPlaybackPolicy");
        if (autoMediaPlaybackPolicy != null) {
            updateAutoMediaPlaybackPolicy(autoMediaPlaybackPolicy);
        }
        if (params.containsKey("userAgent")) {
            String userAgent = (String) params.get("userAgent");
            updateUserAgent(userAgent);
        }
        if (params.containsKey("initialUrl")) {
            String url = (String) params.get("initialUrl");
            webView.loadUrl(url);
        }
    }

    /**
     * Creates a {@link android.webkit.WebView} and configures it according to the supplied
     * parameters.
     *
     * <p>The {@link WebView} is configured with the following predefined settings:
     *
     * <ul>
     *   <li>always enable the DOM storage API;
     *   <li>always allow JavaScript to automatically open windows;
     *   <li>always allow support for multiple windows;
     *   <li>always use the {@link FlutterWebChromeClient} as web Chrome client.
     * </ul>
     *
     * <p><strong>Important:</strong> This method is visible for testing purposes only and should
     * never be called from outside this class.
     *
     * @param webViewBuilder  a {@link WebViewBuilder} which is responsible for building the {@link
     *                        WebView}.
     * @param params          creation parameters received over the method channel.
     * @param webChromeClient an implementation of WebChromeClient This value may be null.
     * @return The new {@link android.webkit.WebView} object.
     */
    @VisibleForTesting
    static WebView createWebView(WebViewBuilder webViewBuilder, Map<String, Object> params, WebChromeClient webChromeClient) {
        boolean usesHybridComposition = Boolean.TRUE.equals(params.get("usesHybridComposition"));
        webViewBuilder
                .setUsesHybridComposition(usesHybridComposition)
                .setDomStorageEnabled(true) // Always enable DOM storage API.
                .setJavaScriptCanOpenWindowsAutomatically(true) // Always allow automatically opening of windows.
                .setSupportMultipleWindows(true) // Always support multiple windows.
                .setWebChromeClient(webChromeClient); // Always use {@link FlutterWebChromeClient} as web Chrome client.

        return webViewBuilder.build();
    }

    @Override
    public View getView() {
        return webView;
    }

    // @Override
    // This is overriding a method that hasn't rolled into stable Flutter yet. Including the
    // annotation would cause compile time failures in versions of Flutter too old to include the new
    // method. However leaving it raw like this means that the method will be ignored in old versions
    // of Flutter but used as an override anyway wherever it's actually defined.
    // TODO(mklim): Add the @Override annotation once flutter/engine#9727 rolls to stable.
    public void onInputConnectionUnlocked() {
        if (webView instanceof InputAwareWebView) {
            ((InputAwareWebView) webView).unlockInputConnection();
        }
    }

    // @Override
    // This is overriding a method that hasn't rolled into stable Flutter yet. Including the
    // annotation would cause compile time failures in versions of Flutter too old to include the new
    // method. However leaving it raw like this means that the method will be ignored in old versions
    // of Flutter but used as an override anyway wherever it's actually defined.
    // TODO(mklim): Add the @Override annotation once flutter/engine#9727 rolls to stable.
    public void onInputConnectionLocked() {
        if (webView instanceof InputAwareWebView) {
            ((InputAwareWebView) webView).lockInputConnection();
        }
    }

    // @Override
    // This is overriding a method that hasn't rolled into stable Flutter yet. Including the
    // annotation would cause compile time failures in versions of Flutter too old to include the new
    // method. However leaving it raw like this means that the method will be ignored in old versions
    // of Flutter but used as an override anyway wherever it's actually defined.
    // TODO(mklim): Add the @Override annotation once stable passes v1.10.9.
    public void onFlutterViewAttached(@NonNull View flutterView) {
        if (webView instanceof InputAwareWebView) {
            ((InputAwareWebView) webView).setContainerView(flutterView);
        }
    }

    // @Override
    // This is overriding a method that hasn't rolled into stable Flutter yet. Including the
    // annotation would cause compile time failures in versions of Flutter too old to include the new
    // method. However leaving it raw like this means that the method will be ignored in old versions
    // of Flutter but used as an override anyway wherever it's actually defined.
    // TODO(mklim): Add the @Override annotation once stable passes v1.10.9.
    public void onFlutterViewDetached() {
        if (webView instanceof InputAwareWebView) {
            ((InputAwareWebView) webView).setContainerView(null);
        }
    }

    @Override
    public void onMethodCall(MethodCall methodCall, @NonNull Result result) {
        switch (methodCall.method) {
            case "loadUrl":
                loadUrl(methodCall, result);
                break;
            case "updateSettings":
                updateSettings(methodCall, result);
                break;
            case "canGoBack":
                canGoBack(result);
                break;
            case "canGoForward":
                canGoForward(result);
                break;
            case "goBack":
                goBack(result);
                break;
            case "goForward":
                goForward(result);
                break;
            case "reload":
                reload(result);
                break;
            case "currentUrl":
                currentUrl(result);
                break;
            case "evaluateJavascript":
                evaluateJavaScript(methodCall, result);
                break;
            case "addJavascriptChannels":
                addJavaScriptChannels(methodCall, result);
                break;
            case "removeJavascriptChannels":
                removeJavaScriptChannels(methodCall, result);
                break;
            case "clearCache":
                clearCache(result);
                break;
            case "getTitle":
                getTitle(result);
                break;
            case "scrollTo":
                scrollTo(methodCall, result);
                break;
            case "scrollBy":
                scrollBy(methodCall, result);
                break;
            case "getScrollX":
                getScrollX(result);
                break;
            case "getScrollY":
                getScrollY(result);
                break;
            default:
                result.notImplemented();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadUrl(MethodCall methodCall, Result result) {
        Map<String, Object> request = (Map<String, Object>) methodCall.arguments;
        String url = (String) request.get("url");
        Map<String, String> headers = (Map<String, String>) request.get("headers");
        if (headers == null) {
            headers = Collections.emptyMap();
        }
        webView.loadUrl(url, headers);
        result.success(null);
    }

    private void canGoBack(Result result) {
        result.success(webView.canGoBack());
    }

    private void canGoForward(Result result) {
        result.success(webView.canGoForward());
    }

    private void goBack(Result result) {
        if (webView.canGoBack()) {
            webView.goBack();
        }
        result.success(null);
    }

    private void goForward(Result result) {
        if (webView.canGoForward()) {
            webView.goForward();
        }
        result.success(null);
    }

    private void reload(Result result) {
        webView.reload();
        result.success(null);
    }

    private void currentUrl(Result result) {
        result.success(webView.getUrl());
    }

    @SuppressWarnings("unchecked")
    private void updateSettings(MethodCall methodCall, Result result) {
        applySettings((Map<String, Object>) methodCall.arguments);
        result.success(null);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void evaluateJavaScript(MethodCall methodCall, final Result result) {
        String jsString = (String) methodCall.arguments;
        if (jsString == null) {
            throw new UnsupportedOperationException("JavaScript string cannot be null");
        }
        webView.evaluateJavascript(
                jsString,
                new android.webkit.ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        result.success(value);
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private void addJavaScriptChannels(MethodCall methodCall, Result result) {
        List<String> channelNames = (List<String>) methodCall.arguments;
        registerJavaScriptChannelNames(channelNames);
        result.success(null);
    }

    @SuppressWarnings("unchecked")
    private void removeJavaScriptChannels(MethodCall methodCall, Result result) {
        List<String> channelNames = (List<String>) methodCall.arguments;
        for (String channelName : channelNames) {
            webView.removeJavascriptInterface(channelName);
        }
        result.success(null);
    }

    private void clearCache(Result result) {
        webView.clearCache(true);
        WebStorage.getInstance().deleteAllData();
        result.success(null);
    }

    private void getTitle(Result result) {
        result.success(webView.getTitle());
    }

    private void scrollTo(MethodCall methodCall, Result result) {
        Map<String, Object> request = methodCall.arguments();
        int x = (int) request.get("x");
        int y = (int) request.get("y");

        webView.scrollTo(x, y);

        result.success(null);
    }

    private void scrollBy(MethodCall methodCall, Result result) {
        Map<String, Object> request = methodCall.arguments();
        int x = (int) request.get("x");
        int y = (int) request.get("y");

        webView.scrollBy(x, y);
        result.success(null);
    }

    private void getScrollX(Result result) {
        result.success(webView.getScrollX());
    }

    private void getScrollY(Result result) {
        result.success(webView.getScrollY());
    }

    private void applySettings(Map<String, Object> settings) {
        for (String key : settings.keySet()) {
            switch (key) {
                case "jsMode":
                    Integer jsMode = (Integer) settings.get(key);
                    if (jsMode != null) {
                        updateJsMode(jsMode);
                    }
                    break;
                case "hasNavigationDelegate":
                    final boolean hasNavigationDelegate = (boolean) settings.get(key);

                    final WebViewClient webViewClient =
                            flutterWebViewClient.createWebViewClient(hasNavigationDelegate);

                    webView.setWebViewClient(webViewClient);
                    break;
                case "debuggingEnabled":
                    final boolean debuggingEnabled = (boolean) settings.get(key);
                    WebView.setWebContentsDebuggingEnabled(debuggingEnabled);
                    break;
                case "hasProgressTracking":
                    flutterWebViewClient.hasProgressTracking = (boolean) settings.get(key);
                    break;
                case "gestureNavigationEnabled":
                    break;
                case "userAgent":
                    updateUserAgent((String) settings.get(key));
                    break;
                case "allowsInlineMediaPlayback":
                    // no-op inline media playback is always allowed on Android.
                    break;
                case "mixedContentMode":
                    Integer mixedContentMode = (Integer) settings.get(key);
                    if (mixedContentMode != null) {
                        updateMixedContentMode(mixedContentMode);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown WebView setting: " + key);
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void updateJsMode(int mode) {
        switch (mode) {
            case 0: // disabled
                webView.getSettings().setJavaScriptEnabled(false);
                break;
            case 1: // unrestricted
                webView.getSettings().setJavaScriptEnabled(true);
                break;
            default:
                throw new IllegalArgumentException("Trying to set unknown JavaScript mode: " + mode);
        }
    }

    private void updateAutoMediaPlaybackPolicy(int mode) {
        // This is the index of the AutoMediaPlaybackPolicy enum, index 1 is always_allow, for all
        // other values we require a user gesture.
        boolean requireUserGesture = mode != 1;
        webView.getSettings().setMediaPlaybackRequiresUserGesture(requireUserGesture);
    }

    private void updateMixedContentMode(int mode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(mode);
        }
    }

    private void registerJavaScriptChannelNames(List<String> channelNames) {
        for (String channelName : channelNames) {
            final JavaScriptChannel channel = new JavaScriptChannel(methodChannel, channelName, platformThreadHandler);
            webView.addJavascriptInterface(channel, channelName);
        }
    }

    private void updateUserAgent(String userAgent) {
        webView.getSettings().setUserAgentString(userAgent);
    }

    @Override
    public void dispose() {
        methodChannel.setMethodCallHandler(null);
        if (webView instanceof InputAwareWebView) {
            ((InputAwareWebView) webView).dispose();
        }
        webView.destroy();
    }

    public void setActivity(Activity activity) {
        flutterWebChromeClient.setActivity(activity);
    }
}
