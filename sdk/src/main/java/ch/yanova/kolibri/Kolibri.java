package ch.yanova.kolibri;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mmironov on 2/26/17.
 */

public final class Kolibri {

    private static final String PREFS_NAME = "KolibriPrefs";
    private static final String KEY_SEARCH_JSON = "searchJson";

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
        Gson gson = new Gson();
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
            return searchJson.getJSONObject("search").getJSONObject("settings").getString("search-param");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
}
