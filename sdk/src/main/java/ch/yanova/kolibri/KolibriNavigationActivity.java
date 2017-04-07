package ch.yanova.kolibri;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class KolibriNavigationActivity extends KolibriActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        NavigationListener, KolibriInitializeListener {

    public static final String TAG_MAIN_FRAGMENT = "mainFragment";
    private NavigationView navigationView;
    private FloatingActionButton floatingActionButton;

    private View mLayoutError;
    private View mLayoutLoading;
    private View mLayoutOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onPreInitialize();

        setContentView(R.layout.navigation_drawer);

        floatingActionButton = (FloatingActionButton) findViewById(R.id.kolibri_fab);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mLayoutError = navigationView.findViewById(R.id.error);
        mLayoutLoading = navigationView.findViewById(R.id.progress);
        mLayoutOverlay = navigationView.findViewById(R.id.overlay);

        showNavigationLoading();
        setNavigationListener(this);
        loadLocalNavigation();

        final Fragment fragment = onPostInitialize();
        startMainFragment(fragment);
    }

    private void startMainFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.kolibri_main_content, fragment, TAG_MAIN_FRAGMENT).commitAllowingStateLoss();
    }

    protected Fragment getMainFragment() {
        return getSupportFragmentManager().findFragmentByTag(TAG_MAIN_FRAGMENT);
    }

    @Override
    public void onBackPressed() {
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        final Intent intent = item.getIntent();


        // FIXME: Make this activity implicit intents automatically opened
        if (intent.getDataString().startsWith("kolibri://navigation/favorites")) {
            startActivity(intent);
            final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        if (intent.getDataString().startsWith("kolibri://navigation/search")) {
            startActivity(intent);
            final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        if (intent.getDataString().startsWith("kolibri://navigation/shaker")) {
            startActivity(intent);
            final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        Kolibri.notifyComponents(getApplicationContext(), intent);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void constructNavigation(JSONObject navigation) {
        final Menu menu = navigationView.getMenu();
        menu.clear();

        try {
            final JSONArray items = navigation.getJSONArray("items");
            for (int i = 0; i < items.length(); ++i) {

                final JSONObject item = items.getJSONObject(i);

                addJsonItem(menu, item);

                if (item.has("items")) {

                    final JSONArray subItems = item.getJSONArray("items");

                    for (int j = 0; j < subItems.length(); ++j) {
                        addJsonItem(menu, subItems.getJSONObject(j));
                    }
                }
            }

            showNavigation();
        } catch (JSONException e) {
            e.printStackTrace();
            showNavigationError(e.getLocalizedMessage());
        }
    }

    @Override
    public void onLoaded(JSONObject nav) {
        try {
            JSONObject navigation = nav.getJSONObject("navigation");
            JSONObject search = nav.getJSONObject("search");
            Kolibri.updateSearchSetup(this, search.toString());
            if (navigation != null) {
                constructNavigation(navigation);
            }
        } catch (JSONException e) {
            Log.d("KolibriNavActivity", "onLoaded() returned: " + e);
        }
    }

    @Override
    public boolean onFailed(Exception e) {
        showNavigationError(e.getLocalizedMessage());
        return false;
    }

    private void addJsonItem(Menu menu, JSONObject item) throws JSONException {

        if (!item.has("label")) {
            return;
        }

        final String label = item.getString("label");
        String componentUri = item.getString("component");

        if (item.has("url")) {
            final String url = item.getString("url");
            componentUri += "?url=" + url;
        }

        final Uri uri = Uri.parse(componentUri);
        final Intent intent = Kolibri.createIntent(uri);
        intent.putExtra(Intent.EXTRA_TITLE, label);

        menu.add(label).setIntent(intent);
    }

    public FloatingActionButton getFloatingActionButton() {
        return floatingActionButton;
    }

    protected void showNavigationLoading() {

        if (mLayoutOverlay.getVisibility() != View.VISIBLE) {
            mLayoutOverlay.setVisibility(View.VISIBLE);
        }

        if (mLayoutError != null && mLayoutError.getVisibility() != View.GONE) {
            mLayoutError.setVisibility(View.GONE);
        }

        navigationView.setVisibility(View.GONE);

        mLayoutLoading.setVisibility(View.VISIBLE);
    }

    protected void showNavigationError(String text) {

        if (mLayoutOverlay.getVisibility() != View.VISIBLE) {
            mLayoutOverlay.setVisibility(View.VISIBLE);
        }

        if (mLayoutLoading.getVisibility() != View.GONE) {
            mLayoutLoading.setVisibility(View.GONE);
        }

        if (text != null) {
            ((TextView) mLayoutError).setText(text);
        }

        if (navigationView.getVisibility() != View.GONE) {
            navigationView.setVisibility(View.GONE);
        }

        mLayoutError.setVisibility(View.VISIBLE);
    }

    protected void showNavigation() {
        if (mLayoutLoading.getVisibility() != View.GONE) {
            mLayoutLoading.setVisibility(View.GONE);
        }

        if (mLayoutError.getVisibility() != View.GONE) {
            mLayoutError.setVisibility(View.GONE);
        }

        if (mLayoutOverlay.getVisibility() != View.GONE) {
            mLayoutOverlay.setVisibility(View.GONE);
        }

        navigationView.setVisibility(View.VISIBLE);
    }
}
