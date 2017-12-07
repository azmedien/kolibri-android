package ch.yanova.kolibri.prototype;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.components.KolibriWebViewClient;

/**
 * Created by lekov on 7.12.17.
 */

public class NetworkChangeReceiver extends BroadcastReceiver {

  private final KolibriWebView webView;
  private boolean isThereError;

  NetworkChangeReceiver(KolibriWebView webview) {
    this.webView = webview;
    webView.addKolibriWebViewClient(new KolibriWebViewClient() {

      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        isThereError = false;
      }

      @Override
      public void onReceivedError(WebView view, WebResourceRequest request,
          WebResourceError error) {
        super.onReceivedError(view, request, error);
        isThereError = true;
      }
    });
  }

  @Override
  public void onReceive(final Context context, final Intent intent) {
    final ConnectivityManager connMgr = (ConnectivityManager) context
        .getSystemService(Context.CONNECTIVITY_SERVICE);

    final NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();

    if (activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting()) {
      webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
      if (isThereError) {
        webView.reload();
      }
    } else {
      webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
    }
  }
}
