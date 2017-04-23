package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import ch.yanova.kolibri.Kolibri;

/**
 * Created by mmironov on 3/3/17.
 */

public class KolibriWebViewClient extends WebViewClient {

    public static final String PARAM_TARGET = "kolibri-target";
    public static final String TARGET_INTERNAL = "_internal";
    public static final String TARGET_EXTERNAL = "_external";
    public static final String TARGET_SELF = "_self";

    protected boolean shouldHandleInternal() {
        return listener != null && listener.shouldHandleInternal();
    }

    public interface WebClientListener {
        void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error);

        void onPageStarted(WebView view, String url, Bitmap favicon);

        void onPageFinished(WebView view, String url);

        boolean shouldHandleInternal();
    }

    private WebClientListener listener;

    public void setWebClientListener(WebClientListener listener) {
        this.listener = listener;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {

        Uri link = Uri.parse(url);
        return handleUri(view.getContext(), link);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri link = request.getUrl();
        return handleUri(view.getContext(), link);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        if (listener != null) {
            listener.onReceivedError(view, request, error);
        }
    }

    //Implemented for backwards compatibility for devices running Android < Marshmallow (API 23)
    @SuppressWarnings("deprecation")
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        if (listener != null) {
            listener.onReceivedError(view, null, null);
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if (listener != null) {
            listener.onPageStarted(view, url, favicon);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (listener != null) {
            listener.onPageFinished(view, url);
        }
    }

    public boolean handleInNewView(String target) {
        if (target == null) {
            target = TARGET_SELF;
        }

        if (TARGET_INTERNAL.equals(target) && !shouldHandleInternal()) {
            target = TARGET_SELF;
        }

        return !TARGET_SELF.equals(target);

    }

    public boolean handleUri(Context context, Uri link) {
        String target = link.getQueryParameter(PARAM_TARGET);

        if (target == null) {
            final String domain = Kolibri.getInstance(context).getRuntime().getDomain();

            if (link.getHost().equals(domain)) {
                target = TARGET_INTERNAL;
            } else {
                target = TARGET_EXTERNAL;
            }
        }

        final boolean handleInNewView = handleInNewView(target);

        if (handleInNewView) {
            Intent linkIntent = target.equals(TARGET_INTERNAL) ?
                    new Intent(Intent.ACTION_VIEW, Uri.parse("kolibri://internal/webview?url=" + link)) :
                    new Intent(Intent.ACTION_VIEW, link);

            context.startActivity(linkIntent);

            final String scheme = link.getScheme();

            //TODO: Fix me. This is still experimental.
            if (scheme.equals(Kolibri.getInstance(context).getRuntime().getScheme())) {
                Kolibri.notifyComponents(context, Kolibri.createIntent(link));
            }
        }

        return handleInNewView;
    }
}
