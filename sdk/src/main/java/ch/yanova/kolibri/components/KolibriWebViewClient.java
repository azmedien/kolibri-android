package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ch.yanova.kolibri.BuildConfig;
import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.coordinators.ActionButtonCoordinator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by mmironov on 3/3/17.
 */

public class KolibriWebViewClient extends WebViewClient {

    public static final String PARAM_TARGET = "kolibri-target";
    public static final String TARGET_INTERNAL = "_internal";
    public static final String TARGET_EXTERNAL = "_external";
    public static final String TARGET_SELF = "_self";
    public static final String AMP_REGEX = "^(www\\.)?amp.*$";
    public static final String TAG = "KolibriWebViewClient";
    public static final String HEADER_FAVORITES = "Kolibri-Favorizable";
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    protected boolean shouldHandleInternal() {

        if (listener != null) {
            return listener.shouldHandleInternal();
        }

        return false;
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
        return handleUri((KolibriWebView) view, view.getContext(), link);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri link = request.getUrl();
        return handleUri((KolibriWebView) view, view.getContext(), link);
    }

    void getHeaders(final KolibriWebView view, final String url) {
        final OkHttpClient client = new OkHttpClient();

        final Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "text/html")
                .addHeader("Content-Encoding", "UTF-8")
                .addHeader("User-Agent", view.getSettings().getUserAgentString())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response != null && response.isSuccessful()) {


                    String headerFavorites = response.header(HEADER_FAVORITES);

                    Log.i(TAG, "getHeaders: " + response.headers());

                    String uriString = TRUE.equals(headerFavorites) ?
                            ActionButtonCoordinator.URI_SHOW :
                            ActionButtonCoordinator.URI_HIDE;

                    uriString += "?url=" + url;

                    final Intent intent = Kolibri.createIntent(Uri.parse(uriString));

                    Kolibri.notifyComponents(view.getContext(), intent);
                }
            }
        });
    }


    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        assert listener != null;
        listener.onReceivedError(view, request, error);

    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        assert listener != null;
        listener.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        assert listener != null;
        listener.onPageFinished(view, url);
    }

    boolean handleUri(KolibriWebView view, Context context, Uri link) {
        String target = link.getQueryParameter(PARAM_TARGET);
        String host = link.getHost();

        getHeaders(view, link.toString());

        if (target == null) {
            target = TARGET_SELF;
        }

        if (TARGET_INTERNAL.equals(target) && !shouldHandleInternal()) {
            target = TARGET_SELF;
        }

        if (TARGET_SELF.equals(target)) {
            return false;
        }

        Intent linkIntent = target.equals(TARGET_INTERNAL) ?
                new Intent(Intent.ACTION_VIEW, Uri.parse("kolibri://internal/webview?url=" + link)) :
                new Intent(Intent.ACTION_VIEW, link);

        context.startActivity(linkIntent);
        return true;
    }
}
