package ch.yanova.kolibri.prototype;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.View;

import com.crashlytics.android.Crashlytics;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriCoordinator;
import ch.yanova.kolibri.KolibriNavigationActivity;
import ch.yanova.kolibri.KolibriProvider;
import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.coordinators.WebViewCoordinator;
import io.fabric.sdk.android.Fabric;


public class MainActivity extends KolibriNavigationActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onPreInitialize() {
    }

    @Override
    public Fragment onPostInitialize() {
        return WebViewFragment.newInstance();
    }

    @Override
    public void onBindComponents() {
        final WebViewFragment webViewFragment = (WebViewFragment) getMainFragment();
        final KolibriWebView webView = webViewFragment.getWebview();

        Kolibri.bind(webView, new KolibriProvider() {
            @Nullable
            @Override
            public KolibriCoordinator provideCoordinator(View view) {
                return new WebViewCoordinator(null);
            }
        });
    }

    @Override
    public void onNavigationInitialize() {
    }
}
