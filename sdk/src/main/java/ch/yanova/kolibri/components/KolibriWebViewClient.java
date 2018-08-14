package ch.yanova.kolibri.components;

import android.net.Uri;
import android.webkit.WebViewClient;

import java.util.Collections;
import java.util.Map;

/**
 * Created by lekov on 9/2/17.
 */

public class KolibriWebViewClient extends WebViewClient {

  protected boolean onCustomTarget(Uri link, String target) {
    return false;
  }

  /**
   *
   * Supply the webview client with extra parameters
   *
   * <ul>
   *     <li>`username` - username for authentication</li>
   *     <li>`password` - password for authentication</li>
   * </ul>
   *
   * @return Map containing the supplied parameters
   */
  protected Map<String, String> onRequestExtraParams() { return Collections.emptyMap(); }
}
