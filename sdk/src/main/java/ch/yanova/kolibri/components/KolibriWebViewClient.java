package ch.yanova.kolibri.components;

import android.net.Uri;
import android.webkit.WebViewClient;

/**
 * Created by lekov on 9/2/17.
 */

public class KolibriWebViewClient extends WebViewClient {

  protected boolean onCustomTarget(Uri link, String target) {
    return false;
  }
}
