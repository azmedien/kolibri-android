package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebView;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import ch.yanova.kolibri.ActionButtonListener;
import ch.yanova.kolibri.BuildConfig;

/**
 * Created by mmironov on 2/26/17.
 */

public class KolibriWebView extends WebView implements KolibriComponent {

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

    private void init() {

        if (!isInEditMode()) {
            client = new KolibriWebViewClient();
            setWebViewClient(client);
            getSettings().setJavaScriptEnabled(true);
            getSettings().setUserAgentString(UA_STRING_PREFIX + " " + getSettings().getUserAgentString());
        }
    }

    public void setActionButtonListener(ActionButtonListener listener) {
        if (client != null) {
            client.setActionButtonListener(listener);
        }
    }

    @Override
    public void handleIntent(Intent intent) {

        final UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(intent.getData().toString());

        if (sanitizer.hasParameter("url")) {
            String url = intent.getData().getQueryParameter("url");
            KolibriWebViewClient client = new KolibriWebViewClient();
            boolean handled = client.handleUri(getContext(), Uri.parse(url));

            if (!handled) {
                loadUrl(url);
            }
        }
    }
}
