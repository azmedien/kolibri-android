package ch.yanova.kolibri;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import java.util.Map;

import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.components.KolibriWebViewClient;
import ch.yanova.kolibri.components.OnAmpDataFoundListener;
import ch.yanova.kolibri.coordinators.WebViewCoordinator;

/**
 * Created by lekov on 4/2/17.
 */

public class WebViewFragment extends KolibriLoadingFragment implements KolibriWebViewClient.WebClientListener, OnAmpDataFoundListener {

    private Intent shareIntent;

    KolibriWebView getWebView() {
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
        if (data.size() > 0 && data.containsKey(WebViewCoordinator.FAV_LABEL)) {
            final String url = data.get(WebViewCoordinator.META_CANONICAL_URL);

            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, data.get(WebViewCoordinator.FAV_LABEL));
            shareIntent.putExtra(Intent.EXTRA_TEXT, url);
        }
    }
}
