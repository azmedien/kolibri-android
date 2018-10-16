package ch.yanova.kolibri;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.ProgressBar;
import ch.yanova.kolibri.components.KolibriLoadingView;
import ch.yanova.kolibri.components.KolibriWebChromeClient;
import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.components.KolibriWebViewClient;
import ch.yanova.kolibri.coordinators.WebViewCoordinator;
import ch.yanova.kolibri.network.NetworkUtils;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.AestheticActivity;
import com.afollestad.aesthetic.NavigationViewMode;
import java.util.Locale;
import java.util.Map;

/**
 * Created by lekov on 26.10.17.
 */

public class InternalActivity extends AestheticActivity implements RuntimeListener {

    private FloatingActionButton floatingActionButton;

    private KolibriWebView webView;
    private KolibriLoadingView webviewOverlay;

    private boolean restarted;
    private boolean pageHasError;

    private Intent shareIntent;
    private ProgressBar progress;
    private RuntimeConfig configuration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_internal);

        floatingActionButton = findViewById(R.id.kolibri_fab);
        webView = findViewById(R.id.webview);
        webviewOverlay = findViewById(R.id.overlay);
        progress = findViewById(R.id.progress);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        restarted = false;

        webView.addKolibriWebChromeClient(
                new KolibriWebChromeClient() {
                    @Override
                    public void onProgressChanged(WebView view, int newProgress) {
                        super.onProgressChanged(view, newProgress);
                        if (newProgress < 100) {
                            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                                progress.setProgress(newProgress, true);
                            } else {
                                progress.setProgress(newProgress);
                            }
                        } else {
                            progress.setProgress(100);
                            progress.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    progress.setVisibility(View.GONE);
                                }
                            }, 500);

                        }
                    }
                });


        webView.addKolibriWebViewClient(new KolibriWebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                shareIntent = null;
                pageHasError = false;

                invalidateOptionsMenu();

                if (!pageHasError) {
                    progress.setProgress(0);
                    progress.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                if (!pageHasError) {
                    webviewOverlay.showView();
                }
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                super.onReceivedError(view, request, error);
                showPageError(error.getErrorCode(), error.getDescription());
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description,
                                        String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                showPageError(errorCode, description);
            }

            private void showPageError(int errorCode, CharSequence description) {
                pageHasError = true;

                Kolibri.getInstance(InternalActivity.this).applyRuntimeTheme(false);

                if (errorCode == ERROR_CONNECT || errorCode == ERROR_HOST_LOOKUP) {
                    webviewOverlay.showError(getResources().getString(R.string.internet_error_message));
                } else {
                    webviewOverlay.showError(
                            String.format(Locale.getDefault(), "Error %d: %s", errorCode, description));
                }

                progress.setVisibility(View.GONE);
                progress.setProgress(0);
            }
        });

        Kolibri.getInstance(this).applyRuntimeTheme(false);

        Intent linkIntent = getIntent();

        if (linkIntent != null && linkIntent.getData() != null) {

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            if (linkIntent.hasExtra(Intent.EXTRA_TITLE)) {
                getSupportActionBar().setTitle(linkIntent.getStringExtra(Intent.EXTRA_TITLE));
            }

            if (NetworkUtils.isConnectedToInternet(this)) {
                final Uri url = linkIntent.getData();
                webView.loadUrl(url.getQueryParameter("url"));
            } else {
                webviewOverlay.showError();
            }
        } else {
            Kolibri.getInstance(this).loadRuntimeConfiguration(this);
        }
    }

    @Override
    protected void onPause() {
        webView.onPause();
        webView.pauseTimers();
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restarted = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Kolibri kolibri = Kolibri.getInstance(this);
        kolibri.loadRuntimeConfiguration(this);

        webView.resumeTimers();
        webView.onResume();

        if (restarted) {
            // In case we are returning from other activity which is with runtime theme
            // We want to be sure that if the there was theme-meta, we apply it again
            final Integer lastPrimary = (Integer) webView.getTag(R.id.primaryColor);
            final Integer lastAccent = (Integer) webView.getTag(R.id.accentColor);

            if (lastPrimary != null) {
                Aesthetic.get()
                        .colorPrimary(lastPrimary)
                        .colorAccent(lastAccent)
                        .colorStatusBarAuto()
                        .colorNavigationBarAuto()
                        .textColorPrimary(Color.BLACK)
                        .navigationViewMode(NavigationViewMode.SELECTED_ACCENT)
                        .apply();
            } else {
                Kolibri.getInstance(this).applyRuntimeTheme(true);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_share) {
            startActivity(Intent.createChooser(shareIntent, "Share link!"));
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

    public KolibriWebView getWebView() {
        return webView;
    }

    protected void handleAmpData(Map<String, String> data) {
        if (data.size() > 0 && data.containsKey(WebViewCoordinator.META_SHAREABLE)) {

            final String url = data.containsKey(WebViewCoordinator.META_CANONICAL)
                    ? data.get(WebViewCoordinator.META_CANONICAL)
                    : data.get(WebViewCoordinator.META_URL);

            if (url != null) {
                shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, data.get(WebViewCoordinator.META_TITLE));
                shareIntent.putExtra(Intent.EXTRA_TEXT, url);
            } else {
                shareIntent = null;
            }

            invalidateOptionsMenu();
        }
    }

    protected FloatingActionButton getFloatingActionButton() {
        return floatingActionButton;
    }

    protected KolibriLoadingView getWebviewOverlay() {
        return webviewOverlay;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem share = menu.findItem(R.id.action_share);

        share.setVisible(shareIntent != null);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_webview, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onLoaded(final RuntimeConfig runtime, final boolean isFresh) {
        this.configuration = runtime;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (Boolean.FALSE.toString().equals(runtime.getString("native-navigation").toLowerCase().trim())) {
                    getSupportActionBar().hide();
                }

                // We may skip setup styling if we are coming from background or loading same configuration
                if (!restarted || isFresh) {
                    loadDefaultItem();
                    Kolibri.getInstance(getApplicationContext()).applyRuntimeTheme(true);
                }

                onNavigationInitialize();
            }
        });
    }

    public void onNavigationInitialize() {

        if (getIntent() == null || !getIntent().hasCategory("notification")) {
            return;
        }

        String notificationUrl = getIntent().getStringExtra("url");
        final Uri notificationUri = Uri.parse(notificationUrl);

        if (notificationUri.getScheme().startsWith("http")) {
            final Uri uri = Uri.parse(WebViewCoordinator.webViewUri);

            notificationUrl = UriUtils.appendUtmParameters(notificationUrl);

            final Uri.Builder builder = uri.buildUpon();
            builder.appendQueryParameter("url", notificationUrl);

            final Intent intent = Kolibri.createIntent(builder.build());
            Kolibri.notifyComponents(this, intent);
            setIntent(null);
            return;
        }

        //If the host is not navigation
        final Intent intent = Kolibri.createIntent(notificationUri);
        Kolibri.HandlerType type = Kolibri.notifyComponents(this, intent);

        if (type == Kolibri.HandlerType.NONE) {
            final String title = getIntent().hasExtra(Kolibri.EXTRA_TITLE) ? getIntent().getStringExtra(
                    Kolibri.EXTRA_TITLE) : getIntent().getStringExtra(Intent.EXTRA_TITLE);
            final Intent errorIntent = Kolibri
                    .getErrorIntent(this, title, getString(R.string.text_update_app));
            Kolibri.notifyComponents(InternalActivity.this, errorIntent);
            setIntent(null);
        }
    }

    protected RuntimeConfig.NavigationItem getDefaultItem() {
        final int defaultItemIndex = configuration == null ?
                0 : configuration.getNavigation().getSettings().getInt("default-item");

        return (RuntimeConfig.NavigationItem) configuration.getNavigation().getItems().values().toArray()[defaultItemIndex];
    }

    protected void loadDefaultItem() {

        final String defaultHome = configuration.getString("default-url");
        if (defaultHome.isEmpty()) {
            RuntimeConfig.NavigationItem defaultItem = getDefaultItem();
            Kolibri.notifyComponents(this, Kolibri.createIntent(defaultItem.getUri()));
        } else {
            Kolibri.notifyComponents(this, Kolibri.createIntent(Uri.parse(String.format("kolibri://content/link?url=%s", defaultHome))));
        }
    }

    @Override
    public boolean onFailed(Exception e) {
        return false;
    }
}
