package ch.yanova.kolibri;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.ProgressBar;

import java.util.Map;

import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.components.KolibriWebViewClient;
import ch.yanova.kolibri.components.OnAmpDataFoundListener;
import ch.yanova.kolibri.coordinators.WebViewCoordinator;

import static ch.yanova.kolibri.RuntimeConfig.THEME_COLOR_PRIMARY;
import static ch.yanova.kolibri.RuntimeConfig.THEME_COLOR_PRIMARY_DARK;

/**
 * Created by lekov on 4/2/17.
 */

public class WebViewFragment extends KolibriLoadingFragment implements OnAmpDataFoundListener {

    private static final String TAG = "WebViewFragment";
    private Intent shareIntent;
    private boolean isThemeTinted;
    private boolean showSearchOption = true;
    private boolean showShareOption;

    public KolibriWebView getWebView() {
        return webView;
    }

    private KolibriWebView webView;

    public static WebViewFragment newInstance() {

        Bundle args = new Bundle();

        WebViewFragment fragment = new WebViewFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        webView = view.findViewById(R.id.webview);
        webView.setTag(KolibriWebView.class);
        webView.setKolibriWebViewClient(new KolibriWebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                showShareOption = false;
                shareIntent = null;

                if (view == null) {
                    return;
                }

                if (getActivity() instanceof KolibriNavigationActivity && isThemeTinted) {
                    final KolibriNavigationActivity kna = (KolibriNavigationActivity) getActivity();
                    kna.applyDefaultPalette();

                    TintUtils.tintProgressBar((ProgressBar) mLayoutLoading);

                    isThemeTinted = false;
                }

                getActivity().invalidateOptionsMenu();
                showPageLoading();
            }

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                showPage();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                showPage();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                final String errorMessage = error != null ? error.toString() : null;

                showPageError(errorMessage, "Click", null);
            }
        });

        if (getArguments().containsKey("url")) {
            final String url = getArguments().getString("url");
            webView.loadUrl(url);
        }

        setHasOptionsMenu(true);
        ActivityCompat.invalidateOptionsMenu(getActivity());
        showPageLoading();
    }

    @NonNull
    @Override
    protected View getMainContentView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.kolibri_web_fragment, container, false);
    }


    @Override
    public void onFound(Map<String, String> data) {
        if (data.size() > 0 && data.containsKey(WebViewCoordinator.META_SHAREABLE)) {

            final String url = data.containsKey(WebViewCoordinator.META_CANONICAL)
                    ? data.get(WebViewCoordinator.META_CANONICAL)
                    : data.get(WebViewCoordinator.META_URL);

            if (url != null) {
                shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, data.get(WebViewCoordinator.META_TITLE));
                shareIntent.putExtra(Intent.EXTRA_TEXT, url);

                showShareOption = true;
            } else {
                showShareOption = false;
                shareIntent = null;
            }

            getActivity().invalidateOptionsMenu();
        }

        if (data.containsKey(WebViewCoordinator.META_THEME_COLOR) && data.get(WebViewCoordinator.META_THEME_COLOR).length() > 0) {
            final int[] palette = RuntimeConfig.getMaterialPalette(data.get(WebViewCoordinator.META_THEME_COLOR));

            if (isThemeTinted || palette.length == 0) {
                return;
            }

            isThemeTinted = true;

            getWebView().post(new Runnable() {
                @Override
                public void run() {

                    if (getActivity() instanceof KolibriNavigationActivity) {
                        final Toolbar toolbar = ((KolibriNavigationActivity) getActivity()).getToolbar();
                        TintUtils.tintToolbar(getActivity(), toolbar, palette[THEME_COLOR_PRIMARY], palette[THEME_COLOR_PRIMARY_DARK], false);
                        TintUtils.tintProgressBar((ProgressBar) mLayoutLoading, palette[THEME_COLOR_PRIMARY]);

                        final KolibriNavigationActivity kna = (KolibriNavigationActivity) getActivity();
                        kna.applyColorPalette(palette);
                    }
                }
            });
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final MenuItem search = menu.findItem(R.id.action_search);
        final MenuItem share = menu.findItem(R.id.action_share);

        search.setVisible(showSearchOption);
        share.setVisible(showShareOption && shareIntent != null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_webview, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final int i = item.getItemId();

        if (i == R.id.action_share) {
            startActivity(Intent.createChooser(shareIntent, "Share link!"));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setShowShareOption(boolean showShareOption) {
        this.showShareOption = showShareOption;
    }

    public void setShowSearchOption(boolean showSearchOption) {
        this.showSearchOption = showSearchOption;
    }
}
