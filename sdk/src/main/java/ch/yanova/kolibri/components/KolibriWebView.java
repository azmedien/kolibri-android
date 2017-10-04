package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;

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

    private List<KolibriWebViewClient> webClients;
    private List<KolibriWebChromeClient> webChromeClients;

    private Intent intent;

    private boolean shouldHandleInternal;
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

            final InternalWebViewClient internalWebViewClient = new InternalWebViewClient();
            final InternalChromeClient internalChromeClient = new InternalChromeClient();

            webClients = new ArrayList<>();
            webChromeClients = new ArrayList<>();

            super.setWebViewClient(internalWebViewClient);
            super.setWebChromeClient(internalChromeClient);

            setVerticalScrollBarEnabled(false);
            setHorizontalScrollBarEnabled(false);

            getSettings().setJavaScriptEnabled(true);
            getSettings().setAppCacheEnabled(true);
            getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
            getSettings().setDomStorageEnabled(true);
            getSettings().setUserAgentString(UA_STRING_PREFIX + " " + getSettings().getUserAgentString());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // chromium, enable hardware acceleration
                setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else {
                // older android version, disable hardware acceleration
                setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }

            getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

            clearHistory = false;
        }
    }

    @Override
    public final void setWebViewClient(WebViewClient client) {
        throw new KolibriException("Cannot preset WebView client. Use #setKolibriWebViewClient instead.");
    }

    @Override
    public final void setWebChromeClient(WebChromeClient client) {
        throw new KolibriException("Cannot preset WebChrome client. Use #setKolibriWebChromeClient instead.");
    }

    public void addKolibriWebViewClient(@NonNull KolibriWebViewClient client) {
        if (client != null) {
            webClients.add(client);
        }
    }

    public void addKolibriWebChromeClient(@NonNull KolibriWebChromeClient client) {
        if (client != null) {
            webChromeClients.add(client);
        }
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

        if (TARGET_INTERNAL.equals(target) && !shouldHandleInternal) {
            target = TARGET_SELF;
        }

        return !TARGET_SELF.equals(target);
    }

    public boolean handleUri(Uri link) {
        final Context context = getContext();
        final String target = Kolibri.getInstance(getContext()).getTarget(link);

        final boolean handleInNewView = handleInNewView(target);
        boolean handleCustomTarget = false;

        if (handleInNewView) {

            for (KolibriWebViewClient webClient : webClients) {
                if (webClient.onCustomTarget(link, target)) {
                    handleCustomTarget = true;
                    break;
                }
            }

            if (!handleCustomTarget) {
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

    private class InternalChromeClient extends WebChromeClient {


        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && newProgress == 100) {
                final String url = view.getUrl();
                if ("about:blank".equals(url)) {
                    return;
                }

                final Uri link = Uri.parse(url);
                final String target = Kolibri.getInstance(view.getContext()).getTarget(link);

                // Skip external targets when reporting to netmetrix
                if (!TARGET_EXTERNAL.equals(target)) {
                    KolibriApp.getInstance().logEvent(null, link.toString());
                }
            }
            for (KolibriWebChromeClient webChromeClient : webChromeClients) {
                webChromeClient.onProgressChanged(view, newProgress);
            }

        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);
            for (KolibriWebChromeClient webChromeClient : webChromeClients) {
                webChromeClient.onShowCustomView(view, callback);
            }
        }

        @Override
        public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
            super.onShowCustomView(view, requestedOrientation, callback);
            for (KolibriWebChromeClient webChromeClient : webChromeClients) {
                webChromeClient.onShowCustomView(view, requestedOrientation, callback);
            }
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
            for (KolibriWebChromeClient webChromeClient : webChromeClients) {
                webChromeClient.onHideCustomView();
            }
        }

        @Override
        public View getVideoLoadingProgressView() {

            for (KolibriWebChromeClient webChromeClient : webChromeClients) {
                if (webChromeClient.getVideoLoadingProgressView() != null) {
                    return webChromeClient.getVideoLoadingProgressView();
                }
            }

            return super.getVideoLoadingProgressView();
        }

        @Override
        public void getVisitedHistory(ValueCallback<String[]> callback) {
            super.getVisitedHistory(callback);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

            for (KolibriWebChromeClient webChromeClient : webChromeClients) {
                if (webChromeClient.onShowFileChooser(webView, filePathCallback, fileChooserParams)) {
                    return true;
                }
            }

            return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);

        }
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


            for (KolibriWebViewClient webClient : webClients) {
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

            for (KolibriWebViewClient webClient : webClients) {
                webClient.onPageFinished(view, url);
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            for (KolibriWebViewClient webClient : webClients) {
                webClient.onPageStarted(view, url, favicon);
            }
        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            Crashlytics.log(5, "KolibriWebView", "onReceivedError() called with: view = [" + view + "], request = [" + request + "], error = [" + error + "]");

            // We ignore errors regarding assets loading,
            // so in this case we check if the request url and webview url are same
            if (request.getUrl().toString().equals(view.getUrl()) || request.isForMainFrame()) {
                for (KolibriWebViewClient webClient : webClients) {
                    webClient.onReceivedError(view, request, error);
                }
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Crashlytics.log(5, "KolibriWebView", "onReceivedError() called with: view = [" + view + "], errorCode = [" + errorCode + "], description = [" + description + "], failingUrl = [" + failingUrl + "]");

            for (KolibriWebViewClient webClient : webClients) {
                webClient.onReceivedError(view, errorCode, description, failingUrl);
            }
        }
    }

    public boolean shouldHandleInternal() {
        return shouldHandleInternal;
    }

    public void setHandleInternal(boolean handleInternal) {
        this.shouldHandleInternal = handleInternal;
    }

    public boolean shouldClearHistory() {
        return clearHistory;
    }

    public void setClearHistory(boolean clearHistory) {
        this.clearHistory = clearHistory;
    }
}
