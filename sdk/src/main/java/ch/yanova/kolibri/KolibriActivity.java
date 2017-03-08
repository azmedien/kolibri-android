package ch.yanova.kolibri;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class KolibriActivity extends AppCompatActivity {

    private static final String NAVIGATION_URL = "";
    private static final String TAG = "KolibriActivity";

    private static final String META_NAVIGATION = "kolibri_navigation_url";

    private NavigationListener navigationListener;

    public void setNavigationListener(NavigationListener listener) {
        navigationListener = listener;
    }

    public void loadLocalNavigation() {

        try {
            InputStream is = getAssets().open("menu.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            final String json = new String(buffer, "UTF-8");
            final JSONObject navJson = new JSONObject(json);

            if (navigationListener != null) {
                navigationListener.onLoaded(navJson);
            }

        } catch (IOException | JSONException ex) {
            Log.e(TAG, "loadLocalNavigation: ", ex);

            if (navigationListener != null) {
                navigationListener.onFailed(ex);
            }
        }
    }

    private String getNavigationUrl() {
        try {
            final ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            final Bundle bundle = ai.metaData;

            return bundle.getString(META_NAVIGATION);
        } catch (Exception ignored) {
        }

        return null;
    }

    private void loadNavigation() {

        final OkHttpClient client = new OkHttpClient();

        final Request request = new Request.Builder()
                .url(NAVIGATION_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (navigationListener != null) {
                    final boolean userDefined = navigationListener.onFailed(e);
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

                    Log.w(TAG, "onResponse: " + navigationJson.toString());

                    if (navigationListener != null) {
                        navigationListener.onLoaded(navigationJson);
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "onResponse: ", e);

                    if (navigationListener != null) {
                        navigationListener.onFailed(e);
                    }
                }
            }
        });
    }
}
