package ch.yanova.kolibri.components;

import android.support.annotation.NonNull;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * Created by lekov on 8/22/17.
 */

public class KolibriWebChromeClient extends WebChromeClient {

    private WebViewListener listener;

    public void setWebViewListener(@NonNull WebViewListener listener) {
        this.listener = listener;
    }

    @NonNull
    public WebViewListener getWebViewListener() {
        return listener;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        listener.onPageProgress(view, newProgress);
    }
}
