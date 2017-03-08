package ch.yanova.kolibri.components;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import ch.yanova.kolibri.R;

public class WebViewActivity extends AppCompatActivity {

    private KolibriWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        webView = (KolibriWebView) findViewById(R.id.activity_web_view);
        webView.setWebViewClient(new KolibriWebViewClient() {
            @Override
            protected boolean shouldHandleInternal() {
                return false;
            }
        });

        Intent linkIntent = getIntent();

        if (linkIntent != null) {
            Uri url = linkIntent.getData();
            webView.loadUrl(url.toString());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
