package ch.yanova.kolibri;

import java.io.InputStream;
import java.util.Scanner;
import org.json.JSONException;
import org.json.JSONObject;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Created by lekov on 24.03.18.
 */

@Implements(Kolibri.class)
public class ShadowKolibri {

  public static final String APP_URL = "https://kolibri.herokuapp.com/apps/7yaaRbHQx2NvYTori9EWqazJ";

  @Implementation
  public static ShadowKolibri getInstance() {
    return new ShadowKolibri();
  }

  @Implementation
  protected void __constructor__() {
  }

  @Implementation
  public synchronized RuntimeConfig getRuntime() {
    final InputStream is = this.getClass().getClassLoader().getResourceAsStream("config.json");
    try {
      return new RuntimeConfig(streamToJson(is), String.format("%s/runtime", APP_URL));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return null;
  }

  private JSONObject streamToJson(InputStream is) throws JSONException {
    Scanner s = new Scanner(is).useDelimiter("\\A");
    String result = s.hasNext() ? s.next() : "";
    return new JSONObject(result);
  }
}
