package ch.yanova.kolibri;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.components.KolibriWebViewClient;
import ch.yanova.kolibri.coordinators.WebViewCoordinator;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowWebView;

/**
 * Created by lekov on 24.03.18.
 */

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27, manifest = Config.NONE, shadows = {ShadowKolibri.class,
    ShadowKolibriWebViewClient.class})
public class KolibriWebViewTest {

  private KolibriWebView webview;
  private ShadowWebView shadowWebView;
  private WebViewCoordinator coordinator;
  private ShadowKolibriWebViewClient client;
  private Random random;

  @Before
  public void setup() {
    webview = new KolibriWebView(RuntimeEnvironment.application);
    client = Shadow.extract(new KolibriWebViewClient());
    coordinator = new WebViewCoordinator();

    Kolibri.bind(webview, coordinator);
    webview.addKolibriWebViewClient(client.client);
    shadowWebView = Shadows.shadowOf(webview);

    random = new Random();
  }

  @Test
  public void webviewShouldNotBeNull() {
    assertNotNull(webview);
    assertNotNull(shadowWebView);
  }

  /**w
   * Mimic loading of a Webpage. This can be moved to shadow.
   */
  private void loadPage() {
    webview.loadUrl("https://example.com");
    client.onPageStarted(webview, "https://example.com", null);

    final int chance = random.nextInt();
    if (chance % 2 == 0) {
      client.onPageFinished(webview, "https://example.com");
      client.onPageCommitVisible(webview, "https://example.com");
    } else {
      client.onPageCommitVisible(webview, "https://example.com");
      client.onPageFinished(webview, "https://example.com");
    }

    client.onPageFinished(webview, "https://example.com");
  }
}
