package ch.yanova.kolibri.components;

import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

/**
 * Created by lekov on 8/22/17.
 */

public interface WebViewListener {
    void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error);

    void onPageStarted(WebView view, String url, Bitmap favicon);

    void onPageFinished(WebView view, String url);

    void onPageProgress(WebView view, int progress);

    boolean shouldHandleInternal();

    boolean onCustomTarget(Uri link, String target);
}
