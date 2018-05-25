package ch.yanova.kolibri;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

/** Created by lekov on 24.03.18. */
@RunWith(RobolectricTestRunner.class)
@Config(
  sdk = 27,
  shadows = {ShadowKolibri.class}
)
public class KolibriTest {

  private Kolibri kolibri;
  private ShadowKolibri shadowKolibri;

  @Before
  public void setup() {
    kolibri = Kolibri.getInstance(RuntimeEnvironment.application);
    shadowKolibri = Shadow.extract(kolibri);
  }

  @After
  public void finishComponentTesting() {
    resetSingleton(Kolibri.class, "mInstance");
  }

  private void resetSingleton(Class clazz, String fieldName) {
    Field instance;
    try {
      instance = clazz.getDeclaredField(fieldName);
      instance.setAccessible(true);
      instance.set(null, null);
    } catch (Exception e) {
      throw new RuntimeException();
    }
  }

  @Test
  public void kolibriShouldNotBeNull() {
    assertNotNull(shadowKolibri);
  }

  @Test
  public void kolibriRuntimeShouldBeNull() {
    assertNull(kolibri.getRuntime());
  }

  @Test
  public void kolibriRuntimeShouldLoadCache() throws IOException, JSONException {
    assertNull(kolibri.getRuntime());
    shadowKolibri.loadCachedVersion();
    assertNotNull(kolibri.getRuntime());
    assertTrue(shadowKolibri.isLoadedConfigFromCache());
  }
}
