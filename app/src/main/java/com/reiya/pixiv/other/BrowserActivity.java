package com.reiya.pixiv.other;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import PixivLocalReverseProxy.PixivLocalReverseProxy;
import tech.yojigen.pivisionm.R;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewFeature;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class BrowserActivity extends AppCompatActivity {
    private WebView mWebView;
    private ProgressBar mProgressBar;

    private String rewriteRecaptchaUrlIfNeeded(String url) {
        // Replace blocked Google Recaptcha domains with recaptcha.net mirrors
        // Examples:
        // www.google.com/recaptcha → www.recaptcha.net/recaptcha
        // www.gstatic.com/recaptcha → www.recaptcha.net/recaptcha
        String rewritten = url
                .replace("https://www.google.com/recaptcha/", "https://www.recaptcha.net/recaptcha/")
                .replace("https://www.gstatic.com/recaptcha/", "https://www.recaptcha.net/recaptcha/");
        return rewritten;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        PixivLocalReverseProxy.startServer("12345");
        mWebView = findViewById(R.id.webview);
        mProgressBar = findViewById(R.id.bar);
        findViewById(R.id.close).setOnClickListener(v -> {
            Intent intent = new Intent(BrowserActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
        String loginUrl = getIntent().getStringExtra("loginUrl");
        boolean isNeedProxy = getIntent().getBooleanExtra("isNeedProxy", false);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);
        }
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mProgressBar.setProgress(newProgress);
                super.onProgressChanged(view, newProgress);
            }
        });
        mWebView.setWebViewClient(new WebViewClientCompat() {
            @SuppressLint("WebViewClientOnReceivedSslError")
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull WebResourceRequest request) {
                if (request.getUrl().getScheme().equals("pixiv")) {
                    Intent intent = new Intent(BrowserActivity.this, LoginActivity.class);
                    intent.putExtra("codeUrl", request.getUrl().toString());
                    startActivity(intent);
                    finish();
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(@NonNull WebView view, @NonNull WebResourceRequest request) {
                String originalUrl = request.getUrl().toString();
                String rewritten = rewriteRecaptchaUrlIfNeeded(originalUrl);
                if (!originalUrl.equals(rewritten)) {
                    try {
                        HttpURLConnection connection = (HttpURLConnection) new URL(rewritten).openConnection();
                        connection.setConnectTimeout(8000);
                        connection.setReadTimeout(15000);
                        connection.setRequestProperty("User-Agent", view.getSettings().getUserAgentString());
                        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
                        connection.connect();
                        String contentType = connection.getContentType();
                        String mime = contentType != null && contentType.contains("/") ? contentType.split(";")[0] : "text/plain";
                        String encoding = connection.getContentEncoding();
                        InputStream stream = connection.getInputStream();
                        return new WebResourceResponse(mime, encoding, stream);
                    } catch (Exception e) {
                        Log.w("PixivWebView", "recaptcha rewrite failed: " + e.getMessage());
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        System.out.println(isNeedProxy);
        if (isNeedProxy) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyConfig proxyConfig = new ProxyConfig.Builder().addProxyRule("127.0.0.1:12345").addDirect().build();
                ProxyController.getInstance().setProxyOverride(proxyConfig, Runnable::run, () -> {
                    Log.w("PixivWebView", "WebView proxy init");
                    Map<String, String> header = new HashMap<>();
                    header.put("Accept-Language", "zh_CN");
                    header.put("App-Accept-Language", "zh-hans");
                    mWebView.loadUrl(loginUrl, header);
                });
            }
        }else{
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyConfig proxyConfig = new ProxyConfig.Builder().addDirect().build();
                ProxyController.getInstance().setProxyOverride(proxyConfig, Runnable::run, () -> {
                    Map<String, String> header = new HashMap<>();
                    header.put("Accept-Language", "zh_CN");
                    header.put("App-Accept-Language", "zh-hans");
                    mWebView.loadUrl(loginUrl, header);
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        PixivLocalReverseProxy.stopServer();
        super.onDestroy();
    }
}