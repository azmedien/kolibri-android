package ch.yanova.kolibri.prototype;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriCoordinator;
import ch.yanova.kolibri.KolibriNavigationActivity;
import ch.yanova.kolibri.KolibriProvider;
import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.components.KolibriWebViewClient;
import ch.yanova.kolibri.coordinators.ActionButtonCoordinator;
import ch.yanova.kolibri.coordinators.SearchWebviewCoordinator;

public class MainActivity extends KolibriNavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWebView().addKolibriWebViewClient(new KolibriWebViewClient() {
            @Override
            protected boolean onCustomTarget(Uri link, String target) {

                if ("360player".equals(target)) {
                    startActivity(CardboardActivity.createIntent(MainActivity.this, link));
                    return true;
                }

                return super.onCustomTarget(link, target);
            }
        });

        Kolibri.bind(getWebView(), new KolibriProvider() {
            @Nullable
            @Override
            public KolibriCoordinator provideCoordinator(@NonNull View view) {
                return new SearchWebviewCoordinator() {
                    @Override
                    protected void handleIntent(KolibriWebView view, Intent intent) {
                        super.handleIntent(view, intent);
                    }
                };
            }
        });

        Kolibri.bind(getFloatingActionButton(), new KolibriProvider() {
            @Nullable
            @Override
            public KolibriCoordinator provideCoordinator(@NonNull View view) {
                return new ActionButtonCoordinator();
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem searchItem = menu.findItem(R.id.action_search_show);
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
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private SearchWebviewCoordinator getWebViewCoordinator() {
        return ((SearchWebviewCoordinator) getWebView().getTag(R.id.coordinator));
    }
}
