package ch.yanova.kolibri;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.view.MenuItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by lekov on 5/7/17.
 */

public class RuntimeConfig {
    private final JSONObject runtime;
    private String version;
    private String scheme;
    private String domain;
    private Map<String, Component> components;
    private Navigation navigation;

    RuntimeConfig(@NonNull JSONObject json) {
        runtime = json;

        if (json.length() == 0) {
            throw new KolibriException("Runtime config JSON is empty. Cannot construct the menu.");
        }

        components = new HashMap<>();
        final JSONArray names = runtime.names();
        for (int i = 0; i < names.length(); i++) {
            final String current = names.optString(i);

            if (current == null)
                continue;

            switch (current) {
                case "navigation":
                    navigation = new Navigation(runtime.optJSONObject(current));
                    break;
                case "kolibri-version":
                    version = runtime.optString(current);
                    break;
                case "domain":
                    domain = runtime.optString(current);
                    break;
                case "scheme":
                    scheme = runtime.optString(current);
                    break;
                default:
                    components.put(current, new Component(runtime.optJSONObject(current)));
                    break;

            }
        }

        if (navigation == null || domain == null || scheme == null) {
            throw new KolibriException("Runtime config JSON is not valid one.");
        }
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
     *     The domain is used internally to determine whenever Kolibri must open
     *     some URLs in external browser or within the app.
     * </p>
     *
     * @return Returns domain for this application
     */
    @NonNull
    public String getDomain() {
        return domain;
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

    public Component getComponent(String component) {
        return components.get(component);
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

        Component(JSONObject json) {
            super(json);
        }

        public Settings getSettings() {
            return new Settings(json.optJSONObject(KEY_SETTINGS));
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

    /**
     *
     */
    public static class Navigation extends Component {

        public enum Type {
            DRAWER, TABS, HORIZONTAL, VERTICAL;
        }

        private final Map<String, NavigationItem> items;
        private final String type;

        Navigation(JSONObject json) {
            super(json);

            items = new LinkedHashMap<>();

            try {
                this.type = json.getString("type");

                final JSONArray jsonArray = json.getJSONArray("items");
                for (int i = 0; i < jsonArray.length(); i++) {
                    final NavigationItem item = new NavigationItem(jsonArray.getJSONObject(i));

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
                    throw new KolibriException(String.format("Navigation item with id = %s does not exist", id));
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
    }

    public static class NavigationItem extends Settings {

        private final String id;
        private final String label;
        private final String component;
        private Map<String, NavigationItem> subItems;

        NavigationItem(JSONObject json) {
            super(json);

            subItems = new LinkedHashMap<>();

            try {
                this.id = json.getString("id");
                this.label = json.getString("label");
                this.component = json.getString("component");

                if (json.has("items")) {
                    final JSONArray jsonArray = json.getJSONArray("items");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        final NavigationItem item = new NavigationItem(jsonArray.getJSONObject(i));

                        subItems.put(item.getId(), item);
                    }
                }
            } catch (JSONException e) {
                throw new KolibriException(e);
            }
        }


        public boolean hasSubItems() {
            return subItems.size() > 0;
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
    }

}
