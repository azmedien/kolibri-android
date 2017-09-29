package ch.yanova.kolibri.coordinators;

import android.net.Uri;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.search.OnSubmitFilteredSearchListener;

/**
 * Created by lekov on 4/2/17.
 */

public class SearchWebviewCoordinator extends WebViewCoordinator implements OnSubmitFilteredSearchListener {

    private KolibriWebView kolibriWebView;

    @Override
    protected void attach(KolibriWebView view) {
        super.attach(view);
        this.kolibriWebView = view;
    }

    @Override
    public void onQueryByTags(String query) {

        final String searchParamKey =
                Kolibri.getInstance(kolibriWebView.getContext()).getRuntime().getComponent("search").getSettings().getString("search-param");

        String url = kolibriWebView.getUrl();
        Uri uri = Uri.parse(url);


        Uri.Builder builder = uri.buildUpon();
        builder.clearQuery();

        String searchValue = uri.getQueryParameter(searchParamKey);

        if (searchValue != null) {
            builder.appendQueryParameter(searchParamKey, searchValue);
        }

        url = builder.build().toString();

        String filteredUrl = url;
        if (!url.contains("?")) {
            filteredUrl += "?";
        } else {
            query = "&" + query;
        }

        filteredUrl += query;

        kolibriWebView.loadUrl(filteredUrl);

    }

    @Override
    public void onQueryByText(String text) {

        final String searchParamKey =
                Kolibri.getInstance(kolibriWebView.getContext()).getRuntime().getComponent("search").getSettings().getString("search-param");

        String url = kolibriWebView.getUrl();
        Uri uri = Uri.parse(url);

        if (!"".equals(text)) {
            Uri.Builder builder = uri.buildUpon();
            builder.clearQuery();

            if (uri.getQueryParameterNames().size() <= 0 || uri.getQueryParameter(searchParamKey) == null) {
                builder.appendQueryParameter(searchParamKey, text);
            } else {
                for (String key : uri.getQueryParameterNames()) {
                    if (searchParamKey.equals(key)) {
                        builder.appendQueryParameter(searchParamKey, text);
                    } else {
                        builder.appendQueryParameter(key, uri.getQueryParameter(key));
                    }
                }
            }


            uri = builder.build();
        }

        kolibriWebView.loadUrl(uri.toString());
    }
}
