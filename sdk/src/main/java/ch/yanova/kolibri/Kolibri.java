package ch.yanova.kolibri;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by mmironov on 2/26/17.
 */

public class Kolibri {

    public static class Runtime {

        private final JSONObject fRuntime;

        Runtime(JSONObject runtime) {
            fRuntime = runtime;
        }

        public String getVersion() {
            try {
                return fRuntime.getString("kolibri-version");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        public JSONObject getStyling() {
            try {
                return fRuntime.getJSONObject("styling");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        public JSONObject getNavigation() {
            try {
                return fRuntime.getJSONObject("navigation");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        public JSONObject getComponent(String name) {
            try {
                return fRuntime.getJSONObject(name);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private static final String PREFS_NAME = "KolibriPrefs";
    private static final String KEY_SEARCH_JSON = "searchJson";

    private static final String META_NAVIGATION = "kolibri_navigation_url";
    private static final String PREF_NAME = "ch.yanova.kolibri.RUNTIME_CONFIG";
    private static final String TAG = "Kolibri";

    private static Kolibri mInstance;
    private static Context fContext;

    private Runtime runtime;

    private Kolibri(Context context) {
        // There's no memory leak when we get the application context.
        fContext = context.getApplicationContext();
    }

    public static synchronized Kolibri getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Kolibri(context);
        }
        return mInstance;
    }

    synchronized void loadRuntimeConfiguration(final RuntimeListener runtimeListener) {

        final String url = getNavigationUrl();

        if (url == null) {
            throw new IllegalAccessError("Kolibri navigation url must be set as meta-data in the Manifest.");
        }

        final OkHttpClient client = new OkHttpClient();

        final Request request = new Request.Builder()
                .url(getNavigationUrl())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (runtimeListener != null) {
                    final boolean userDefined = runtimeListener.onFailed(e);
                    if (!userDefined) {
                        Log.d(TAG, "onFailure() called with: call = [" + call + "], e = [" + e + "]");
                    }
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String json = response.body().string();

                try {
                    final JSONObject navigationJson = new JSONObject(json);

                    runtime = new Runtime(navigationJson);

                    if (runtimeListener != null) {
                        runtimeListener.onLoaded(runtime);
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "onResponse: ", e);

                    if (runtimeListener != null) {
                        runtimeListener.onFailed(e);
                    }
                }
            }
        });
    }

    public static void bind(View view, KolibriCoordinator coordinator) {
        view.addOnAttachStateChangeListener(new Binding(view, coordinator));
    }

    public static void bind(View view, KolibriProvider provider) {
        final KolibriCoordinator coordinator = provider.provideCoordinator(view);
        if (coordinator == null) {
            return;
        }

        View.OnAttachStateChangeListener binding = new Binding(view, coordinator);
        view.addOnAttachStateChangeListener(binding);
        // Sometimes we missed the first attach because the child's already been added.
        // Sometimes we didn't. The binding keeps track to avoid double attachment of the Coordinator,
        // and to guard against attachment to two different views simultaneously.
        binding.onViewAttachedToWindow(view);
    }

    @Nullable
    public static KolibriCoordinator getCoordinator(View view) {
        return (KolibriCoordinator) view.getTag(R.id.coordinator);
    }


    public static Intent createIntent(Uri uri) {
        final Intent res = new Intent(Intent.ACTION_VIEW);
        res.setData(uri);

        return res;
    }

    public static void notifyComponents(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static boolean updateSearchSetup(Context context, String searchJson) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(KEY_SEARCH_JSON, searchJson);
        return prefsEditor.commit();
    }

    public static String getSearchJson(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SEARCH_JSON, null);
    }

    public static String searchParamKey(Context context) {
        try {
            final JSONObject searchJson = new JSONObject(getSearchJson(context));
            return searchJson.getJSONObject("settings").getString("search-param");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getNavigationUrl() {
        try {
            final ApplicationInfo ai = fContext.getPackageManager().getApplicationInfo(fContext.getPackageName(), PackageManager.GET_META_DATA);
            final Bundle bundle = ai.metaData;

            return bundle.getString(META_NAVIGATION);
        } catch (Exception ignored) {
        }

        return null;
    }

    public synchronized Runtime getRuntime() {
        return runtime;
    }
}
