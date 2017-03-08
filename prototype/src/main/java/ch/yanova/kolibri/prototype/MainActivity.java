package ch.yanova.kolibri.prototype;

import android.os.Bundle;
import android.view.View;

import com.crashlytics.android.Crashlytics;

import ch.yanova.kolibri.KolibriNavigationActivity;
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
