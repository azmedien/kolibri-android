package ch.yanova.kolibri;

import android.animation.Animator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

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

public class WebViewFragment extends KolibriLoadingFragment implements KolibriWebViewClient.WebClientListener, OnAmpDataFoundListener {

    private Intent shareIntent;
    private boolean isThemeTinted;
    private boolean showSearchOption = true;
    private boolean showShareOption;

    public KolibriWebView getWebView() {
        return webView;
    }

    private KolibriWebView webView;

    public static WebViewFragment newInstance(String url) {

        Bundle args = new Bundle();
        args.putString("url", url);

        WebViewFragment fragment = new WebViewFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        webView = (KolibriWebView) view.findViewById(R.id.webview);
        webView.setTag(KolibriWebView.class);
        webView.setWebClientListener(this);

        int color = ContextCompat.getColor(getContext(), R.color.colorPrimary);
        setProgressColor(color);

        if (getArguments().containsKey("url")) {
            final String url = getArguments().getString("url");
            webView.loadUrl(url);
        }

        setHasOptionsMenu(true);
    }

    @NonNull
    @Override
    protected View getMainContentView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.kolibri_web_fragment, container, false);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        final String errorMessage = error != null ? error.toString() : null;

        showPageError(errorMessage, "Click", null);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {

        showShareOption = false;
        shareIntent = null;

        if (view == null) {
            return;
        }

        final int primary = ContextCompat.getColor(webView.getContext(), R.color.colorPrimary);
        final int primaryDark = ContextCompat.getColor(webView.getContext(), R.color.colorPrimaryDark);

        if (getActivity() instanceof KolibriNavigationActivity && isThemeTinted) {
            final Toolbar toolbar = ((KolibriNavigationActivity) getActivity()).getToolbar();
            tintTheme(toolbar, primary, primaryDark, false);
            isThemeTinted = false;
        }

        showPageLoading();
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        showPage();
    }

    @Override
    public boolean shouldHandleInternal() {
        return true;
    }

    @Override
    public void onFound(Map<String, String> data) {
        if (data.size() > 0 && data.containsKey(WebViewCoordinator.META_SHAREABLE)) {
            final String url = data.get(WebViewCoordinator.META_CANONICAL_URL);

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
                        tintTheme(toolbar, palette[THEME_COLOR_PRIMARY], palette[THEME_COLOR_PRIMARY_DARK], true);
                    }
                }
            });
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem search = menu.findItem(R.id.action_search);
        MenuItem share = menu.findItem(R.id.action_share);


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

    private void tintTheme(View view, int colorPrimary, int colorPrimaryDark, boolean tint) {

        Window window = getActivity().getWindow();

        // get the center for the clipping circle
        int cx = view.getWidth() / 2;
        int cy = view.getHeight() / 2;

        // get the final radius for the clipping circle
        float finalRadius = (float) Math.hypot(cx, cy);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);
            anim.start();

            if (!tint) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(colorPrimaryDark);
            }
        }
        view.setBackgroundColor(colorPrimary);
        setProgressColor(colorPrimary);
    }

    public void setShowShareOption(boolean showShareOption) {
        this.showShareOption = showShareOption;
    }

    public void setShowSearchOption(boolean showSearchOption) {
        this.showSearchOption = showSearchOption;
    }
}
