package ch.yanova.kolibri.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.yanova.kolibri.BuildConfig;

/**
 * Created by mmironov on 2/26/17.
 */

public class KolibriWebView extends WebView implements KolibriComponent {

    public static final String UA_STRING_PREFIX = "Kolibri/" + BuildConfig.VERSION_NAME;

    private static final String GET_HTML_STRING = "javascript:window.GetHtml.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');";
    private static final String JS_INTERFACE_NAME = "GetHtml";

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
            client = new KolibriWebViewClient() {

                @Override
                public void onPageFinished(WebView view, String url) {
                    loadUrl(GET_HTML_STRING);
                }
            };
            setWebViewClient(client);
            getSettings().setJavaScriptEnabled(true);
            getSettings().setAppCacheEnabled(true);
            getSettings().setDomStorageEnabled(true);
            getSettings().setUserAgentString(UA_STRING_PREFIX + " " + getSettings().getUserAgentString());

            addJavascriptInterface(new GetHtmlJsInterface(), JS_INTERFACE_NAME);
        }
    }

    @Override
    public void handleIntent(Intent intent) {

        final UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(intent.getData().toString());

        if (sanitizer.hasParameter("url")) {
            String url = intent.getData().getQueryParameter("url");
            KolibriWebViewClient client = new KolibriWebViewClient();
            boolean handled = client.handleUri(this, getContext(), Uri.parse(url));

            if (!handled) {
                loadUrl(url);
            }
        }
    }

    private static class GetHtmlJsInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html) {
            
        }
    }
}
