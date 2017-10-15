package ch.yanova.kolibri.coordinators;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriApp;
import ch.yanova.kolibri.KolibriCoordinator;
import ch.yanova.kolibri.R;
import ch.yanova.kolibri.RuntimeConfig;
import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.components.OnAmpDataFoundListener;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.NavigationViewMode;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by lekov on 3/28/17.
 */

public class WebViewCoordinator extends KolibriCoordinator<KolibriWebView> implements OnAmpDataFoundListener {

    public static final String webViewUri = "kolibri://content/link";
    private static final String[] sURIs = new String[]{webViewUri};

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

    private KolibriWebView webView;

    @Override
    protected void attach(KolibriWebView view) {
        super.attach(view);
        webView = view;
        view.addJavascriptInterface(new GetHtmlJsInterface(), JS_INTERFACE_NAME);
    }

    @Override
    protected void handleIntent(KolibriWebView view, Intent intent) {
        final UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(intent.getData().toString());

        view.setIntent(intent);

        if (sanitizer.hasParameter("url")) {

            final Uri url = Uri.parse(intent.getData().getQueryParameter("url"));

            final boolean handled = view.handleUri(url);

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

    private void handleAmpData(Map<String, String> data) {
        handleFavorizable(data.get(META_FAVORIZABLE), view.getUrl(), view);

        KolibriApp.getInstance().reportToFirebase(data.get(META_CATEGORY), view.getUrl());
    }

    @Override
    public void onFound(Map<String, String> data) {

    }

    private class GetHtmlJsInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html) {
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
                        handleAmpData(metaData);
                        onFound(metaData);

                        if (metaData.containsKey(META_THEME_COLOR)) {

                            int[] palette = RuntimeConfig.getMaterialPalette(metaData.get(META_THEME_COLOR));

                            Aesthetic.get()
                                    .colorPrimary(palette[RuntimeConfig.THEME_COLOR_PRIMARY])
                                    .colorPrimaryDark(palette[RuntimeConfig.THEME_COLOR_PRIMARY_DARK])
                                    .colorAccent(palette[13])
                                    .colorStatusBarAuto()
                                    .textColorPrimary(Color.BLACK)
                                    .navigationViewMode(NavigationViewMode.SELECTED_ACCENT)
                                    .apply();

                            webView.setTag(R.id.primaryColor, palette[RuntimeConfig.THEME_COLOR_PRIMARY]);
                            webView.setTag(R.id.accentColor, palette[13]);
                        } else {
                            // Check if there's no meta theme and we navigate to menu item.
                            // In this case we prefer default app theme
                            final RuntimeConfig config = Kolibri.getInstance(view.getContext()).getRuntime();
                            final RuntimeConfig.Navigation navigation = config.getNavigation();

                            for (String item : navigation.getItems().keySet()) {
                                final RuntimeConfig.NavigationItem navItem = navigation.getItem(item);

                                if (navItem.hasSetting("url") && navItem.getString("url").equals(webView.getOriginalUrl())) {
                                    Kolibri.getInstance(view.getContext()).applyRuntimeTheme(true);
                                    webView.setTag(R.id.primaryColor, null);
                                    webView.setTag(R.id.accentColor, null);
                                    break;
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    protected String[] kolibriUris() {
        return sURIs;
    }
}
