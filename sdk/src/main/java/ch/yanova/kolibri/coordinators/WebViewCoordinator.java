package ch.yanova.kolibri.coordinators;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriCoordinator;
import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.components.KolibriWebViewClient;
import ch.yanova.kolibri.components.OnAmpDataFoundListener;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by lekov on 3/28/17.
 */

public class WebViewCoordinator extends KolibriCoordinator<KolibriWebView> implements KolibriWebViewClient.WebClientListener, OnAmpDataFoundListener {

    private static final String[] sURIs = new String[]{"kolibri://content/link"};

    public static final String HEADER_FAVORITES = "Kolibri-Favorizable";

    private static final String GET_HTML_STRING = "javascript:window.GetHtml.processHTML('<head>'+document.getElementsByTagName('head')[0].innerHTML+'</head>');";
    private static final String JS_INTERFACE_NAME = "GetHtml";

    public static final String FAV_IMAGE = "og:image";
    public static final String FAV_LABEL = "og:title";
    public static final String META_CANONICAL_URL = "og:url";
    public static final String ATTR_CONTENT = "content";
    public static final String TAG_META = "meta";
    public static final String ATTR_PROPERTY = "property";
    public static final String META_THEME_COLOR = "theme-color";

    private static final String TAG = "WebViewCoordinator";

    public static final int THEME_COLOR_PRIMARY = 5;
    public static final int THEME_COLOR_PRIMARY_LIGHT = 1;
    public static final int THEME_COLOR_PRIMARY_DARK = 7;
    public static final int THEME_COLOR_ACCENT = 11;

    private final OnAmpDataFoundListener listener;
    private String mCurrentThemeColor;

    public WebViewCoordinator(OnAmpDataFoundListener listener) {
        this.listener = listener;
    }

    public WebViewCoordinator() {
        this(null);
    }

    @Override
    protected void attach(KolibriWebView view) {
        super.attach(view);
        view.setWebClientListener(this);
        view.addJavascriptInterface(new GetHtmlJsInterface(), JS_INTERFACE_NAME);
    }

    @Override
    protected void handleIntent(KolibriWebView view, Intent intent) {
        final UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(intent.getData().toString());

        if (sanitizer.hasParameter("url")) {

            final Uri url = Uri.parse(intent.getData().getQueryParameter("url"));

            //FIX ME: Not passing the intent extras, lose TITLE for example in case the client
            // starts internal webview activity
            final boolean handled = view.getClient().handleUri(view.getContext(), url);

            if (!handled) {
                view.loadUrl(url.toString());
            }
        }
    }

    private void getHeaders(final WebView view, final String url) {
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

                    Log.i(TAG, "getHeaders: " + response.headers());

                    final String trueLiteral = String.valueOf(Boolean.TRUE);
                    final String headerFavorites = response.header(HEADER_FAVORITES);
                    String uriString = trueLiteral.equals(headerFavorites) ?
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
    public void onFound(Map<String, String> data) {
        if (listener != null) {
            listener.onFound(data);
        }
    }

    private class GetHtmlJsInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html) {
            Document content = Jsoup.parseBodyFragment(html);
            Elements links = content.getElementsByTag(TAG_META);
            Log.i("PARSING", "processHTML: " + links);
            Map<String, String> metaData = new HashMap<>();

            for (Element link : links) {

                // Get current page theme color
                if (link.hasAttr("name") && link.attr("name").equals(META_THEME_COLOR)) {
                    mCurrentThemeColor = link.attr(META_THEME_COLOR);
                    metaData.put(META_THEME_COLOR, mCurrentThemeColor);
                    continue;
                }

                if (!link.hasAttr(ATTR_PROPERTY))
                    continue;

                final String contentData = link.attr(ATTR_CONTENT);
                final String key = link.attr(ATTR_PROPERTY);

                metaData.put(key, contentData);
            }

            // There's no need to report if actually there's no data
            if (metaData.size() > 0) {
                onFound(metaData);
            }
        }
    }

    @Override
    protected String[] kolibriUris() {
        return sURIs;
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {

    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        mCurrentThemeColor = null;
        getHeaders(view, url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        view.loadUrl(GET_HTML_STRING);
    }

    @Override
    public boolean shouldHandleInternal() {
        return false;
    }

    public int[] getMaterialPalette() {
        return mCurrentThemeColor == null ? new int[]{} : getMaterialPalette(mCurrentThemeColor);
    }

    public static int[] getMaterialPalette(String color){
        int[] result = new int[14];

        result[0] = shadeColor(color, 0.9   ); //----> 50
        result[1] = shadeColor(color, 0.7   ); //----> 100
        result[2] = shadeColor(color, 0.5   ); //----> 200
        result[3] = shadeColor(color, 0.333 ); //----> 300
        result[4] = shadeColor(color, 0.166 ); //----> 400
        result[5] = shadeColor(color, 0     ); //----> 500
        result[6] = shadeColor(color, -0.125); //----> 600
        result[7] = shadeColor(color, -0.25 ); //----> 700
        result[8] = shadeColor(color, -0.375); //----> 800
        result[9] = shadeColor(color, -0.5  ); //----> 900

        result[10] = shadeColor(color, 0.7  ); //----> A100
        result[11] = shadeColor(color, 0.5  ); //----> A200
        result[12] = shadeColor(color, 0.166); //----> A400
        result[13] = shadeColor(color, -0.25); //----> A700

        return result;
    }

    private static int shadeColor(String color, double percent) {
        long f = Long.parseLong(color.substring(1), 16);
        double t = percent < 0 ? 0 : 255;
        double p = percent < 0 ? percent * -1 : percent;
        long R = f >> 16;
        long G = f >> 8 & 0x00FF;
        long B = f & 0x0000FF;
        int red = (int) (Math.round((t - R) * p) + R);
        int green = (int) (Math.round((t - G) * p) + G);
        int blue = (int) (Math.round((t - B) * p) + B);
        return Color.rgb(red, green, blue);
    }
}
