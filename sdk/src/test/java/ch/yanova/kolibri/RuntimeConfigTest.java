package ch.yanova.kolibri;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Scanner;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by lekov on 8.11.17.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RuntimeConfigTest {

  public static final String APP_URL = "https://kolibri.herokuapp.com/apps/7yaaRbHQx2NvYTori9EWqazJ";
  private RuntimeConfig config;

  @Before
  public void setUp() throws Exception {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("config.json");
    config = new RuntimeConfig(streamToJson(is), String.format("%s/runtime", APP_URL));
  }

  private JSONObject streamToJson(InputStream is) throws JSONException {
    Scanner s = new Scanner(is).useDelimiter("\\A");
    String result = s.hasNext() ? s.next() : "";
    return new JSONObject(result);
  }

  @Test(expected = KolibriException.class)
  public void testCannotCreateEmptyConfig() throws JSONException {
    new RuntimeConfig(new JSONObject("{}"),
        "https://kolibri.herokuapp.com/apps/7yaaRbHQx2NvYTori9EWqazJ");
  }

  @Test(expected = KolibriException.class)
  public void testCannotCreateWithoutNavigation() throws JSONException {
    final String invalid =
        "{\n"
            + "  \"kolibri-version\": 1.0,\n"
            + "  \"domain\": \"kolibriframework.io\",\n"
            + "  \"scheme\": \"kolibri\",\n"
            + "  \"amazon\": \"https://yanova-kolibri-assets-dev.s3.amazonaws.com\",\n"
            + "  \"netmetrix-type\": \"universal\",\n"
            + "  \"netmetrix\": \"https://we-ssl.wemfbox.ch/cgi-bin/ivw/CP/apps/wireltern\",\n"
            + "  \"adtech\": {\n"
            + "    \"settings\": {\n"
            + "      \"network_id\": 1605,\n"
            + "      \"subnetwork_id\": 1,\n"
            + "      \"alias\": \"we_int\",\n"
            + "      \"domain\": \"a.adtech.de\",\n"
            + "      \"max_display_time\": 6,\n"
            + "      \"app_name\": \"wireltern\"\n"
            + "    }\n"
            + "  },\n"
            + "  \"styling\": {\n"
            + "    \"color-palette\": {\n"
            + "      \"primary\": \"#E94F35\",\n"
            + "      \"primaryDark\": \"#E94F35\",\n"
            + "      \"primaryLight\": \"#de0000\",\n"
            + "      \"accent\": \"#E94F35\",\n"
            + "      \"toolbarBackground\": \"#F6F6F6\"\n"
            + "    }"
            + "}"
            + "}";
    new RuntimeConfig(new JSONObject(invalid),
        "https://kolibri.herokuapp.com/apps/7yaaRbHQx2NvYTori9EWqazJ");
  }

  @Test
  public void testHasStyling() throws Exception {
    assertNotNull(config.getStyling());
  }

  @Test
  public void testHomeAssetUrl() throws Exception {
    assertNotNull(config.getAssetUrl("home"));
    assertEquals(
        "https://yanova-kolibri-assets-dev.s3.amazonaws.com/apps/7yaaRbHQx2NvYTori9EWqazJ/assets/home.png",
        config.getAssetUrl("home"));
  }

  @Test
  public void testGetVersion() throws Exception {
    assertEquals("1.0", config.getVersion());
  }

  @Test
  public void testGetScheme() throws Exception {
    assertEquals("kolibri", config.getScheme());
  }

  @Test
  public void testGetDomain() throws Exception {
    assertEquals("kolibriframework.io", config.getDomain());
  }

  @Test
  public void testHasMandotoryNavigationComponent() throws Exception {
    assertTrue(config.hasComponent("navigation"));
  }

  @Test
  public void testNoShakeappComponent() throws Exception {
    assertFalse(config.hasComponent("shakeapp"));
  }

  @Test
  public void testGetSearchComponent() throws Exception {
    assertNotNull(config.getComponent("search"));
  }

  @Test
  public void testGetStringValue() throws Exception {
    assertNotNull(config.getString("scheme"));
    assertEquals("kolibri", config.getString("scheme"));
  }

  @Test
  public void testGeneratePalette() throws Exception {
    assertNotNull(RuntimeConfig.getMaterialPalette("#000000"));
  }

  @Test
  public void testGetNavigation() throws Exception {
    assertNotNull(config.getNavigation());
  }

  @Test
  public void testNavigationHasSettings() throws Exception {
    assertTrue(config.getNavigation().hasSettings());
  }

  @Test
  public void testNavigationHasDefaultItem() throws Exception {
    assertTrue(config.getNavigation().getSettings().hasSetting("default-item"));
  }

  @Test
  public void testNavigationDefaultItemHasRightValue() throws Exception {
    assertThat(config.getNavigation().getSettings().getInt("default-item"), is(0));
  }

}