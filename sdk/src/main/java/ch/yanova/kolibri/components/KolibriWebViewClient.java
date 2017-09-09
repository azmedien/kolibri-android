package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriApp;

import static ch.yanova.kolibri.Kolibri.TARGET_EXTERNAL;
import static ch.yanova.kolibri.Kolibri.TARGET_INTERNAL;
import static ch.yanova.kolibri.Kolibri.TARGET_SELF;

/**
 * Created by mmironov on 3/3/17.
 */
public class KolibriWebViewClient extends WebViewClient {

    private static final String TAG = "KolibriWebClient";

    private WebViewListener listener;

    public void setWebViewListener(@NonNull WebViewListener listener) {
        this.listener = listener;
    }

    @NonNull
    public WebViewListener getWebViewListener() {
        return listener;
    }

    boolean shouldHandleInternal() {
        return listener != null && listener.shouldHandleInternal();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri link = Uri.parse(url);
        return handleUri(view, link);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri link = request.getUrl();
        return handleUri(view, link);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        listener.onReceivedError(view, request, error);
    }

    //Implemented for backwards compatibility for devices running Android < Marshmallow (API 23)
    @SuppressWarnings("deprecation")
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        listener.onReceivedError(view, null, null);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        listener.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {

        if (view instanceof KolibriWebView) {
            final KolibriWebView webView = (KolibriWebView)view;

            if (webView.shouldClearHistory()) {
                webView.setClearHistory(false);
                webView.clearHistory();
            }
        }

        super.onPageFinished(view, url);
        listener.onPageFinished(view, url);
    }

    boolean handleInNewView(String target) {
        if (target == null) {
            target = TARGET_SELF;
        }

        if (TARGET_INTERNAL.equals(target) && !shouldHandleInternal()) {
            target = TARGET_SELF;
        }

        return !TARGET_SELF.equals(target);
    }

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

        listener.onPageVisible(view, url);
    }

    public final boolean handleUri(WebView view, Uri link) {
        final Context context = view.getContext();
        final String target = Kolibri.getInstance(view.getContext()).getTarget(link);

        final boolean handleInNewView = handleInNewView(target);

        if (handleInNewView) {

            if (!listener.onCustomTarget(link, target)) {
                Intent linkIntent = target.equals(TARGET_INTERNAL) ?
                        new Intent(Intent.ACTION_VIEW, Uri.parse("kolibri://internal/webview?url=" + link)) :
                        new Intent(Intent.ACTION_VIEW, link);

                if (view instanceof KolibriWebView) {
                    final Intent kolibriIntent = ((KolibriWebView)view).getIntent();

                    if (kolibriIntent != null) {
                        linkIntent.putExtras(kolibriIntent);
                    }
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
}
