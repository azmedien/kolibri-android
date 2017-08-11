package ch.yanova.kolibri;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

import ch.yanova.kolibri.notifications.InternalNotificationsReceiver;

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
import static ch.yanova.kolibri.notifications.InternalNotificationsReceiver.KOLIBRI_ID_INTENT;
import static ch.yanova.kolibri.notifications.InternalNotificationsReceiver.KOLIBRI_NOTIFICATION_INTENT;

public abstract class KolibriNavigationActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        RuntimeListener, KolibriInitializeListener {

    public static final String TAG_MAIN_FRAGMENT = "mainFragment";
    // this set prevents collecting targets by garbage collector
    final Set<Target> targets = new HashSet<>();
    protected RuntimeConfig configuration;
    private NavigationView navigationView;
    private FloatingActionButton floatingActionButton;
    private View mLayoutError;
    private View mLayoutLoading;
    private View mLayoutOverlay;
    private boolean restarted;
    private DrawerLayout drawer;
    private final View.OnClickListener onFooterClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Intent intent = (Intent) v.getTag();

            final String title = intent.getStringExtra(Intent.EXTRA_TITLE);

            final Kolibri kolibri = Kolibri.getInstance(KolibriNavigationActivity.this);
            kolibri.setPreviousMenuItem(kolibri.selectedMenuitem());
            kolibri.setSelectedMenuItem(title);
            kolibri.setFromMenuItemClick(true);

            Kolibri.notifyComponents(KolibriNavigationActivity.this, intent);
            drawer.closeDrawer(GravityCompat.START);
        }
    };
    private Toolbar toolbar;
    private View headerImageContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onPreInitialize();

        setContentView(R.layout.navigation_drawer);

        floatingActionButton = (FloatingActionButton) findViewById(R.id.kolibri_fab);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mLayoutError = navigationView.findViewById(R.id.error);
        mLayoutLoading = navigationView.findViewById(R.id.progress);
        mLayoutOverlay = navigationView.findViewById(R.id.overlay);

        headerImageContainer = navigationView.getHeaderView(0).findViewById(R.id.header_image_container);

        final Fragment fragment = onPostInitialize();
        startMainFragment(fragment);

        restarted = false;
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
        final Kolibri kolibri = Kolibri.getInstance(this);
        if (kolibri.fromMenuItemClick() && kolibri.previousMenuItem() != null) {
            getSupportActionBar().setTitle(kolibri.previousMenuItem());
            kolibri.setSelectedMenuItem(kolibri.previousMenuItem());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showNavigationLoading();
        Kolibri kolibri = Kolibri.getInstance(this);
        kolibri.loadRuntimeConfiguration(this);
    }

    private void startMainFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.kolibri_main_content, fragment, TAG_MAIN_FRAGMENT).commitAllowingStateLoss();
    }

    @NonNull
    protected Fragment getMainFragment() {
        return getSupportFragmentManager().findFragmentByTag(TAG_MAIN_FRAGMENT);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        final Intent intent = item.getIntent();

        final String title = intent.getStringExtra(Intent.EXTRA_TITLE);

        final Kolibri kolibri = Kolibri.getInstance(this);
        kolibri.setPreviousMenuItem(kolibri.selectedMenuitem());
        kolibri.setSelectedMenuItem(title);
        kolibri.setFromMenuItemClick(true);

        KolibriApp.getInstance().logMenuItemToFirebase(item);

        final boolean handled = Kolibri.notifyComponents(this, intent);

        if (!handled) {
            Kolibri.notifyComponents(this, Kolibri.getErrorIntent(this, "No Such Component Exists!"));
        }

        for (int i = 0; i < navigationView.getMenu().size(); i++) {
            if (item.equals(navigationView.getMenu().getItem(i))) {
                item.setChecked(true);
            } else {
                navigationView.getMenu().getItem(i).setChecked(false);
            }
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void constructNavigation(Navigation navigation) {
        final Menu menu = navigationView.getMenu();
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

        showNavigation();

        if (!restarted) {
            loadDefaultItem();
        }

        onNavigationInitialize();
    }

    protected void loadDefaultItem() {

        final Menu menu = navigationView.getMenu();

        for (int i = 0; i < menu.size(); i++) {
            final MenuItem item = menu.getItem(i);
            if (item.getIntent().getCategories().contains(Intent.CATEGORY_DEFAULT)) {
                item.setChecked(true);
                Kolibri.notifyComponents(this, item.getIntent());
                break;
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onNavigationInitialize() {
        Intent intent = getIntent();

        if (intent == null) {
            return;
        }

        intent = prepareIntentFromNotification(intent);

        if (intent.hasExtra(ID)) {
            final Menu menu = navigationView.getMenu();

            for (int i = 0; i < menu.size(); i++) {

                final MenuItem item = menu.getItem(i);
                final String idInMenu = item.getIntent().getStringExtra(ID);

                if (intent.getStringExtra(ID).equals(idInMenu)) {

                    final Uri data = intent.getData();
                    final String urlToLoad = data.getQueryParameter("url");
                    final Uri menuData = item.getIntent().getData();

                    //There is a specific url that was pushed to the app
                    if (urlToLoad != null) {
                        intent.putExtra(Kolibri.EXTRA_GO_BACK_URL, menuData.getQueryParameter("url"));
                    } else { //We will load the url for the navigation item having the id from the notification
                        final String url = menuData.getQueryParameter("url");
                        final Uri uriWithUrl = data.buildUpon().appendQueryParameter("url", url).build();
                        intent.setData(uriWithUrl);
                    }

                    unselectAllMenuItemsExcept(item);
                    intent.putExtra(Intent.EXTRA_TITLE, item.getTitle());

                    final Kolibri kolibri = Kolibri.getInstance(this);
                    kolibri.setPreviousMenuItem(kolibri.selectedMenuitem());
                    kolibri.setSelectedMenuItem(item.getTitle().toString());

                    setIntent(null);
                    Kolibri.notifyComponents(this, intent);
                    return;
                }
            }

            setIntent(null);
            Kolibri.notifyComponents(this, Kolibri.getErrorIntent(this, "No Such Component Exists!"));
            return;
        }

        if (intent.getData() != null && intent.getData().getQueryParameter("url") != null) {
            setIntent(null);
            Kolibri.notifyComponents(this, intent);
        }
    }

    private void unselectAllMenuItemsExcept(MenuItem item) {
        final Menu menu = navigationView.getMenu();

        for (int m = 0; m < menu.size(); m++) {
            if (!menu.getItem(m).equals(item)) {
                menu.getItem(m).setChecked(false);
            }
        }

        item.setChecked(true);
    }

    private Intent prepareIntentFromNotification(Intent intent) {

        Intent result = intent;

        if (intent.hasExtra("component")) {
            String componentUri = intent.getStringExtra("component");

            if (componentUri != null) {
                result = InternalNotificationsReceiver.getResultIntent(this, componentUri);
            } else {
                result = Kolibri.createIntent(Uri.parse("kolibri://notification"));
            }

            final PackageManager packageManager = getPackageManager();
            if (result.resolveActivity(packageManager) == null) {

                if (result.hasExtra(Kolibri.EXTRA_ID)) {
                    final String id = result.getStringExtra(Kolibri.EXTRA_ID);

                    if (Kolibri.getInstance(this).getRuntime().getNavigation().hasItem(id)) {
                        final String query = result.getData().getQuery();

                        String modifiedUri = KOLIBRI_NOTIFICATION_INTENT;
                        if (query != null) {
                            modifiedUri += "?" + query;
                        }

                        result = Kolibri.createIntent(Uri.parse(modifiedUri));
                        result.putExtra(Kolibri.EXTRA_ID, id);
                    }
                } else {
                    Log.e("KolibriNotifications", "Notification received but nobody cannot handle the deeplink.");
                    result = Kolibri.getErrorIntent(this, "Content of this type cannot be open.");
                }

            }
        }
        return result;
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
                        final FrameLayout layout = (FrameLayout) headerView.findViewById(R.id.header_image_container);
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
        final LinearLayout footerView = (LinearLayout) navigationView.findViewById(R.id.kolibri_footer);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showNavigationError(e.getLocalizedMessage());
            }
        });
        return false;
    }

    private void addNavigationItem(Menu menu, NavigationItem item) {
        final String label = item.getLabel();
        String componentUri = item.getComponent();

        if (item.hasSetting("url")) {
            final String url = item.getString("url");
            componentUri += "?url=" + url;
        }

        final Uri uri = Uri.parse(componentUri);
        final Intent intent = Kolibri.createIntent(uri);
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

    @UiThread
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

    @UiThread
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

    @UiThread
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

    public Toolbar getToolbar() {
        return toolbar;
    }
}
