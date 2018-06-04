package ch.yanova.kolibri;

import static ch.yanova.kolibri.Constants.APP_URL;
import static ch.yanova.kolibri.Constants.TEST_CONFIG_RES;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import org.json.JSONException;
import org.json.JSONObject;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

/** Created by lekov on 24.03.18. */
@Implements(Kolibri.class)
public class ShadowKolibri {

  private boolean cacheDisabled;
  private boolean isLoadedConfigFromCache;

  @Implementation
  public static Kolibri getInstance() {
    return ReflectionHelpers.newInstance(Kolibri.class);
  }

  @Implementation
  public RuntimeConfig getRuntimeConfigFromCache() {

    if (cacheDisabled) {
      return null;
    }

    RuntimeConfig config = null;
    SharedPreferences preferences =
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application);
    try {
      config =
          new RuntimeConfig(
              new JSONObject(preferences.getString("runtime", "{}")),
              String.format("%s/runtime", APP_URL));
      isLoadedConfigFromCache = true;
    } catch (Exception ignored) {
      ignored.printStackTrace();
    }

    return config;
  }

  @Implementation
  String getRuntimeUrl() {
    return String.format("%s/runtime", APP_URL);
  }

  private JSONObject streamToJson(InputStream is) throws JSONException {
    Scanner s = new Scanner(is).useDelimiter("\\A");
    String result = s.hasNext() ? s.next() : "";
    return new JSONObject(result);
  }

  public void loadCachedVersion() throws JSONException, IOException {
    final InputStream is = this.getClass().getClassLoader().getResourceAsStream(TEST_CONFIG_RES);
    SharedPreferences preferences =
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application);
    preferences.edit().putString("runtime", streamToJson(is).toString()).apply();
    is.close();
  }

  public boolean isLoadedConfigFromCache() {
    return isLoadedConfigFromCache;
  }
}
