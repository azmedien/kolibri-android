package ch.yanova.kolibri;

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
            String json = new String(buffer, "UTF-8");

            JSONObject navJson = new JSONObject(json);

            if (navigationListener != null) {
                navigationListener.onLoaded(navJson);
            }

        } catch (IOException | JSONException ex) {
            Log.e(TAG, "loadLocalNavigation: ", ex);
        }
    }

    private void loadNavigation() {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(NAVIGATION_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (navigationListener != null) {
                    boolean userDefined = navigationListener.onFailed();
                    if (!userDefined) {
                        Log.d(TAG, "onFailure() called with: call = [" + call + "], e = [" + e + "]");
                    }
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String json = response.body().string();
                try {
                    JSONObject navigationJson = new JSONObject(json);
                    Log.w(TAG, "onResponse: " + navigationJson.toString());
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse: ", e);
                }
            }
        });
    }
}
