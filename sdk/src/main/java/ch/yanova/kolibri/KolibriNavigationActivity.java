package ch.yanova.kolibri;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.yanova.kolibri.components.KolibriLoadingView;
import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.components.KolibriWebViewClient;
import ch.yanova.kolibri.coordinators.WebViewCoordinator;

import static ch.yanova.kolibri.RuntimeConfig.COMPONENT;
import static ch.yanova.kolibri.RuntimeConfig.ICON;
import static ch.yanova.kolibri.RuntimeConfig.ID;
import static ch.yanova.kolibri.RuntimeConfig.ITEMS;
import static ch.yanova.kolibri.RuntimeConfig.LABEL;
import static ch.yanova.kolibri.RuntimeConfig.Navigation;
import static ch.yanova.kolibri.RuntimeConfig.NavigationItem;
import static ch.yanova.kolibri.RuntimeConfig.Styling;
import static ch.yanova.kolibri.RuntimeConfig.THEME_COLOR_PRIMARY;
import static ch.yanova.kolibri.RuntimeConfig.THEME_COLOR_PRIMARY_DARK;
import static ch.yanova.kolibri.RuntimeConfig.getMaterialPalette;

public abstract class KolibriNavigationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, RuntimeListener {

    // this set prevents collecting targets by garbage collector
    final Set<Target> targets = new HashSet<>();

    protected RuntimeConfig configuration;
    private NavigationView navigationView;
    private FloatingActionButton floatingActionButton;
    private KolibriWebView webView;
    private KolibriLoadingView webviewOverlay;
    private KolibriLoadingView menuOverlay;

    private boolean restarted;
    private DrawerLayout drawer;
    private final View.OnClickListener onFooterClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Intent intent = (Intent) v.getTag();

            notifyComponenets(intent);

            drawer.closeDrawer(GravityCompat.START);
        }
    };

    private Kolibri.HandlerType notifyComponenets(Intent intent) {
        final Kolibri.HandlerType type = Kolibri.notifyComponents(KolibriNavigationActivity.this, intent);

        if (type == Kolibri.HandlerType.COMPONENT) {

            final Uri uri = intent.getData();
            final String target = Kolibri.getInstance(this).getTarget(uri);
            if (Kolibri.TARGET_SELF.equals(target)) {
                final String title = intent.getStringExtra(Intent.EXTRA_TITLE);
                setActionBarTitle(title);
            }
        }

        if (type == Kolibri.HandlerType.NONE) {
            Kolibri.notifyComponents(KolibriNavigationActivity.this, Kolibri.getErrorIntent(KolibriNavigationActivity.this, getString(R.string.text_update_app)));
        }

        return type;
    }

    private Toolbar toolbar;
    private View headerImageContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.navigation_drawer);

        floatingActionButton = findViewById(R.id.kolibri_fab);
        webView = findViewById(R.id.webview);
        webviewOverlay = findViewById(R.id.overlay);
        menuOverlay = findViewById(R.id.menuOverlay);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        headerImageContainer = navigationView.getHeaderView(0).findViewById(R.id.header_image_container);

        restarted = false;

        webView.addKolibriWebViewClient(new KolibriWebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                getWebviewOverlay().showLoading();
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                getWebviewOverlay().showView();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    // On old devices without commitVisible we delay preventing flickering.
                    getWebviewOverlay().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getWebviewOverlay().showView();
                        }
                    }, 250);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                getWebviewOverlay().showError();
            }
        });
    }

    public void applyDefaultPalette() {
        final RuntimeConfig.Styling styling = configuration.getStyling();
        final int primary = styling.getPrimary();
        final int[] materialPalette = getMaterialPalette(String.format("#%06X", 0xFFFFFF & primary));

        applyColorPalette(materialPalette);
    }

    public void applyColorPalette(int[] palette) {
        TintUtils.tintToolbar(this, toolbar, palette[THEME_COLOR_PRIMARY], palette[THEME_COLOR_PRIMARY_DARK], false);
        headerImageContainer.setBackgroundColor(palette[THEME_COLOR_PRIMARY]);

        final int tintColor = TintUtils.isDarkColor(palette[THEME_COLOR_PRIMARY]) ? palette[THEME_COLOR_PRIMARY] : palette[THEME_COLOR_PRIMARY_DARK];
        TintUtils.tintNavigationView(navigationView, tintColor);
    }

    private void setupStyling() {
        if (configuration == null) {
            return;
        }

        final Styling styling = configuration.getStyling();

        if (styling.hasPaletteColor(Styling.OVERRIDES_TOOLBAR_BACKGROUND)) {
            final int toolbarBackgroud = styling.getPaletteColor(Styling.OVERRIDES_TOOLBAR_BACKGROUND);
            final int[] palette = getMaterialPalette(String.format("#%06X", 0xFFFFFF & toolbarBackgroud));

            TintUtils.tintToolbar(this, toolbar, palette[THEME_COLOR_PRIMARY], palette[THEME_COLOR_PRIMARY_DARK], false);
            headerImageContainer.setBackgroundColor(palette[THEME_COLOR_PRIMARY]);
        } else {
            TintUtils.tintToolbar(this, toolbar, styling.getPrimary(), styling.getPrimaryDark(), false);
            headerImageContainer.setBackgroundColor(styling.getPrimary());
        }

        if (styling.hasPaletteColor(Styling.OVERRIDES_TOOLBAR_TEXT)) {
            toolbar.setTitleTextColor(styling.getPaletteColor(Styling.OVERRIDES_TOOLBAR_TEXT));
            toolbar.setSubtitle(styling.getPaletteColor(Styling.OVERRIDES_TOOLBAR_TEXT));
        }


        if (styling.hasPaletteColor(Styling.OVERRIDES_NAVIGATION_ITEM_SELECTED)) {
            final int menuItemSelected = styling.getPaletteColor(Styling.OVERRIDES_NAVIGATION_ITEM_SELECTED);
            TintUtils.tintNavigationView(navigationView, menuItemSelected);
        } else {
            TintUtils.tintNavigationView(navigationView, styling.getPrimary());
        }

        if (styling.hasPaletteColor(Styling.OVERRIDES_NAVIGATION_HEADER_BACKGROUND)) {
            headerImageContainer.setBackgroundColor(styling.getPaletteColor(Styling.OVERRIDES_NAVIGATION_HEADER_BACKGROUND));
        } else {
            headerImageContainer.setBackgroundColor(styling.getPrimary());
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restarted = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        menuOverlay.showLoading();

        Kolibri kolibri = Kolibri.getInstance(this);
        kolibri.loadRuntimeConfiguration(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        KolibriApp.getInstance().logMenuItemToFirebase(item);

        final Intent intent = item.getIntent();

        Kolibri.HandlerType type = notifyComponenets(intent);

        if (type.equals(Kolibri.HandlerType.COMPONENT)) {
            unselectAllMenuItemsExcept(item);
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected void setActionBarTitle(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && title != null) {
            actionBar.setTitle(title);
        }
    }

    private void constructNavigation(Navigation navigation) {
        final Menu menu = navigationView.getMenu();

        final MenuItem selectedMenuItem = getSelectedMenuItem();
        final String selectedMenuItemId = selectedMenuItem == null ?
                null : selectedMenuItem.getIntent().getStringExtra(ID);

        menu.clear();

        final Map<String, RuntimeConfig.NavigationItem> items = navigation.getItems();
        for (String id : items.keySet()) {
            final NavigationItem item = items.get(id);
            addNavigationItem(menu, item);


            if (!item.hasSubItems()) {
                continue;
            }

            final Map<String, RuntimeConfig.NavigationItem> subItems = item.getSubItems();

            for (String subId : subItems.keySet()) {
                addNavigationItem(menu, subItems.get(subId));
            }
        }

        menu.getItem(navigation.getSettings().getInt("default-item")).getIntent().addCategory(Intent.CATEGORY_DEFAULT);

        menuOverlay.showView();

        if (!restarted) {
            loadDefaultItem();
        } else if (selectedMenuItemId != null) {
            final MenuItem itemByid = findMenuItem(selectedMenuItemId);
            unselectAllMenuItemsExcept(itemByid);
        }

        onNavigationInitialize();
    }

    protected void loadDefaultItem() {

        final MenuItem defaultItem = getDefaultItem();
        defaultItem.setChecked(true);
        Kolibri.notifyComponents(this, defaultItem.getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    public void onNavigationInitialize() {

        if (getIntent() == null || !getIntent().hasCategory("notification")) {
            return;
        }

        final String notificationUrl = getIntent().getStringExtra("url");
        final Uri notificationUri = Uri.parse(notificationUrl);

        if (notificationUri.getScheme().startsWith("http")) {
            Uri uri = Uri.parse(WebViewCoordinator.webViewUri);

            Uri.Builder builder = uri.buildUpon();
            builder.appendQueryParameter("url", notificationUrl);

            Intent intent = Kolibri.createIntent(builder.build());
            Kolibri.notifyComponents(this, intent);
            setIntent(null);
            return;
        }

        if ("navigation".equals(notificationUri.getAuthority())) {

            final List<String> segments = notificationUri.getPathSegments();

            if (segments != null && !segments.isEmpty()) {
                final String id = segments.get(0);

                final Menu menu = navigationView.getMenu();

                for (int i = 0; i < menu.size(); i++) {

                    final MenuItem item = menu.getItem(i);
                    final String idInMenu = item.getIntent().getStringExtra(ID);

                    if (id.equals(idInMenu)) {

                        final String urlToLoad = notificationUri.getQueryParameter("url");
                        final Uri menuData = item.getIntent().getData();

                        Intent intent = Kolibri.createIntent(menuData);

                        //There is a specific url that was pushed to the app
                        if (urlToLoad != null) {
                            Uri intentData = Uri.parse(WebViewCoordinator.webViewUri);
                            Uri.Builder builder = intentData.buildUpon();
                            builder.appendQueryParameter("url", urlToLoad);
                            intentData = builder.build();
                            intent.setData(intentData);
                            intent.putExtra(Kolibri.EXTRA_GO_BACK_URL, menuData.getQueryParameter("url"));
                        }

                        unselectAllMenuItemsExcept(item);
                        intent.putExtra(Intent.EXTRA_TITLE, item.getTitle());

                        setIntent(null);
                        Kolibri.notifyComponents(this, intent);
                        return;
                    }
                }
            }

            //If the was no id
            final Menu menu = navigationView.getMenu();
            final Intent intent = menu.getItem(0).getIntent();
            Kolibri.notifyComponents(this, intent);
            setIntent(null);
            return;
        }

        //If the host is not navigation
        final Intent intent = Kolibri.createIntent(notificationUri);
        Kolibri.HandlerType type = Kolibri.notifyComponents(this, intent);

        if (type == Kolibri.HandlerType.NONE) {
            setIntent(null);
            Kolibri.notifyComponents(this, Kolibri.getErrorIntent(this, getString(R.string.text_update_app)));
        }
    }

    public void unselectAllMenuItemsExcept(MenuItem item) {
        final Menu menu = navigationView.getMenu();

        for (int m = 0; m < menu.size(); m++) {
            if (!menu.getItem(m).equals(item)) {
                menu.getItem(m).setChecked(false);
            }
        }

        item.setChecked(true);
    }

    @Override
    public void onLoaded(@NonNull final RuntimeConfig runtime) {

        this.configuration = runtime;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Navigation navigation = runtime.getNavigation();

                setupStyling();
                setupHeader();

                constructNavigation(navigation);
                if (navigation.hasSetting("footer")) {
                    try {
                        constructFooter(navigation.getObject("footer"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void setupHeader() {
        final JSONObject header = configuration.getNavigation().getObject("header");
        final View headerView = navigationView.getHeaderView(0);

        if (header == null) {
            return;
        }

        if (header.has("background")) {
            try {
                final Target target = new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        final FrameLayout layout = headerView.findViewById(R.id.header_image_container);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            layout.setBackground(new BitmapDrawable(getResources(), bitmap));
                        } else {
                            layout.setBackgroundDrawable(new BitmapDrawable(bitmap));
                        }
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                };
                targets.add(target);
                final String background = configuration.getAssetUrl(header.getString("background"));
                Picasso.with(this).load(background).into(target);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (header.has("image")) {
            try {
                final String image = configuration.getAssetUrl(header.getString("image"));
                Picasso.with(this).load(image).into((ImageView) headerView.findViewById(R.id.header_image));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void constructFooter(JSONObject footer) throws JSONException {

        if (!footer.has(ITEMS) || isFooterConstructed()) {
            return;
        }

        final JSONArray items = footer.getJSONArray(ITEMS);
        final LinearLayout footerView = navigationView.findViewById(R.id.kolibri_footer);
        final LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < items.length(); i++) {
            final TextView tv = (TextView) inflater.inflate(R.layout.footer_item, footerView, false);

            JSONObject item = items.getJSONObject(i);
            final String label = item.getString(LABEL);
            tv.setText(label);

            String componentUri = item.getString(COMPONENT);
            if (item.has("url")) {
                final String url = item.getString("url");
                componentUri += "?url=" + url;
            }

            final Uri uri = Uri.parse(componentUri);
            final Intent intent = Kolibri.createIntent(uri);
            intent.putExtra(Intent.EXTRA_TITLE, label);

            final String id = item.getString(ID);
            intent.putExtra(Kolibri.EXTRA_ID, id);

            tv.setTag(intent);
            tv.setOnClickListener(onFooterClick);

            footerView.addView(tv);
        }

    }

    private boolean isFooterConstructed() {
        return ((LinearLayout) navigationView.findViewById(R.id.kolibri_footer)).getChildCount() > 0;
    }

    @Override
    public boolean onFailed(final Exception e) {
        navigationView.post(new Runnable() {
            @Override
            public void run() {
                menuOverlay.showError();
            }
        });
        return false;
    }

    private void addNavigationItem(Menu menu, NavigationItem item) {
        final String label = item.getLabel();
        Uri componentUri = item.getUri();

        final Intent intent = Kolibri.createIntent(componentUri);
        intent.putExtra(Intent.EXTRA_TITLE, label);

        final String id = item.getId();
        intent.putExtra(Kolibri.EXTRA_ID, id);

        MenuItem menuItem = menu.add(label).setIntent(intent);

        if (item.hasSetting(ICON)) {
            String iconUrl = item.getIcon();
            loadMenuIcon(menuItem, iconUrl);
        }
    }

    private void loadMenuIcon(final MenuItem menuItem, String url) {

        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                BitmapDrawable mBitmapDrawable = new BitmapDrawable(getResources(), bitmap);
                menuItem.setIcon(mBitmapDrawable);
            }

            @Override
            public void onBitmapFailed(Drawable drawable) {
            }

            @Override
            public void onPrepareLoad(Drawable drawable) {
            }
        };

        targets.add(target);

        Picasso.with(this).load(url).into(target);

    }

    @NonNull
    public FloatingActionButton getFloatingActionButton() {
        return floatingActionButton;
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    public Menu getMenu() {
        return navigationView.getMenu();
    }

    public MenuItem getSelectedMenuItem() {

        for (int i = 0; i < getMenu().size(); ++i) {

            if (getMenu().getItem(i).isChecked()) {
                return getMenu().getItem(i);
            }
        }

        return null;
    }

    public MenuItem findMenuItem(String id) {

        for (int i = 0; i < getMenu().size(); ++i) {

            if (getMenu().getItem(i).getIntent().getStringExtra(ID).equals(id)) {
                return getMenu().getItem(i);
            }
        }

        return null;
    }

    protected MenuItem getDefaultItem() {
        final int defaultItemIndex = configuration.getNavigation().getSettings().getInt("default-item");

        return getMenu().getItem(defaultItemIndex);
    }

    @Override
    public void onBackPressed() {


        // FIXME
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }

        final MenuItem selectedItem = getSelectedMenuItem();
        final MenuItem defaultItem = getDefaultItem();

        if (webView.canGoBack()) {

            final String selectedItemUrl = selectedItem.getIntent()
                    .getData().getQueryParameter("url");
            final String currentUrl = webView.getOriginalUrl();

            //If the url we are going back from
            //came from a menu item click, we clear the history
            //and go back home
            if (currentUrl.equals(selectedItemUrl)) {
                final String url = defaultItem.getIntent().getData().getQueryParameter("url");

                //If there is url on default item, then we load this url and clear that history
                //so that the next time the user presses back they are redirected out
                //of the app
                if (url != null) {
                    webView.setClearHistory(true);
                    webView.loadUrl(url);
                    setActionBarTitle(defaultItem.getTitle().toString());
                    unselectAllMenuItemsExcept(defaultItem);
                    return;
                }
            } else {
                webView.goBack();
                return;
            }
        }

        super.onBackPressed();
    }

    protected KolibriWebView getWebView() {
        return webView;
    }

    protected KolibriLoadingView getWebviewOverlay() {
        return webviewOverlay;
    }
}
