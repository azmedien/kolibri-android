package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import ch.yanova.kolibri.BuildConfig;
import ch.yanova.kolibri.KolibriApp;
import ch.yanova.kolibri.KolibriCoordinator;

/**
 * Created by mmironov on 2/26/17.
 */

public class KolibriWebView extends WebView {

    public static final String UA_STRING_PREFIX = "Kolibri/" + BuildConfig.VERSION_NAME;

    private KolibriWebViewClient client;


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

    public void setWebClientListener(KolibriWebViewClient.WebClientListener listener) {
        client.setWebClientListener(listener);
    }

    private void init() {

        if (!isInEditMode()) {
            setLayerType(View.LAYER_TYPE_NONE, null);
            setWebViewClient(new KolibriWebViewClient());
            getSettings().setJavaScriptEnabled(true);
            getSettings().setAppCacheEnabled(true);
            getSettings().setDomStorageEnabled(true);
            getSettings().setUserAgentString(UA_STRING_PREFIX + " " + getSettings().getUserAgentString());

            KolibriApp.getInstance().setUserAgent(getSettings().getUserAgentString());
        }
    }

    @Override
    public void setWebViewClient(WebViewClient client) {

        if (this.client != null) {
            throw new IllegalAccessError("Cannot override kolibri's webview client");
        } else {
            super.setWebViewClient(client);
            this.client = (KolibriWebViewClient) client;
        }
    }

    public KolibriWebViewClient getClient() {
        return client;
    }
}
