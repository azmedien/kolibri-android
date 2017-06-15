package ch.yanova.kolibri;

import android.animation.Animator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.WindowManager;
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
import java.util.Map;
import java.util.Set;

public abstract class KolibriNavigationActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        RuntimeListener, KolibriInitializeListener {

    public static final String TAG_MAIN_FRAGMENT = "mainFragment";
    private NavigationView navigationView;
    private FloatingActionButton floatingActionButton;

    private View mLayoutError;
    private View mLayoutLoading;
    private View mLayoutOverlay;

    private boolean restarted;

    private final View.OnClickListener onFooterClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Intent i = (Intent) v.getTag();
            startActivity(Intent.createChooser(i, "Open with..."));
        }
    };
    private DrawerLayout drawer;
    private Toolbar toolbar;
    private View headerImageContainer;
    protected RuntimeConfig configuration;

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

    private void setupStyling() {
        if (configuration == null) {
            return;
        }

        final RuntimeConfig.Styling styling = configuration.getStyling();

        if (styling.hasOverridesFor(RuntimeConfig.Styling.OVERRIDES_TOOLBAR_BACKGROUND)) {
            final int toolbarBackgroud = styling.getToolbarBackgroundOverride();
            final int[] palette = RuntimeConfig.getMaterialPalette(String.format("#%06X", 0xFFFFFF & toolbarBackgroud));

            tintTheme(toolbar, palette[RuntimeConfig.THEME_COLOR_PRIMARY], palette[RuntimeConfig.THEME_COLOR_PRIMARY_DARK], true);
            headerImageContainer.setBackgroundColor(palette[RuntimeConfig.THEME_COLOR_PRIMARY]);
        }

        if (styling.hasOverridesFor(RuntimeConfig.Styling.OVERRIDES_TOOLBAR_TEXT)) {
            toolbar.setTitleTextColor(styling.getToolbarTextOverride());
        }


        if (styling.hasOverridesFor(RuntimeConfig.Styling.OVERRIDES_MENU_ITEM_SELECTED)) {
            final int menuItemSelected = styling.getToolbarBackgroundOverride();
            final int[] palette = RuntimeConfig.getMaterialPalette(String.format("#%06X", 0xFFFFFF & menuItemSelected));

            // FOR NAVIGATION VIEW ITEM TEXT COLOR
            int[][] states = new int[][]{
                    new int[]{-android.R.attr.state_checked},  // unchecked
                    new int[]{android.R.attr.state_checked},   // checked
                    new int[]{}                                // default
            };

            // Fill in color corresponding to state defined in state
            int[] colors = new int[]{
                    Color.parseColor("#000000"),
                    palette[RuntimeConfig.THEME_COLOR_PRIMARY],
                    Color.parseColor("#000000"),
            };

            ColorStateList navigationViewColorStateList = new ColorStateList(states, colors);

            // apply to text color
            navigationView.setItemTextColor(navigationViewColorStateList);

            // apply to icon color
            navigationView.setItemIconTintList(navigationViewColorStateList);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restarted = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
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

        Kolibri.setSelectedMenuItem(title);
        KolibriApp.getInstance().logMenuItemToFirebase(item);

        final PackageManager packageManager = getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        Kolibri.notifyComponents(getApplicationContext(), intent);

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

    private void constructNavigation(RuntimeConfig.Navigation navigation) {
        final Menu menu = navigationView.getMenu();
        menu.clear();

        final Map<String, RuntimeConfig.NavigationItem> items = navigation.getItems();
        for (String id : items.keySet()) {
            final RuntimeConfig.NavigationItem item = items.get(id);
            addNavigationItem(menu, item);


            if (!item.hasSubItems())
                continue;

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
    public void onLoaded(@NonNull final RuntimeConfig runtime) {

        this.configuration = runtime;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final RuntimeConfig.Navigation navigation = runtime.getNavigation();

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
            Picasso.with(this).load(header.getString("background")).into(target);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            Picasso.with(this).load(header.getString("image")).into((ImageView) headerView.findViewById(R.id.header_image));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void constructFooter(JSONObject footer) throws JSONException {

        if (!footer.has("items") || isFooterConstructed()) {
            return;
        }

        final JSONArray items = footer.getJSONArray("items");
        final LinearLayout footerView = (LinearLayout) navigationView.findViewById(R.id.kolibri_footer);
        final LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < items.length(); i++) {
            final TextView tv = (TextView) inflater.inflate(R.layout.footer_item, footerView, false);
            tv.setText(items.getJSONObject(i).getString("label"));
            tv.setTag(Kolibri.createIntent(Uri.parse(items.getJSONObject(i).getString("url"))));
            tv.setOnClickListener(onFooterClick);

            footerView.addView(tv);
        }

    }

    private void tintTheme(View view, int colorPrimary, int colorPrimaryDark, boolean tint) {

        Window window = getWindow();

        // get the center for the clipping circle
        int cx = view.getWidth() / 2;
        int cy = view.getHeight() / 2;

        // get the final radius for the clipping circle
        float finalRadius = (float) Math.hypot(cx, cy);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);
            anim.start();

            if (!tint) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(colorPrimaryDark);
            }
        }
        view.setBackgroundColor(colorPrimary);
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

    private void addNavigationItem(Menu menu, RuntimeConfig.NavigationItem item) {
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

        if (item.hasSetting("icon-normal")) {
            String iconUrl = item.getString("icon-normal");
            loadMenuIcon(menuItem, iconUrl);
        }
    }

    // this set prevents collecting targets by garbage collector
    final Set<Target> targets = new HashSet<>();

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
