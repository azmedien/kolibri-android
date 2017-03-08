package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
}
