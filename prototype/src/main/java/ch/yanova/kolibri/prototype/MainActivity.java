package ch.yanova.kolibri.prototype;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.crashlytics.android.Crashlytics;

import ch.yanova.kolibri.KolibriNavigationActivity;
import ch.yanova.kolibri.components.KolibriWebView;
import io.fabric.sdk.android.Fabric;


public class MainActivity extends KolibriNavigationActivity implements View.OnClickListener {

    private KolibriWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        getFloatingActionButton().setOnClickListener(this);
    }

    @Override
    public View getMainContentView() {

        if (webView == null) {
            webView = new KolibriWebView(this);
        }

        return webView;
    }

    @Override
    public void onClick(View v) {
        Snackbar.make(v, "Favoriten bace!", Snackbar.LENGTH_LONG).show();
    }
}
