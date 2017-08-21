package ch.yanova.kolibri;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by lekov on 5/5/17.
 */

public class KolibriApp extends Application {

    private static final String TAG = "KolibriApp";

    private static KolibriApp instance;

    private OkHttpClient netmetrixClient;

    private static String lastUrlLogged;
    private static boolean firebaseEnabled = true;

    private static int viewportWidthPixels;
    private static int viewportHeightPixels;


    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, String.format("Using Kolibri %s version", BuildConfig.VERSION_NAME));

        final DisplayMetrics lDisplayMetrics = getResources().getDisplayMetrics();
        viewportWidthPixels = Math.round(lDisplayMetrics.widthPixels / lDisplayMetrics.density);
        viewportHeightPixels = Math.round(lDisplayMetrics.heightPixels / lDisplayMetrics.density);

        final ClearableCookieJar cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(this));

        instance = this;

        netmetrixClient = new OkHttpClient.Builder().cookieJar(cookieJar).build();
    }

    public void logEvent(@Nullable String name, @Nullable String url) {

        if (lastUrlLogged != null && lastUrlLogged.equals(url)) {
            Log.i(TAG, String.format("Trying to log again event for %s. Skipped.", url));
            return;
        }

        lastUrlLogged = url;

        Log.d(TAG, "logEvent() called with: name = [" + name + "], url = [" + url + "]");

        reportToNetmetrix(url);
    }

    void logMenuItemToFirebase(@NonNull MenuItem item) {
        if (firebaseEnabled) {

            final Intent intent = item.getIntent();

            if (intent == null) {
                Log.i(TAG, "logMenuItemToFirebase: Invalid menu item. Intent must be supplied!");
                return;
            }

            final FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);

            final Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, intent.getStringExtra(Kolibri.EXTRA_ID));

            if (intent.hasExtra(Intent.EXTRA_TITLE))
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, intent.getStringExtra(Intent.EXTRA_TITLE));

            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, intent.getStringExtra(Kolibri.EXTRA_ID));
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        }
    }

    private void reportToNetmetrix(@Nullable String url) {

        final String netmetrix = Kolibri.getInstance(this).getRuntime().getString("netmetrix");

        // Netmetrix not configured, skipping
        if (netmetrix == null || "".equals(netmetrix)) {
            return;
        }

        String variant = Kolibri.getInstance(this).getRuntime().getString("nemetrix-type");

        if (variant == null) {
            variant = "universal";
        }

        final StringBuilder sb = new StringBuilder(netmetrix);
        sb.append("/").append("android");
        sb.append("/").append(variant);
        sb.append("?d=").append(System.currentTimeMillis());
        sb.append("&x=").append(viewportWidthPixels).append("x").append(viewportHeightPixels);

        if (url != null) {
            sb.append("&r=").append(url);
        }

        // TODO: check error if request is successful but the server return some error

        netmetrixClient.newCall(
                new Request.Builder()
                        .url(sb.toString())
                        .get()
                        .header("Accept-Language", "de")
                        .header("User-Agent", "Mozilla/5.0 (Linux; U; Android-"+ variant +")")
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Report to Netmetrix failed: ", e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            Log.i(TAG, "Successfully reported to Netmetrix");
                        } else {
                            Log.e(TAG, "Report to Netmetrix failed ");
                        }
                    }
                });

        Log.d(TAG, "reportToNetmetrix() called with: url = [" + sb.toString() + "]");
    }

    public void reportToFirebase(@Nullable String name, @NonNull String url) {
        if (firebaseEnabled) {

            final FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);

            final Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, url);

            if (name != null) {
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, name);
            } else {
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "application/amp+html");
            }

            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        }
    }

    @UiThread
    public static String getUserAgent(@NonNull final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return WebSettings.getDefaultUserAgent(context);
        } else {
            try {
                final Class<?> webSettingsClassicClass = Class.forName("android.webkit.WebSettingsClassic");
                final Constructor<?> constructor = webSettingsClassicClass.getDeclaredConstructor(Context.class, Class.forName("android.webkit.WebViewClassic"));
                constructor.setAccessible(true);
                final Method method = webSettingsClassicClass.getMethod("getUserAgentString");
                return (String) method.invoke(constructor.newInstance(context, null));
            } catch (final Exception e) {
                return new WebView(context).getSettings()
                        .getUserAgentString();
            }
        }
    }

    @AnyThread
    public static boolean isFirebaseEnabled() {
        return firebaseEnabled;
    }

    @AnyThread
    public static void setFirebaseEnabled(boolean firebaseEnabled) {
        KolibriApp.firebaseEnabled = firebaseEnabled;
    }

    public static KolibriApp getInstance() {
        return instance;
    }
}
