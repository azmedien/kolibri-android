package ch.yanova.kolibri.prototype;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.crashlytics.android.Crashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriActivity;
import ch.yanova.kolibri.KolibriNavigationActivity;
import ch.yanova.kolibri.NavigationListener;
import ch.yanova.kolibri.components.KolibriWebView;
import io.fabric.sdk.android.Fabric;


public class MainActivity extends KolibriNavigationActivity {

    private KolibriWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
    }

    @Override
    public View getMainContentView() {

        webView = (webView == null) ? new KolibriWebView(this) : webView;

        return webView;
    }
}
