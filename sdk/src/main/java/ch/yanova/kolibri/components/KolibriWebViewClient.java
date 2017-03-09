package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;

import ch.yanova.kolibri.ActionButtonListener;
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

    private ActionButtonListener actionButtonListener;

    protected boolean shouldHandleInternal() {
        return true;
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
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return preprocessRequest(url);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, final WebResourceRequest request) {
        return preprocessRequest(request.getUrl().toString());
    }

    private WebResourceResponse preprocessRequest(final String url) {
        final OkHttpClient client = new OkHttpClient();

        final Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "text/html")
                .addHeader("Content-Encoding", "UTF-8")
                .get()
                .build();

        try {
            final Response response = client.newCall(request).execute();

            if (response != null && response.isSuccessful()) {

                final String kolibriInfo = response.header("Kolibri-Info");

                if (kolibriInfo != null) {
                    // Quick and dirty
                    if (kolibriInfo.contains("categroy:recipie;favorit-button:yes") && actionButtonListener != null) {
                        actionButtonListener.showActionButton();
                        actionButtonListener.onActionButtonClick(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Snackbar.make(v, url, Snackbar.LENGTH_INDEFINITE);
                            }
                        });
                    }
                }

                // FIXME: https://artemzin.com/blog/android-webview-io/
                return new WebResourceResponse("text/html", "UTF-8", response.body().byteStream());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    boolean handleUri(Context context, Uri link) {
        String target = link.getQueryParameter(PARAM_TARGET);
        String host = link.getHost();

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

    public void setActionButtonListener(ActionButtonListener actionButtonListener) {
        this.actionButtonListener = actionButtonListener;
    }
}
