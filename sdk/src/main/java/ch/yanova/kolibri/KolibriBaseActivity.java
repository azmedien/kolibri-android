package ch.yanova.kolibri;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.coordinators.WebViewCoordinator;


public abstract class KolibriBaseActivity extends KolibriNavigationActivity implements View.OnClickListener {

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
        return WebViewFragment.newInstance("");
    }

    @Override
    public void onBindComponents() {

        Kolibri.bind(getWebView(), new KolibriProvider() {
            @Nullable
            @Override
            public KolibriCoordinator provideCoordinator(View view) {
                return new WebViewCoordinator(getMainWebViewFragment());
            }
        });
    }

    public KolibriWebView getWebView() {
        final WebViewFragment webViewFragment = (WebViewFragment) getMainFragment();
        return webViewFragment.getWebView();
    }

    public WebViewFragment getMainWebViewFragment() {
        return (WebViewFragment) getMainFragment();
    }

    @Override
    public void onBackPressed() {
        if (getWebView().canGoBack()) {
            getWebView().goBack();
        } else {
            super.onBackPressed();
        }
    }
}
