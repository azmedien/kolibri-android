package ch.yanova.kolibri.prototype;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriActivity;
import ch.yanova.kolibri.NavigationListener;
import ch.yanova.kolibri.components.KolibriWebView;


public class MainActivity extends KolibriActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        NavigationListener {

    private NavigationView navigationView;
    private KolibriWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        webView = (KolibriWebView) findViewById(R.id.webView);
    }

    @Override
    protected void onStart() {
        super.onStart();

        setNavigationListener(this);
        loadLocalNavigation();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
        manager.sendBroadcast(item.getIntent());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onLoaded(JSONObject nav) {
        Menu menu = navigationView.getMenu();

        try {
            final JSONArray items = nav.getJSONObject("navigation").getJSONArray("items");
            for (int i = 0; i < items.length(); ++i) {

                JSONObject item = items.getJSONObject(i);

                addJsonItem(menu, item);

                if (item.has("items")) {

                    final JSONArray subItems = item.getJSONArray("items");

                    for (int j = 0; j < subItems.length(); ++j) {
                        addJsonItem(menu, subItems.getJSONObject(j));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onFailed() {
        return false;
    }

    private void addJsonItem(Menu menu, JSONObject item) throws JSONException {

        if (!item.has("label")) {
            return;
        }

        String label = item.getString("label");
        String componentUri = item.getString("component");
        Kolibri.bind(webView, componentUri);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(componentUri);
        intent.setData(uri);

        menu.add(label).setIntent(intent);
    }
}
