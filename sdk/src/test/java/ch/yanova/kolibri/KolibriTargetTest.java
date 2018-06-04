package ch.yanova.kolibri;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.net.Uri;
import java.io.IOException;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

/**
 * Created by lekov on 24.03.18.
 */

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27, manifest = Config.NONE, shadows = {ShadowKolibri.class})
public class KolibriTargetTest {

  private Kolibri kolibri;
  private ShadowKolibri shadowKolibri;

  @Before
  public void setup() throws IOException, JSONException {
    kolibri = Kolibri.getInstance(RuntimeEnvironment.application);
    shadowKolibri = Shadow.extract(kolibri);
    shadowKolibri.loadCachedVersion();
  }

  @Test
  public void kolibriShouldNotBeNull() {
    assertNotNull(shadowKolibri);
  }

  @Test
  public void testUrlWithSelfParamShouldBeSelf() {
    final Uri url = Uri.parse("https://example.com?kolibri-target=_self");
    assertEquals(Kolibri.TARGET_SELF, kolibri.getTarget(url));
  }

  @Test
  public void testUrlWithInternalParamShouldBeInternal() {
    final Uri url = Uri.parse("https://example.com?kolibri-target=_internal");
    assertEquals(Kolibri.TARGET_INTERNAL, kolibri.getTarget(url));
  }

  @Test
  public void testUrlWithExternalParamShouldBeExternal() {
    final Uri url = Uri.parse("https://example.com?kolibri-target=_external");
    assertEquals(Kolibri.TARGET_EXTERNAL, kolibri.getTarget(url));
  }

//  @Test
//  public void testUrlWithoutTargetShouldBeInternalWhenInSameDomain() {
//    final Uri url = Uri.parse("https://kolibriframework.io/home");
//    assertEquals(Kolibri.TARGET_INTERNAL, kolibri.getTarget(url));
//  }

//  @Test
//  public void testUrlWithoutTargetShouldBeExternalWhenInDifferentDomain() {
//    final Uri url = Uri.parse("https://example.com");
//    assertEquals(Kolibri.TARGET_EXTERNAL, kolibri.getTarget(url));
//  }

//  @Test
//  public void testRelativeUrlsShouldBeInternal() {
//    final Uri url = Uri.parse("/inner/page");
//    assertEquals(Kolibri.TARGET_INTERNAL, kolibri.getTarget(url));
//  }

  @Test
  public void testNotHierarchicalShouldBeExternal() {
    final Uri url = Uri.parse("external:inner/page");
    assertEquals(Kolibri.TARGET_EXTERNAL, kolibri.getTarget(url));
  }

//  @Test
//  public void testThatWWWCuaseNoIssuesWithTargets() {
//    final Uri self = Uri.parse("https://www.example.com?kolibri-target=_self");
//    assertEquals(Kolibri.TARGET_SELF, kolibri.getTarget(self));
//
//    final Uri internal = Uri.parse("http://www.example.com?kolibri-target=_internal");
//    assertEquals(Kolibri.TARGET_INTERNAL, kolibri.getTarget(internal));
//
//    final Uri external = Uri.parse("www.example.com?kolibri-target=_external");
//    assertEquals(Kolibri.TARGET_EXTERNAL, kolibri.getTarget(external));
//
//    final Uri sameDomain = Uri.parse("https://www.kolibriframework.io/home");
//    assertEquals(Kolibri.TARGET_INTERNAL, kolibri.getTarget(sameDomain));
//  }
}
