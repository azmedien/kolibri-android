package ch.yanova.kolibri.prototype;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.crashlytics.android.Crashlytics;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriCoordinator;
import ch.yanova.kolibri.KolibriNavigationActivity;
import ch.yanova.kolibri.KolibriProvider;
import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.coordinators.SearchWebviewCoordinator;
import ch.yanova.kolibri.coordinators.WebViewCoordinator;
import io.fabric.sdk.android.Fabric;


public class MainActivity extends KolibriNavigationActivity implements View.OnClickListener {

    private KolibriWebView webView;

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
        final WebViewFragment webViewFragment = (WebViewFragment) getMainFragment();
        webView = webViewFragment.getWebView();

        Kolibri.bind(webView, new KolibriProvider() {
            @Nullable
            @Override
            public KolibriCoordinator provideCoordinator(View view) {
                return new SearchWebviewCoordinator(null, webViewFragment);
            }
        });
    }

    @Override
    public void onNavigationInitialize() {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                getWebViewCoordinator().onQueryByText(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    private SearchWebviewCoordinator getWebViewCoordinator() {
        return ((SearchWebviewCoordinator)webView.getTag(R.id.coordinator));
    }
}
