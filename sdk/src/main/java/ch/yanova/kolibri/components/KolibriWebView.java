package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by mmironov on 2/26/17.
 */

public class KolibriWebView extends WebView implements KolibriComponent {

    public KolibriWebView(Context context) {
        super(context);
        init();
    }

    public KolibriWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KolibriWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public KolibriWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {

        if (!isInEditMode()) {
            setWebViewClient(new WebViewClient());
            getSettings().setJavaScriptEnabled(true);
        }

    }

    @Override
    public void handleIntent(Intent intent) {
        String url = intent.getData().getQueryParameter("url");
        loadUrl(url);
    }
}
