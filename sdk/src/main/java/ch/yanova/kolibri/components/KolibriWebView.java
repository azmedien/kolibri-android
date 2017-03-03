package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import ch.yanova.kolibri.KolibriActivity;

/**
 * Created by mmironov on 2/26/17.
 */

public class KolibriWebView extends WebView implements KolibriComponent {

    public static final String TAG = "KolibriWebView";

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

    private void init() {

        if (!isInEditMode()) {
            setWebViewClient(new KolibriWebViewClient());
            getSettings().setJavaScriptEnabled(true);
        }

    }

    @Override
    public void handleIntent(Intent intent) {
        String url = intent.getData().getQueryParameter("url");
        KolibriWebViewClient client = new KolibriWebViewClient();
        boolean handled = client.handleUri(getContext(), Uri.parse(url));

        if (!handled) {
            loadUrl(url);
        }
    }
}
