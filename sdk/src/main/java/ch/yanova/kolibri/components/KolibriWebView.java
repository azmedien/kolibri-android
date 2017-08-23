package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import ch.yanova.kolibri.BuildConfig;

/**
 * Created by mmironov on 2/26/17.
 */

public class KolibriWebView extends WebView {

    public static final String UA_STRING_PREFIX = "Kolibri/" + BuildConfig.VERSION_NAME;

    private KolibriWebViewClient webClient;
    private KolibriWebChromeClient chromeClient;

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

    public void setWebViewListener(WebViewListener listener) {
        webClient.setWebViewListener(listener);
        chromeClient.setWebViewListener(listener);
    }

    private void init() {

        if (!isInEditMode()) {
            setLayerType(View.LAYER_TYPE_NONE, null);
            setWebViewClient(new KolibriWebViewClient());
            setWebChromeClient(new KolibriWebChromeClient());

            getSettings().setJavaScriptEnabled(true);
            getSettings().setAppCacheEnabled(true);
            getSettings().setDomStorageEnabled(true);
            getSettings().setUserAgentString(UA_STRING_PREFIX + " " + getSettings().getUserAgentString());
        }
    }

    @Override
    public void setWebViewClient(WebViewClient client) {

        if (this.webClient != null) {
            throw new IllegalAccessError("Cannot override kolibri's webview webClient");
        } else {
            super.setWebViewClient(client);
            this.webClient = (KolibriWebViewClient) client;
        }
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {

        super.setWebChromeClient(client);

        if (client instanceof KolibriWebChromeClient) {
            this.chromeClient = (KolibriWebChromeClient) client;
        }

    }

    public KolibriWebViewClient getWebClient() {
        return webClient;
    }
}
