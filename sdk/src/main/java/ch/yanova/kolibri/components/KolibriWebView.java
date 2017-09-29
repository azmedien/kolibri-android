package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import ch.yanova.kolibri.BuildConfig;
import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriApp;
import ch.yanova.kolibri.KolibriException;

import static ch.yanova.kolibri.Kolibri.TARGET_EXTERNAL;
import static ch.yanova.kolibri.Kolibri.TARGET_INTERNAL;
import static ch.yanova.kolibri.Kolibri.TARGET_SELF;

/**
 * Created by mmironov on 2/26/17.
 */

public class KolibriWebView extends WebView {

    public static final String UA_STRING_PREFIX = "Kolibri/" + BuildConfig.VERSION_NAME;
    private static final String GET_HTML_STRING = "javascript:window.GetHtml.processHTML('<head>'+document.getElementsByTagName('head')[0].innerHTML+'</head>');";

    private KolibriWebViewClient webClient;
    private WebChromeClient chromeClient;

    private Intent intent;

    private boolean clearHistory;

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

    private void init() {

        if (!isInEditMode()) {

            InternalWebViewClient internalWebViewClient = new InternalWebViewClient();

            setLayerType(View.LAYER_TYPE_NONE, null);
            super.setWebViewClient(internalWebViewClient);
            super.setWebChromeClient(new KolibriWebChromeClient());

            getSettings().setJavaScriptEnabled(true);
            getSettings().setAppCacheEnabled(true);
            getSettings().setDomStorageEnabled(true);
            getSettings().setUserAgentString(UA_STRING_PREFIX + " " + getSettings().getUserAgentString());

            clearHistory = false;
        }
    }

    @Override
    public final void setWebViewClient(WebViewClient client) {
        throw new KolibriException("Cannot preset WebView client. Use #setKolibriWebViewClient instead.");
    }

    @Override
    public final void setWebChromeClient(WebChromeClient client) {
    }

    public void setKolibriWebViewClient(KolibriWebViewClient client) {
        webClient = client;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    private boolean handleInNewView(String target) {
        if (target == null) {
            target = TARGET_SELF;
        }

        if (TARGET_INTERNAL.equals(target) && !webClient.shouldHandleInternal()) {
            target = TARGET_SELF;
        }

        return !TARGET_SELF.equals(target);
    }

    public boolean handleUri(Uri link) {
        final Context context = getContext();
        final String target = Kolibri.getInstance(getContext()).getTarget(link);

        final boolean handleInNewView = handleInNewView(target);

        if (handleInNewView) {

            if (!webClient.onCustomTarget(link, target)) {
                Intent linkIntent = target.equals(TARGET_INTERNAL) ?
                        new Intent(Intent.ACTION_VIEW, Uri.parse("kolibri://internal/webview?url=" + link)) :
                        new Intent(Intent.ACTION_VIEW, link);

                final Intent kolibriIntent = getIntent();

                if (kolibriIntent != null) {
                    linkIntent.putExtras(kolibriIntent);
                }

                context.startActivity(linkIntent);

                final String scheme = link.getScheme();

                if (scheme.equals(Kolibri.getInstance(context).getRuntime().getScheme())) {
                    Kolibri.notifyComponents(context, Kolibri.createIntent(link));
                }
            }
        }

        return handleInNewView;
    }

    private class InternalWebViewClient extends WebViewClient {

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri link = Uri.parse(url);
            return handleUri(link);
        }

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri link = request.getUrl();
            return handleUri(link);
        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onPageCommitVisible(WebView view, String url) {
            super.onPageCommitVisible(view, url);

            if ("about:blank".equals(url)) {
                return;
            }

            final Uri link = Uri.parse(url);
            final String target = Kolibri.getInstance(view.getContext()).getTarget(link);

            // Skip external targets when reporting to netmetrix
            if (!TARGET_EXTERNAL.equals(target)) {
                KolibriApp.getInstance().logEvent(null, link.toString());
            }

            if (webClient != null) {
                webClient.onPageCommitVisible(view, url);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            if (shouldClearHistory()) {
                setClearHistory(false);
                clearHistory();
            }

            view.loadUrl(GET_HTML_STRING);

            if (webClient != null) {
                webClient.onPageFinished(view, url);
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            if (webClient != null) {
                webClient.onPageStarted(view, url, favicon);
            }
        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);

            if (webClient != null) {
                webClient.onReceivedError(view, request, error);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            if (webClient != null) {
                webClient.onReceivedError(view, errorCode, description, failingUrl);
            }
        }
    }

    public boolean shouldClearHistory() {
        return clearHistory;
    }

    public void setClearHistory(boolean clearHistory) {
        this.clearHistory = clearHistory;
    }
}
