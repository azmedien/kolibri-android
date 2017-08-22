package ch.yanova.kolibri.coordinators;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriApp;
import ch.yanova.kolibri.KolibriCoordinator;
import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.components.OnAmpDataFoundListener;
import ch.yanova.kolibri.components.WebViewListener;

/**
 * Created by lekov on 3/28/17.
 */

public class WebViewCoordinator extends KolibriCoordinator<KolibriWebView> implements WebViewListener, OnAmpDataFoundListener {

    private static final String[] sURIs = new String[]{"kolibri://content/link", "kolibri://notification"};

    private static final String GET_HTML_STRING = "javascript:window.GetHtml.processHTML('<head>'+document.getElementsByTagName('head')[0].innerHTML+'</head>');";
    private static final String JS_INTERFACE_NAME = "GetHtml";

    public static final String META_IMAGE = "og:image";
    public static final String META_TITLE = "og:title";
    public static final String META_URL = "og:url";

    public static final String META_CANONICAL = "canonical";

    public static final String META_CATEGORY = "kolibri-category";
    public static final String META_FAVORIZABLE = "kolibri-favorizable";
    public static final String META_SHAREABLE = "kolibri-shareable";

    public static final String TAG_META = "meta";
    public static final String TAG_LINK = "link";

    public static final String ATTR_CONTENT = "content";
    public static final String ATTR_PROPERTY = "property";
    public static final String ATTR_REL = "rel";
    public static final String ATTR_HREF = "href";

    public static final String META_THEME_COLOR = "theme-color";

    private static final String TAG = "WebViewCoordinator";
    public static final String NAME = "name";

    private final OnAmpDataFoundListener listener;

    public WebViewCoordinator(@NonNull OnAmpDataFoundListener listener) {
        this.listener = listener;
    }

    @Override
    protected void attach(KolibriWebView view) {
        super.attach(view);
        view.setWebViewListener(this);
        view.addJavascriptInterface(new GetHtmlJsInterface(), JS_INTERFACE_NAME);
    }

    @Override
    protected void handleIntent(KolibriWebView view, Intent intent) {
        final UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(intent.getData().toString());

        if (sanitizer.hasParameter("url")) {

            final Uri url = Uri.parse(intent.getData().getQueryParameter("url"));

            //FIX ME: Not passing the intent extras, lose TITLE for example in case the client
            // starts internal webview activity
            final boolean handled = view.getWebClient().handleUri(view.getContext(), url);

            if (!handled) {
                view.loadUrl(url.toString());
            }
        }
    }

    private void handleFavorizable(String favorizable, String url, WebView view) {
        final String trueLiteral = String.valueOf(Boolean.TRUE);
        String uriString = trueLiteral.equals(favorizable) ?
                ActionButtonCoordinator.URI_SHOW :
                ActionButtonCoordinator.URI_HIDE;

        uriString += "?url=" + url;

        final Intent intent = Kolibri.createIntent(Uri.parse(uriString));

        Kolibri.notifyComponents(view.getContext(), intent);
    }

    @Override
    public void onFound(Map<String, String> data) {
        listener.onFound(data);

        handleFavorizable(data.get(META_FAVORIZABLE), view.getUrl(), view);

        KolibriApp.getInstance().reportToFirebase(data.get(META_CATEGORY), view.getUrl());
    }

    private class GetHtmlJsInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        void processHTML(String html) {
            Document content = Jsoup.parseBodyFragment(html);
            Elements elements = content.getElementsByTag(TAG_META);
            Log.i("PARSING", "processHTML meta: " + elements);
            final Map<String, String> metaData = new HashMap<>();

            for (Element link : elements) {

                // Get current page theme color
                if (link.hasAttr(NAME) && link.attr(NAME).equals(META_THEME_COLOR)) {
                    metaData.put(META_THEME_COLOR, link.attr(ATTR_CONTENT));
                    continue;
                }

                if (link.hasAttr(NAME)
                        && (link.attr(NAME).equals(META_CATEGORY)
                        || link.attr(NAME).equals(META_FAVORIZABLE)
                        || link.attr(NAME).equals(META_SHAREABLE))) {
                    metaData.put(link.attr(NAME), link.attr(ATTR_CONTENT));
                    continue;
                }


                if (!link.hasAttr(ATTR_PROPERTY))
                    continue;

                final String contentData = link.attr(ATTR_CONTENT);
                final String key = link.attr(ATTR_PROPERTY);

                metaData.put(key, contentData);
            }

            elements = content.getElementsByTag(TAG_LINK);
            Log.i("PARSING", "processHTML link: " + elements);

            for (Element element : elements) {
                if (element.hasAttr(ATTR_REL) && element.attr(ATTR_REL).equals(META_CANONICAL)) {
                    metaData.put(element.attr(ATTR_REL), element.attr(ATTR_HREF));
                }
            }

            // There's no need to report if actually there's no data
            if (metaData.size() > 0) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        onFound(metaData);
                    }
                });
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
    }

    @Override
    public void onPageVisible(WebView view, String url) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript(
                    "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
                    new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String html) {
                            new GetHtmlJsInterface().processHTML(html);
                        }
                    });
        } else {
            view.loadUrl(GET_HTML_STRING);
        }
    }

    @Override
    public void onPageFinished(final WebView view, String url) {}

    @Override
    public boolean shouldHandleInternal() {
        return false;
    }

    @Override
    public boolean onCustomTarget(Uri link, String target) {
        return false;
    }

}
