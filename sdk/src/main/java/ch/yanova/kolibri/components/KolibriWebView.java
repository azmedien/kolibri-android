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
import android.webkit.WebViewClient;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.yanova.kolibri.BuildConfig;

/**
 * Created by mmironov on 2/26/17.
 */

public class KolibriWebView extends WebView implements KolibriComponent {

    public static final String UA_STRING_PREFIX = "Kolibri/" + BuildConfig.VERSION_NAME;

    private static final String GET_HTML_STRING = "javascript:window.GetHtml.processHTML('<head>'+document.getElementsByTagName('head')[0].innerHTML+'</head>');";
    private static final String JS_INTERFACE_NAME = "GetHtml";

    public static final String FAV_IMAGE = "og:image";
    public static final String FAV_LABEL = "og:title";
    public static final String ATTR_CONTENT = "content";
    public static final String TAG_META = "meta";
    public static final String ATTR_PROPERTY = "property";

    private KolibriWebViewClient client;

    public void setOnAmpDataFoundListener(OnAmpDataFoundListener onAmpDataFoundListener) {
        this.onAmpDataFoundListener = onAmpDataFoundListener;
    }

    private OnAmpDataFoundListener onAmpDataFoundListener;

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

    public void addWebClientListener(KolibriWebViewClient.WebClientListener listener) {
        client.addWebClientListener(listener);
    }

    private void init() {

        if (!isInEditMode()) {
            client = new KolibriWebViewClient() {

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
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

    private class GetHtmlJsInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html) {

            Document content = Jsoup.parseBodyFragment(html);
            Elements links = content.getElementsByTag(TAG_META);
            Log.i("PARSING", "processHTML: " + links);
            Map<String, String> favData = new HashMap<>();
            for (Element link : links) {
                if (FAV_IMAGE.equals(link.attr(ATTR_PROPERTY)) || FAV_LABEL.equals(link.attr(ATTR_PROPERTY))) {
                    String contentData = link.attr(ATTR_CONTENT);

                    String key = FAV_IMAGE.equals(link.attr(ATTR_PROPERTY)) ?
                            FAV_IMAGE : FAV_LABEL;

                    if (onAmpDataFoundListener != null) {
                        favData.put(key, contentData);
                        onAmpDataFoundListener.onFound(favData);
                    }
                }
            }
        }
    }
}
