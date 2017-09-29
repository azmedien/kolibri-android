package ch.yanova.kolibri.prototype;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.Map;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriBaseActivity;
import ch.yanova.kolibri.KolibriCoordinator;
import ch.yanova.kolibri.KolibriProvider;
import ch.yanova.kolibri.WebViewFragment;
import ch.yanova.kolibri.coordinators.SearchWebviewCoordinator;

public class MainActivity extends KolibriBaseActivity implements View.OnClickListener {

    @Override
    public void onBindComponents() {
        Kolibri.bind(getWebView(), new KolibriProvider() {
            @Nullable
            @Override
            public KolibriCoordinator provideCoordinator(@NonNull View view) {
                return new SearchWebviewCoordinator() {
                    @Override
                    public void onFound(Map<String, String> data) {
                        super.onFound(data);
                        getMainWebViewFragment().onFound(data);
                    }
                };
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        WebViewFragment fragment = getMainWebViewFragment();
        fragment.setShowSearchOption(false);

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
