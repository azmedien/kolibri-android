package ch.yanova.kolibri;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.AnyThread;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.NavigationViewMode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static ch.yanova.kolibri.RuntimeConfig.THEME_COLOR_PRIMARY;
import static ch.yanova.kolibri.RuntimeConfig.getMaterialPalette;

/**
 * Created by mmironov on 2/26/17.
 */

public class Kolibri {

    public static final String EXTRA_ID = "id";
    public static final String EXTRA_QUERY = "query";
    public static final String EXTRA_GO_BACK_URL = "go_back_url";
    public static final String EXTRA_ERROR_MESSAGE = "error_message";

    private static final String KEY_SUBSCRIBED_FOR_PUSH = "subscribedForPush";

    public static final String PARAM_TARGET = "kolibri-target";
    public static final String TARGET_INTERNAL = "_internal";
    public static final String TARGET_EXTERNAL = "_external";
    public static final String TARGET_SELF = "_self";

    public static boolean isPageSearchable(Context context, String pageId) {

        if (pageId == null) {
            return false;
        }

        try {
            final RuntimeConfig.Component search = Kolibri.getInstance(context).getRuntime().getComponent("search");

            if (search.hasSetting("navigation")) {
                JSONArray items = search.getObject("navigation").getJSONArray("items");

                for (int i = 0; i < items.length(); ++i) {
                    if (pageId.equals(items.getJSONObject(i).getString("id"))) {
                        return true;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static final String PREFS_NAME = "KolibriPrefs";
    private static final String KEY_SEARCH_JSON = "searchJson";

    private static final String META_NAVIGATION = "kolibri_navigation_url";
    private static final String META_NOTIFICATION_ICON = "kolibri_notification_icon";

    private static final String PREF_NAME = "ch.yanova.kolibri.RUNTIME_CONFIG";
    private static final String TAG = "Kolibri";

    private static Kolibri mInstance;
    private static Context fContext;

    private SharedPreferences preferences;
    private RuntimeConfig runtime;

    private Kolibri(Context context) {
        // There's no memory leak when we get the application context.
        fContext = context.getApplicationContext();
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized Kolibri getInstance(@NonNull Context context) {
        if (mInstance == null) {
            mInstance = new Kolibri(context);
        }
        return mInstance;
    }

    synchronized void loadRuntimeConfiguration(final RuntimeListener runtimeListener) {

        final String url = getRuntimeUrl();

        if (url == null) {
            throw new IllegalAccessError("Kolibri navigation url must be set as meta-data in the Manifest.");
        }

        int cacheSize = 10 * 1024 * 1024; // 10 MiB
        Cache cache = new Cache(fContext.getCacheDir(), cacheSize);

        final OkHttpClient client = new OkHttpClient.Builder().cache(cache).build();

        final Request request = new Request.Builder()
                .url(getRuntimeUrl())
                .header("Cache-Control", "public, max-age=604800")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (runtimeListener != null) {
                    final boolean userDefined = runtimeListener.onFailed(e);
                    if (!userDefined) {
                        try { // Try to load saved one as a fallback configuratio
                            runtime = new RuntimeConfig(new JSONObject(preferences.getString("runtime", "{}")), getRuntimeUrl());
                            runtimeListener.onLoaded(runtime, false);
                        } catch (JSONException | KolibriException exception) {
                            runtimeListener.onFailed(exception);
                        }
                    }
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String json = response.body().string();
                final boolean isFresh = response.cacheResponse() == null;
                Exception exception = null;

                try {

                    Log.i(TAG, "onResponse: cache " + response.cacheResponse());
                    Log.i(TAG, "onResponse: network " + response.networkResponse());

                    JSONObject navigationJson = new JSONObject(json);
                    runtime = new RuntimeConfig(navigationJson, getRuntimeUrl());
                    preferences.edit().putString("runtime", json).apply();
                } catch (JSONException e) {

                    try { // Try to load saved one as a fallback configuratio
                        runtime = new RuntimeConfig(new JSONObject(preferences.getString("runtime", "{}")), getRuntimeUrl());
                    } catch (JSONException | KolibriException ignored) {
                        exception = ignored;
                    }
                } catch (KolibriException kolibri) {
                    exception = kolibri;
                } finally {
                    if (runtimeListener != null) {
                        if (exception == null) {
                            runtimeListener.onLoaded(runtime, isFresh);
                        } else {
                            runtimeListener.onFailed(exception);
                        }
                    }
                }
            }
        });
    }

    @UiThread
    public static void bind(View view, KolibriCoordinator coordinator) {
        view.addOnAttachStateChangeListener(new Binding(view, coordinator));
    }

    @UiThread
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


    @AnyThread
    public static Intent createIntent(@NonNull Uri uri) {
        final Intent res = new Intent(Intent.ACTION_VIEW);
        res.setData(uri);
        return res;
    }

    @AnyThread
    public static HandlerType notifyComponents(@NonNull Context context, @NonNull Intent intent) {
        boolean handled = LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        if (handled) {
            return HandlerType.COMPONENT;
        }

        final PackageManager packageManager = context.getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            context.startActivity(intent);
            return HandlerType.ACTIVITY;
        }

        return HandlerType.NONE;
    }

    public void applyRuntimeTheme() {
        final RuntimeConfig.Styling styling = runtime.getStyling();
        int primaryColor = styling.getPrimary();
        int accentColor = styling.getAccent();

        if (styling.hasPaletteColor(RuntimeConfig.Styling.OVERRIDES_TOOLBAR_BACKGROUND)) {
            final int toolbarBackgroud = styling.getPaletteColor(RuntimeConfig.Styling.OVERRIDES_TOOLBAR_BACKGROUND);
            final int[] palette = getMaterialPalette(String.format("#%06X", 0xFFFFFF & toolbarBackgroud));

            primaryColor = palette[THEME_COLOR_PRIMARY];
            accentColor = palette[13];
        }

        Aesthetic.get()
                .colorPrimary(primaryColor)
                .colorAccent(accentColor)
                .colorStatusBarAuto()
                .textColorPrimary(Color.BLACK)
                .navigationViewMode(NavigationViewMode.SELECTED_ACCENT)
                .apply();
    }

    public static String searchParamKey(Context context) {
        return Kolibri.getInstance(context).getRuntime().getComponent("search").getSettings().getString("search-param");
    }

    String getRuntimeUrl() {
        try {
            final ApplicationInfo ai = fContext.getPackageManager().getApplicationInfo(fContext.getPackageName(), PackageManager.GET_META_DATA);
            final Bundle bundle = ai.metaData;

            return bundle.getString(META_NAVIGATION);
        } catch (Exception ignored) {
        }

        return null;
    }

    @DrawableRes
    public int getNotificationIcon() {
        try {
            final ApplicationInfo ai = fContext.getPackageManager().getApplicationInfo(fContext.getPackageName(), PackageManager.GET_META_DATA);
            final Bundle bundle = ai.metaData;

            return bundle.getInt(META_NOTIFICATION_ICON, R.drawable.ic_train_grey600_24dp);
        } catch (Exception ignored) {
        }

        return R.drawable.ic_train_grey600_24dp;
    }

    @AnyThread
    public synchronized RuntimeConfig getRuntime() {
        return runtime;
    }

    public static Intent getErrorIntent(Context context, String title, String errorMessage) {

        final Intent errorIntent = new Intent(context, ErrorActivity.class);
        errorIntent.putExtra(Intent.EXTRA_TITLE, title);
        errorIntent.putExtra(EXTRA_ERROR_MESSAGE, errorMessage);
        return errorIntent;
    }

    public void subscribeForPushNotifications() {
        preferences.edit().putBoolean(KEY_SUBSCRIBED_FOR_PUSH, true).apply();
    }

    public void unsubscribeFromPushNotifications() {

        if (preferences.contains(KEY_SUBSCRIBED_FOR_PUSH)) {
            preferences.edit().remove(KEY_SUBSCRIBED_FOR_PUSH).apply();
        }
    }

    public boolean isSubscribedForPushNotifications() {
        return preferences.getBoolean(KEY_SUBSCRIBED_FOR_PUSH, false);
    }

    public boolean hasUnsubscribedFromPushExplicitly() {
        return preferences.contains(KEY_SUBSCRIBED_FOR_PUSH);
    }

    public enum HandlerType {
        COMPONENT, ACTIVITY, NONE
    }

    @NonNull
    public String getTarget(Uri link) {
        String target = link.getQueryParameter(PARAM_TARGET);

        if (target == null) {
            String domain = getRuntime().getDomain();
            String host = link.getHost();

            if (domain.startsWith("www.")) {
                domain = domain.substring(4);
            }

            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            if (host.equals(domain)) {
                target = TARGET_INTERNAL;
            } else {
                target = TARGET_EXTERNAL;
            }
        }
        return target;
    }
}
