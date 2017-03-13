package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;

import ch.yanova.kolibri.Kolibri;
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
    public static final String HEADER_FAVORITES = "kolibri-favorizable";
    public static final String TRUE = "ture";
    public static final String FALSE = "false";

    protected boolean shouldHandleInternal() {
        return true;
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
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response != null && response.isSuccessful()) {

                    Log.i(TAG, "getHeaders: " + response);

                    String headerFavorites = response.header(HEADER_FAVORITES);
                    headerFavorites = TRUE;

                    String uriString = TRUE.equals(headerFavorites) ?
                            KolibriFloatingActionButton.URI_SHOW :
                            KolibriFloatingActionButton.URI_HIDE;

                    uriString += "?url=" + url;

                    final Intent intent = Kolibri.createIntent(Uri.parse(uriString));

                    Kolibri.notifyComponents(view.getContext(), intent);
                }
            }
        });
    }

    boolean handleUri(KolibriWebView view, Context context, Uri link) {
        String target = link.getQueryParameter(PARAM_TARGET);
        String host = link.getHost();

        getHeaders(view, link.toString());

        if (target == null) {
            target = host.matches(AMP_REGEX) ? TARGET_SELF : TARGET_INTERNAL;
        }

        if (TARGET_INTERNAL.equals(target) && !shouldHandleInternal()) {
            target = TARGET_SELF;
        }

        if (TARGET_SELF.equals(target)) {
            return false;
        }

        Intent linkIntent = target.equals(TARGET_INTERNAL) ?
                new Intent(context, WebViewActivity.class) :
                new Intent(Intent.ACTION_VIEW);
        linkIntent.setData(link);

        context.startActivity(linkIntent);
        return true;
    }
}
