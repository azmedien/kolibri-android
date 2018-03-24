package ch.yanova.kolibri;

import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import ch.yanova.kolibri.components.KolibriWebViewClient;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

/**
 * Created by lekov on 24.03.18.
 */

@Implements(KolibriWebViewClient.class)
public class ShadowKolibriWebViewClient {

  @RealObject KolibriWebViewClient client;

  private int pageStartedCount = 0;
  private int pageFinishedCount = 0;
  private int pageCommitVisible = 0;

  @Implementation
  protected void __constructor__() {
  }

  @Implementation
  protected boolean onCustomTarget(Uri link, String target) {
    return false;
  }

  @Implementation
  public void onPageStarted(WebView view, String url, Bitmap favicon) {
    pageStartedCount++;
    client.onPageStarted(view, url, favicon);
  }

  @Implementation
  public void onPageFinished(WebView view, String url) {
    pageFinishedCount++;
    client.onPageFinished(view, url);
  }

  @Implementation
  public void onPageCommitVisible(WebView view, String url) {
    pageCommitVisible++;
    client.onPageCommitVisible(view, url);
  }

  public int getPageStartedCount() {
    return pageStartedCount;
  }

  public int getPageFinishedCount() {
    return pageFinishedCount;
  }

  public int getPageCommitVisible() {
    return pageCommitVisible;
  }
}
