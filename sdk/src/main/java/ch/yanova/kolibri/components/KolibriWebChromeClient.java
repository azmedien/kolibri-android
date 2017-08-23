package ch.yanova.kolibri.components;

import android.os.Build;
import android.support.annotation.NonNull;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriApp;

/**
 * Created by mmironov on 8/23/17.
 */

public class KolibriWebChromeClient extends WebChromeClient {

    private WebViewListener listener;

    public void setWebViewListener(@NonNull WebViewListener listener) {
        this.listener = listener;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && newProgress == 100) {
            if (listener != null) {
                listener.onPageVisible(view, view.getUrl());
            }

            KolibriApp.getInstance().logEvent(null, view.getUrl());
        }
    }
}
