package ch.yanova.kolibri;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by lekov on 5/7/17.
 */

public class RuntimeConfig {

  public static final String ID = "id";
  public static final String LABEL = "label";
  public static final String COMPONENT = "component";
  public static final String ITEMS = "items";
  public static final String TYPE = "type";
  public static final String NAVIGATION = "navigation";
  public static final String KOLIBRI_VERSION = "kolibri-version";
  public static final String DOMAIN = "domain";
  public static final String SCHEME = "scheme";
  public static final String STYLING = "styling";
  public static final String PROXY = "proxy";
  public static final String COLOR_PALETTE = "color-palette";
  public static final String ICON = "icon";

  public static final int THEME_COLOR_PRIMARY = 5;
  public static final int THEME_COLOR_PRIMARY_LIGHT = 1;
  public static final int THEME_COLOR_PRIMARY_DARK = 7;
  public static final int THEME_COLOR_ACCENT = 11;

  private final JSONObject runtime;
  private final String formattedAssetUrl;

  private Styling styling;
  private String version;
  private String scheme;
  private String domain;
  private String proxy;

  private Map<String, Component> components;
  private Navigation navigation;

  RuntimeConfig(@NonNull JSONObject json, String url) {
    runtime = json;

    final String applicationId = Uri.parse(url).getPathSegments().get(1);
    final boolean isAssetsExternal = json.has("amazon");

    formattedAssetUrl = isAssetsExternal
        ? json.optString("amazon") + "/apps/" + applicationId + "/assets/%s.png"
        : url.replace("runtime", "assets") + "/%s/download";

    if (json.length() == 0) {
      throw new KolibriException("Runtime config JSON is empty. Cannot construct configuration.");
    }

    parseStructure();

    if (navigation == null || domain == null || scheme == null || styling == null) {
      throw new KolibriException("Runtime config JSON is not valid one.");
    }
  }

  @NonNull
  private static String getAssetUrl(@NonNull String formattedAssetUrl, @NonNull String asset) {
    return asset.startsWith("http") ? asset : String.format(formattedAssetUrl, asset);
  }

  public static int[] getMaterialPalette(String color) {
    int[] result = new int[14];

    result[0] = shadeColor(color, 0.9); //----> 50
    result[1] = shadeColor(color, 0.7); //----> 100
    result[2] = shadeColor(color, 0.5); //----> 200
    result[3] = shadeColor(color, 0.333); //----> 300
    result[4] = shadeColor(color, 0.166); //----> 400
    result[5] = shadeColor(color, 0); //----> 500
    result[6] = shadeColor(color, -0.125); //----> 600
    result[7] = shadeColor(color, -0.25); //----> 700
    result[8] = shadeColor(color, -0.375); //----> 800
    result[9] = shadeColor(color, -0.5); //----> 900

    result[10] = shadeColor(color, 0.7); //----> A100
    result[11] = shadeColor(color, 0.5); //----> A200
    result[12] = shadeColor(color, 0.166); //----> A400
    result[13] = shadeColor(color, -0.25); //----> A700

    return result;
  }

  private static int shadeColor(String color, double percent) {
    long f = Long.parseLong(color.substring(1), 16);
    double t = percent < 0 ? 0 : 255;
    double p = percent < 0 ? percent * -1 : percent;
    long R = f >> 16;
    long G = f >> 8 & 0x00FF;
    long B = f & 0x0000FF;
    int red = (int) (Math.round((t - R) * p) + R);
    int green = (int) (Math.round((t - G) * p) + G);
    int blue = (int) (Math.round((t - B) * p) + B);
    return Color.rgb(red, green, blue);
  }

  private void parseStructure() {
    components = new HashMap<>();
    final JSONArray names = runtime.names();

    for (int i = 0; i < names.length(); i++) {
      final String current = names.optString(i);

      if (current == null) {
        continue;
      }

      switch (current) {
        case NAVIGATION:
          navigation = new Navigation(runtime.optJSONObject(current), formattedAssetUrl);
          break;
        case KOLIBRI_VERSION:
          version = runtime.optString(current);
          break;
        case DOMAIN:
          domain = runtime.optString(current);
          break;
        case SCHEME:
          scheme = runtime.optString(current);
          break;
        case STYLING:
          styling = new Styling(runtime.optJSONObject(current));
          break;
        case PROXY:
          proxy = runtime.optString(current);
          break;
        default:
          components.put(current, new Component(runtime.optJSONObject(current)));
          break;
      }
    }
  }

  /**
   * Check if proxy is specified in the configuration
   *
   * @return Retruns if runtime configuration is configured for proxy mode
   */
  public boolean inProxyMode() {
    return proxy != null;
  }

  /**
   *
   * Get Proxy URL
   *
   * @return Returns the proxy URL if proxy is specified, null otherwise
   */
  @Nullable
  public String getProxy() {
    return proxy;
  }

  @NonNull
  String getAssetUrl(@NonNull String asset) {
    return getAssetUrl(formattedAssetUrl, asset);
  }

  /**
   * Return Kolibri Runtime configuration version
   *
   * @return Returns current version of the Kolibri configuration
   */
  @NonNull
  public String getVersion() {
    return version;
  }

  /**
   * Return current app scheme
   *
   * @return Returns components scheme for this application
   */
  @NonNull
  public String getScheme() {
    return scheme;
  }

  /**
   * Return current app domain
   * <p>
   * The domain is used internally to determine whenever Kolibri must open
   * some URLs in external browser or within the app.
   * </p>
   *
   * @return Returns domain for this application
   */
  @NonNull
  public String getDomain() {
    return domain;
  }

  /**
   * Return base styling of the app
   *
   * @return Returns current app styling from the current configuration
   */
  @NonNull
  public Styling getStyling() {
    return styling;
  }

  /**
   * Check if runtime configuration contains given component
   *
   * @param component Component to be search for
   * @return Returns true if runtime config has the component, false otherwise
   */
  public boolean hasComponent(String component) {
    return runtime.has(component);
  }

  /**
   * Get component from the runtime configuration
   *
   * @param component Name of the desired component
   * @return Returns component if any, null if component not persist in the runtime configuration
   */
  public Component getComponent(String component) {
    return components.get(component);
  }

  /**
   * Get string value from the runtime configuration
   *
   * @param name Name of the required field
   * @return Returns value for the field with given name, null otherwise
   */
  public String getString(String name) {
    return runtime.optString(name);
  }

  /**
   * Return application navigation
   *
   * @return Return navigation object from the configuration
   */
  @NonNull
  public Navigation getNavigation() {
    return navigation;
  }

  /**
   *
   */
  public static class Component extends Settings {

    static final String KEY_SETTINGS = "settings";
    static final String KEY_STYLING = STYLING;

    Component(JSONObject json) {
      super(json);
    }

    public Settings getSettings() {
      return new Settings(json.optJSONObject(KEY_SETTINGS));
    }

    public boolean hasSettings() {
      return json.has(KEY_SETTINGS);
    }

    public Styling getStyling() {
      return new Styling(json.optJSONObject(KEY_STYLING));
    }

    public boolean hasStyling() {
      return json.has(KEY_STYLING);
    }
  }

  /**
   *
   */
  public static class Settings {

    final JSONObject json;

    Settings(JSONObject json) {
      this.json = json;
    }

    public int getInt(String key) {
      try {
        return json.getInt(key);
      } catch (JSONException e) {
        throw new KolibriException(e);
      }
    }

    public String getString(String key) {
      try {
        return json.getString(key);
      } catch (JSONException e) {
        throw new KolibriException(e);
      }
    }

    public double getDouble(String key) {
      try {
        return json.getDouble(key);
      } catch (JSONException e) {
        throw new KolibriException(e);
      }
    }

    public boolean getBoolean(String key) {
      try {
        return json.getBoolean(key);
      } catch (JSONException e) {
        throw new KolibriException(e);
      }
    }

    public Uri getUri(String key) {
      return Uri.parse(getString(key));
    }

    public boolean hasSetting(String key) {
      return json.has(key);
    }

    public JSONObject getObject(String key) {
      try {
        return json.getJSONObject(key);
      } catch (JSONException e) {
        throw new KolibriException(e);
      }
    }

    public JSONArray getArray(String key) {
      try {
        return json.getJSONArray(key);
      } catch (JSONException e) {
        throw new KolibriException(e);
      }
    }
  }

  public static class Styling {

    public static final String COLOR_PRIMARY = "primary";
    public static final String COLOR_PRIMARY_DARK = "primaryDark";
    public static final String COLOR_PRIMARY_LIGHT = "primaryLight";
    public static final String COLOR_ACCENT = "accent";

    public static final String OVERRIDES_TOOLBAR_BACKGROUND = "toolbarBackground";
    public static final String OVERRIDES_NAVIGATION_HEADER_BACKGROUND = "navigationHeaderBackground";

    private final JSONObject json;

    public Styling(JSONObject json) {
      this.json = json;
    }

    public int getColor(String key) {
      try {
        return Color.parseColor(json.getString(key));
      } catch (JSONException | IllegalArgumentException e) {
        throw new KolibriException(e);
      }
    }

    public boolean hasPalette() {
      return json.has(COLOR_PALETTE);
    }

    public boolean hasPaletteColor(String color) {
      return hasPalette() && json.optJSONObject(COLOR_PALETTE).has(color);
    }

    public int getPrimary() {
      return getPaletteColor(COLOR_PRIMARY);
    }

    public int getPrimaryDark() {
      return getPaletteColor(COLOR_PRIMARY_DARK);
    }

    public int getPrimaryLight() {
      return getPaletteColor(COLOR_PRIMARY_LIGHT);
    }

    public int getAccent() {
      return getPaletteColor(COLOR_ACCENT);
    }

    public int getPaletteColor(String name) {
      try {
        return Color.parseColor(json.getJSONObject(COLOR_PALETTE).getString(name));
      } catch (JSONException | IllegalArgumentException e) {
        throw new KolibriException(e);
      }
    }
  }

  /**
   *
   */
  public static class Navigation extends Component {

    private final Map<String, NavigationItem> items;
    private final String type;
    Navigation(JSONObject json, String formattedAssetUrl) {
      super(json);

      items = new LinkedHashMap<>();

      try {
        this.type = json.getString(TYPE);

        final JSONArray jsonArray = json.getJSONArray(ITEMS);
        for (int i = 0; i < jsonArray.length(); i++) {
          final NavigationItem item = new NavigationItem(jsonArray.getJSONObject(i),
              formattedAssetUrl);

          items.put(item.getId(), item);
        }
      } catch (JSONException e) {
        throw new KolibriException(e);
      }
    }

    public Type getType() {
      return Type.valueOf(type.toUpperCase());
    }

    public NavigationItem getItem(String id) {
      try {
        if (!hasItem(id)) {
          throw new KolibriException(
              String.format("Navigation item with id = %s does not exist", id));
        }

        return items.get(id);
      } catch (Exception e) {
        throw new KolibriException(e);
      }
    }

    public Map<String, NavigationItem> getItems() {
      return Collections.unmodifiableMap(items);
    }

    public boolean hasItem(String id) {
      return items.containsKey(id);
    }

    public enum Type {
      DRAWER, TABS, HORIZONTAL, VERTICAL;
    }
  }

  public static class NavigationItem extends Settings {

    private final String id;
    private final String label;
    private final String component;
    private final String icon;
    private final String formattedAssetUrl;
    private Map<String, NavigationItem> subItems;
    private Drawable iconDrawable;

    NavigationItem(JSONObject json, String formattedAssetUrl) {
      super(json);

      subItems = new LinkedHashMap<>();
      this.formattedAssetUrl = formattedAssetUrl;

      try {
        this.id = json.getString(ID);
        this.label = json.getString(LABEL);
        this.component = json.getString(COMPONENT);
        this.icon = json.optString(ICON);

        if (json.has(ITEMS)) {
          final JSONArray jsonArray = json.getJSONArray(ITEMS);
          for (int i = 0; i < jsonArray.length(); i++) {
            final NavigationItem item = new NavigationItem(jsonArray.getJSONObject(i),
                formattedAssetUrl);

            subItems.put(item.getId(), item);
          }
        }
      } catch (JSONException e) {
        throw new KolibriException(e);
      }
    }

    public Map<String, NavigationItem> getSubItems() {
      return Collections.unmodifiableMap(subItems);
    }

    public String getId() {
      return id;
    }

    public String getLabel() {
      return label;
    }

    public String getComponent() {
      return component;
    }

    public Uri getUri() {
      StringBuilder sb = new StringBuilder(component);

      if (json.has("url")) {
        sb.append("?url=").append(json.opt("url"));
      }

      return Uri.parse(sb.toString());
    }

    public String getIcon() {
      if (icon == null || icon.isEmpty()) {
        return null;
      }

      String formattedIcon = icon;

      if (icon.startsWith("http")) {
        return getAssetUrl(formattedAssetUrl, icon);
      } else if (formattedAssetUrl.contains("amazon")) {
        formattedIcon = icon.split("-")[0];
      } else if (!icon.contains("-png")) {
        formattedIcon = icon + "-png";
      }

      return getAssetUrl(formattedAssetUrl, formattedIcon);
    }

    public boolean hasSubItems() {
      return subItems.size() > 0;
    }
  }
}
