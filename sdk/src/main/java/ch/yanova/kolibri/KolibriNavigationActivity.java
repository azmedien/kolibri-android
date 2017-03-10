package ch.yanova.kolibri;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.yanova.kolibri.components.KolibriComponent;
import ch.yanova.kolibri.components.KolibriFloatingActionButton;

public abstract class KolibriNavigationActivity extends KolibriActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        NavigationListener {

    private NavigationView navigationView;
    private FrameLayout mainContentView;
    private KolibriFloatingActionButton floatingActionButton;

    private View mLayoutError;
    private View mLayoutLoading;
    private View mLayoutOverlay;

    private AlphaAnimation mAminFadeIn;
    private AlphaAnimation mAminFadeOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.navigation_drawer);

        mainContentView = (FrameLayout) findViewById(R.id.kolibri_main_content);
        mainContentView.addView(getMainContentView());

        mLayoutError = findViewById(R.id.error);
        mLayoutLoading = findViewById(R.id.progress);
        mLayoutOverlay = findViewById(R.id.overlay);

        floatingActionButton = (KolibriFloatingActionButton) findViewById(R.id.kolibri_fab);
        Kolibri.bind(floatingActionButton, KolibriFloatingActionButton.URI_SHOW);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        showNavigationLoading();

        setNavigationListener(this);
        loadLocalNavigation();
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

        final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
        final Intent intent = item.getIntent();
        intent.putExtra("handle", true);
        manager.sendBroadcast(intent);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onLoaded(JSONObject nav) {
        final Menu menu = navigationView.getMenu();
        menu.clear();

        try {
            final JSONArray items = nav.getJSONObject("navigation").getJSONArray("items");
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

        Kolibri.bind((KolibriComponent) getMainContentView(), componentUri);

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        final Uri uri = Uri.parse(componentUri);
        intent.setData(uri);

        menu.add(label).setIntent(intent);
    }

    public abstract View getMainContentView();

    public FloatingActionButton getFloatingActionButton() {
        return floatingActionButton;
    }

    protected void showNavigationLoading() {

        if (mLayoutOverlay.getVisibility() != View.VISIBLE) {
            mLayoutOverlay.setVisibility(View.VISIBLE);
            mLayoutOverlay.startAnimation(getAminFadeIn());
        }

        if (mLayoutError != null && mLayoutError.getVisibility() != View.GONE) {
            mLayoutError.setVisibility(View.GONE);
            mLayoutError.startAnimation(getAminFadeOut());
        }

        navigationView.setVisibility(View.GONE);
        navigationView.startAnimation(getAminFadeOut());

        mLayoutLoading.setVisibility(View.VISIBLE);
    }

    protected void showNavigationError(String text) {

        if (mLayoutOverlay.getVisibility() != View.VISIBLE) {
            mLayoutOverlay.setVisibility(View.VISIBLE);
            mLayoutOverlay.startAnimation(getAminFadeIn());
        }

        if (mLayoutLoading.getVisibility() != View.GONE) {
            mLayoutLoading.setVisibility(View.GONE);
            mLayoutLoading.startAnimation(getAminFadeOut());
        }

        if (text != null) {
            ((TextView) mLayoutError).setText(text);
        }

        if (navigationView.getVisibility() != View.GONE) {
            navigationView.setVisibility(View.GONE);
            navigationView.startAnimation(getAminFadeOut());
        }

        mLayoutError.setVisibility(View.VISIBLE);
        mLayoutError.startAnimation(getAminFadeIn());
    }

    protected void showNavigation() {
        if (mLayoutLoading.getVisibility() != View.GONE) {
            mLayoutLoading.setVisibility(View.GONE);
            mLayoutLoading.startAnimation(getAminFadeOut());
        }

        if (mLayoutError.getVisibility() != View.GONE) {
            mLayoutError.setVisibility(View.GONE);
            mLayoutError.startAnimation(getAminFadeOut());
        }

        if (mLayoutOverlay.getVisibility() != View.GONE) {
            mLayoutOverlay.setVisibility(View.GONE);
            mLayoutOverlay.startAnimation(getAminFadeOut());
        }

        navigationView.setVisibility(View.VISIBLE);
        navigationView.startAnimation(getAminFadeIn());
    }

    protected void showFloatingButton() {
        floatingActionButton.show();
    }

    protected void hideFloatingButton() {
        floatingActionButton.show();
    }

    protected void toggleFloatingButton() {
        if (floatingActionButton.isShown()) {
            floatingActionButton.hide();
        } else {
            floatingActionButton.show();
        }
    }

    protected boolean isNavigationVisible() {
        return navigationView.getVisibility() == View.VISIBLE;
    }

    protected boolean isFabVisible() {
        return floatingActionButton.getVisibility() == View.VISIBLE;
    }

    public Animation getAminFadeIn() {
        if (mAminFadeIn == null) {
            mAminFadeIn = new AlphaAnimation(0f, 1f);
            mAminFadeIn.setDuration(200);
        }
        return mAminFadeIn;
    }

    public Animation getAminFadeOut() {
        if (mAminFadeOut == null) {
            mAminFadeOut = new AlphaAnimation(1f, 0f);
            mAminFadeOut.setDuration(200);
        }
        return mAminFadeOut;
    }
}
