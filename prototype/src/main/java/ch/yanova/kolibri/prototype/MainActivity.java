package ch.yanova.kolibri.prototype;

import android.os.Bundle;
import android.view.View;

import com.crashlytics.android.Crashlytics;

import ch.yanova.kolibri.ActionButtonListener;
import ch.yanova.kolibri.KolibriNavigationActivity;
import ch.yanova.kolibri.components.KolibriWebView;
import io.fabric.sdk.android.Fabric;


public class MainActivity extends KolibriNavigationActivity implements ActionButtonListener {

    private KolibriWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
    }

    @Override
    public View getMainContentView() {

        webView = (webView == null) ? new KolibriWebView(this) : webView;
        webView.setActionButtonListener(this);

        return webView;
    }

    @Override
    public void showActionButton() {
        showFloatingButton();
    }

    @Override
    public void hideActionButton() {
        hideFloatingButton();
    }

    @Override
    public void onActionButtonClick(View.OnClickListener listener) {
        getFloatingActionButton().setOnClickListener(listener);
    }
}
