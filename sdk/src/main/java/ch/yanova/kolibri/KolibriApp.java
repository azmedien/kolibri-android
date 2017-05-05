package ch.yanova.kolibri;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;

import ch.yanova.kolibri.network.NetworkUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.CookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by lekov on 5/5/17.
 */

public class KolibriApp extends Application {

    private static KolibriApp sInstance;

    private FirebaseAnalytics mFirebaseAnalytics;
    private OkHttpClient mNetmetrixClient;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        final CookieJar cookieJar = new NetworkUtils.KolibriCookieJar();
        mNetmetrixClient = new OkHttpClient.Builder().cookieJar(cookieJar).build();
    }

    public void logEvent(String name, String url) {

        final Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, url);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "text/html");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);


        // Netmetrix not configured
        if (Kolibri.getInstance(this).getNetmetrixUrl() == null)
            return;

        final String sb = Kolibri.getInstance(this).getNetmetrixUrl() + "/" + "wildeisen" +
                "/" + "android" +
                "/" + FirebaseInstanceId.getInstance().getId() +
                "?d=" + System.currentTimeMillis();

        // TODO:
        // 1. change user agent to be the kolibri one
        // 2. check error if request is successful but the server return some error
        mNetmetrixClient.newCall(
                new Request.Builder()
                        .url(sb)
                        .get()
                        .header("Accept-Language", "de")
                        .header("User-Agent", "Mozilla/5.0 (Linux; U; Android-phone)")
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e("KolibriApp", "Report to Netmetrix failed: ", e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            Log.i("KolibriApp", "Successfully reported to Netmetrix");
                        } else {
                            Log.e("KolibriApp", "Report to Netmetrix failed ");
                        }
                    }
                });
    }

    public static KolibriApp getInstance() {
        return sInstance;
    }
}
