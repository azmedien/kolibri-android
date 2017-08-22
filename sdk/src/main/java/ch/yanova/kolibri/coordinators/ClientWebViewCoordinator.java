package ch.yanova.kolibri.coordinators;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import ch.yanova.kolibri.components.OnAmpDataFoundListener;
import ch.yanova.kolibri.components.WebViewListener;

/**
 * Created by lekov on 3/28/17.
 */

public class ClientWebViewCoordinator extends WebViewCoordinator {

    private final WebViewListener listener;

    private boolean hasReceivedError;

    public ClientWebViewCoordinator(OnAmpDataFoundListener ampDataFoundListener, @NonNull WebViewListener listener) {
        super(ampDataFoundListener);
        this.listener = listener;
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);

        hasReceivedError = true;
        listener.onReceivedError(view, request, error);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);

        hasReceivedError = false;
        listener.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageVisible(WebView view, String url) {
        super.onPageVisible(view, url);

        listener.onPageVisible(view, url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        if (hasReceivedError) {
            listener.onReceivedError(view, null, null);
        } else {
            listener.onPageFinished(view, url);
        }
    }

    @Override
    public boolean shouldHandleInternal() {
        return false;
    }
}
