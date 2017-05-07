package ch.yanova.kolibri;

import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by lekov on 5/5/17.
 */

public class KolibriApp extends Application {

    private static KolibriApp instance;

    private FirebaseAnalytics firebaseAnalytics;
    private OkHttpClient netmetrixClient;

    private String userAgent;

    private static String lastUrlLogged;

    private static int widthPixels;
    private static int heightPixels;

    private static boolean firebaseEnabled = true;

    @Override
    public void onCreate() {
        super.onCreate();

        final DisplayMetrics lDisplayMetrics = getResources().getDisplayMetrics();
        final ClearableCookieJar cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(this));

        instance = this;

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        widthPixels = lDisplayMetrics.widthPixels;
        heightPixels = lDisplayMetrics.heightPixels;

        netmetrixClient = new OkHttpClient.Builder().cookieJar(cookieJar).build();
    }

    public void logEvent(@Nullable String name, @NonNull String url) {

        if (lastUrlLogged != null && lastUrlLogged.contentEquals(url)) {
            Log.i("KolibriApp", String.format("Trying to log again event for %s. Skipped.", url));
            return;
        }

        lastUrlLogged = url;

        Log.d("KolibriApp", "logEvent() called with: name = [" + name + "], url = [" + url + "]");

        reportToFirebase(name, url);
        reportToNetmetrix(url);
    }

    private void reportToNetmetrix(@NonNull String url) {
        // Netmetrix not configured
        if (Kolibri.getInstance(this).getNetmetrixUrl() == null)
            return;

        final String sb = Kolibri.getInstance(this).getNetmetrixUrl() + "/" + "wildeisen" +
                "/" + "android" +
                "/" + FirebaseInstanceId.getInstance().getId() +
                "?r=" + url +
                "&d=" + System.currentTimeMillis() +
                "&x=" + widthPixels + "x" + heightPixels;


        // TODO:
        // 1. check error if request is successful but the server return some error
        netmetrixClient.newCall(
                new Request.Builder()
                        .url(sb)
                        .get()
                        .header("Accept-Language", "de")
                        .header("User-Agent", userAgent == null ? "Mozilla/5.0 (Linux; U; Android-phone)" : userAgent)
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

    private void reportToFirebase(@Nullable String name, @NonNull String url) {
        if (firebaseEnabled) {

            final Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, url);

            if (name != null)
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);

            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "application/amp+html");
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        }
    }

    public void setUserAgent(String mUserAgent) {
        this.userAgent = mUserAgent;
    }

    public static boolean isFirebaseEnabled() {
        return firebaseEnabled;
    }

    public static void setFirebaseEnabled(boolean firebaseEnabled) {
        KolibriApp.firebaseEnabled = firebaseEnabled;
    }

    public static KolibriApp getInstance() {
        return instance;
    }
}
