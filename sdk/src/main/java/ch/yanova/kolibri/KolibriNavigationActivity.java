package ch.yanova.kolibri;

import static ch.yanova.kolibri.RuntimeConfig.COMPONENT;
import static ch.yanova.kolibri.RuntimeConfig.ICON;
import static ch.yanova.kolibri.RuntimeConfig.ID;
import static ch.yanova.kolibri.RuntimeConfig.ITEMS;
import static ch.yanova.kolibri.RuntimeConfig.LABEL;
import static ch.yanova.kolibri.RuntimeConfig.Navigation;
import static ch.yanova.kolibri.RuntimeConfig.NavigationItem;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
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
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
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
import ch.yanova.kolibri.components.KolibriLoadingView;
import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.components.KolibriWebViewClient;
import ch.yanova.kolibri.coordinators.WebViewCoordinator;
import ch.yanova.kolibri.network.NetworkChangeReceiver;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.AestheticActivity;
import com.afollestad.aesthetic.NavigationViewMode;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class KolibriNavigationActivity extends AestheticActivity implements
    NavigationView.OnNavigationItemSelectedListener, RuntimeListener {

  // this set prevents collecting targets by garbage collector
  final Set<Target> targets = new HashSet<>();

  protected RuntimeConfig configuration;
  private NetworkChangeReceiver receiver;

  private NavigationView navigationView;
  private DrawerLayout drawer;
  private final View.OnClickListener onFooterClick = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      final Intent intent = (Intent) v.getTag();
      notifyComponenets(intent);
      drawer.closeDrawer(GravityCompat.START);
    }
  };
  private ActionBarDrawerToggle drawerToggle;
  private FloatingActionButton floatingActionButton;
  private KolibriWebView webView;
  private KolibriLoadingView webviewOverlay;
  private boolean restarted;
  private boolean pageHasError;
  private Intent shareIntent;
  private Toolbar toolbar;
  private View headerImageContainer;

  private Kolibri.HandlerType notifyComponenets(Intent intent) {
    final Kolibri.HandlerType type = Kolibri
        .notifyComponents(KolibriNavigationActivity.this, intent);

    if (type == Kolibri.HandlerType.COMPONENT) {

      final Uri uri = Uri.parse(intent.getData().getQueryParameter("url"));
      final String target = Kolibri.getInstance(this).getTarget(uri);
      if (Kolibri.TARGET_SELF.equals(target)) {
        final String title = intent.getStringExtra(Intent.EXTRA_TITLE);
        setActionBarTitle(title);
      }
    }

    if (type == Kolibri.HandlerType.NONE) {
      final Intent errorIntent = Kolibri
          .getErrorIntent(this, intent.getStringExtra(Intent.EXTRA_TITLE),
              getString(R.string.text_update_app));
      Kolibri.notifyComponents(KolibriNavigationActivity.this, errorIntent);
    }

    return type;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.navigation_drawer);

    floatingActionButton = findViewById(R.id.kolibri_fab);
    webView = findViewById(R.id.webview);
    webviewOverlay = findViewById(R.id.overlay);

    toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    drawer = findViewById(R.id.drawer_layout);

    drawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open,
        R.string.navigation_drawer_close) {

      @Override
      public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);
        invalidateOptionsMenu();
      }

      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        invalidateOptionsMenu();
      }

      @Override
      public void onDrawerSlide(View drawerView, float slideOffset) {
        super.onDrawerSlide(drawerView, slideOffset);
        invalidateOptionsMenu();
      }
    };
    drawerToggle.setDrawerSlideAnimationEnabled(false);
    drawer.addDrawerListener(drawerToggle);

    navigationView = findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);

    headerImageContainer = navigationView.getHeaderView(0)
        .findViewById(R.id.header_image_container);

    restarted = false;

    receiver = new NetworkChangeReceiver(webView);

    webView.addKolibriWebViewClient(new KolibriWebViewClient() {

      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        shareIntent = null;
        pageHasError = false;

        invalidateOptionsMenu();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          if (!pageHasError) {
            // On old devices without commitVisible we delay preventing flickering.
            getWebviewOverlay().postDelayed(new Runnable() {
              @Override
              public void run() {
                getWebviewOverlay().showLoading();
              }
            }, 250);
          }
        }
      }

      @Override
      public void onPageCommitVisible(WebView view, String url) {
        super.onPageCommitVisible(view, url);
        if (!pageHasError) {
          getWebviewOverlay().showView();
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

        Kolibri.getInstance(KolibriNavigationActivity.this).applyRuntimeTheme(false);

        if (errorCode == ERROR_CONNECT || errorCode == ERROR_HOST_LOOKUP || errorCode == ERROR_UNKNOWN) {
          getWebviewOverlay().showError(getResources().getString(R.string.internet_error_message));
        } else {
          getWebviewOverlay().showError(
              String.format(Locale.getDefault(), "Error %d: %s", errorCode, description));
        }
      }
    });
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    drawerToggle.syncState();
  }

  @Override
  protected void onStart() {
    super.onStart();
    registerReceiver(receiver,
        new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    drawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    final int i = item.getItemId();

    if (i == R.id.action_share) {
      startActivity(Intent.createChooser(shareIntent, "Share link!"));
      return true;
    }

    return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
  }

  private void setupStyling() {
    if (configuration == null) {
      return;
    }

    Kolibri.getInstance(this).applyRuntimeTheme(true);

    final RuntimeConfig.Styling styling = configuration.getStyling();
    overrideHeaderBackground(styling);
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

    final RuntimeConfig runtime = Kolibri.getInstance(this).getRuntimeConfigFromCache();

    if (runtime != null) {
      final Navigation nav = runtime.getNavigation();
      constructNavigation(nav);
    }

    kolibri.loadRuntimeConfiguration(this);

    getWebView().resumeTimers();
    getWebView().onResume();

    if (restarted) {

      // Workaround RX observers of Aesthetic which resets the color
      final RuntimeConfig.Styling styling = configuration.getStyling();
      overrideHeaderBackground(styling);

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

  private void overrideHeaderBackground(RuntimeConfig.Styling styling) {

    if (styling.hasPaletteColor(RuntimeConfig.Styling.OVERRIDES_NAVIGATION_HEADER_BACKGROUND)) {
      final int navigationHeaderColor = styling
          .getPaletteColor(RuntimeConfig.Styling.OVERRIDES_NAVIGATION_HEADER_BACKGROUND);
      headerImageContainer.post(new Runnable() {
        @Override
        public void run() {
          headerImageContainer.setBackgroundColor(navigationHeaderColor);
          headerImageContainer.setTag(":aesthetic_ignore");
        }
      });
    }
  }

  @Override
  protected void onPause() {
    getWebView().onPause();
    getWebView().pauseTimers();
    super.onPause();
  }

  @Override
  protected void onStop() {
    unregisterReceiver(receiver);
    drawer.closeDrawer(Gravity.START);
    super.onStop();
  }

  @Override
  public boolean onNavigationItemSelected(@NonNull final MenuItem item) {

    KolibriApp.getInstance().logMenuItemToFirebase(item);

    final MenuItem previouslySelected = getSelectedMenuItem();
    final Intent intent = item.getIntent();

    final PackageManager packageManager = getPackageManager();
    if (intent.resolveActivity(packageManager) != null) {
      // Notify custom components in case they are activities
      KolibriApp.getInstance().logEvent(null, intent.getData().toString());

      // Post to navigation view and select previously one menu item
      // because we won't to select components that are handled by activities.
      navigationView.post(new Runnable() {
        @Override
        public void run() {
          item.setChecked(false);
          previouslySelected.setChecked(true);
        }
      });

      return false;
    }

    notifyComponenets(intent);
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

    menu.getItem(navigation.getSettings().getInt("default-item")).getIntent()
        .addCategory(Intent.CATEGORY_DEFAULT);

    if (!restarted) {
      loadDefaultItem();
    } else if (selectedMenuItemId != null) {
      final MenuItem itemByid = findMenuItem(selectedMenuItemId);
      navigationView.setCheckedItem(itemByid.getItemId());
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
      final Uri uri = Uri.parse(WebViewCoordinator.webViewUri);

      final Uri.Builder builder = uri.buildUpon();
      builder.appendQueryParameter("url", notificationUrl);

      final Intent intent = Kolibri.createIntent(builder.build());
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

            final Intent intent = Kolibri.createIntent(menuData);
            intent.putExtras(item.getIntent().getExtras());

            //There is a specific url that was pushed to the app
            if (urlToLoad != null && !urlToLoad.isEmpty()) {
              intent.putExtra(Kolibri.EXTRA_DEEPLINK, urlToLoad);
            }

            setIntent(null);
            Kolibri.notifyComponents(this, intent);
            navigationView.setCheckedItem(item.getItemId());
            return;
          }
        }
      }

      //If the was no id, load default one instead
      onNavigationItemSelected(getDefaultItem());
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
      Kolibri.notifyComponents(KolibriNavigationActivity.this, errorIntent);
      setIntent(null);
    }
  }

  @Override
  public void onLoaded(@NonNull final RuntimeConfig runtime, final boolean isFresh) {

    this.configuration = runtime;

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final Navigation navigation = runtime.getNavigation();

        // We may skip setup styling if we are coming from background or loading same configuration
        if (!restarted || isFresh) {
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
        } else {
          onNavigationInitialize();
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
    final LinearLayout footerView = navigationView.findViewById(R.id.footer);
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
    return ((LinearLayout) navigationView.findViewById(R.id.footer)).getChildCount() > 0;
  }

  @Override
  public boolean onFailed(final Exception e) {
    return false;
  }

  private void addNavigationItem(Menu menu, NavigationItem item) {
    final String label = item.getLabel();
    Uri componentUri = item.getUri();

    final Intent intent = Kolibri.createIntent(componentUri);
    intent.putExtra(Intent.EXTRA_TITLE, label);

    final String id = item.getId();
    intent.putExtra(Kolibri.EXTRA_ID, id);

    MenuItem menuItem = menu.add(0, id.hashCode(), Menu.NONE, label).setIntent(intent)
        .setCheckable(true);

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
    final int defaultItemIndex = configuration == null ?
        0 : configuration.getNavigation().getSettings().getInt("default-item");

    return getMenu().getItem(defaultItemIndex);
  }

  @Override
  public void onBackPressed() {

    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
      return;
    }

    final MenuItem selectedItem = getSelectedMenuItem();
    final MenuItem defaultItem = getDefaultItem();

    if (webView.canGoBack()) {

      // Go back from deeplink
      final Intent intent = webView.getIntent();

      if (intent.hasExtra(Kolibri.EXTRA_DEEPLINK)) {

        MenuItem category = findMenuItem(intent.getStringExtra(Kolibri.EXTRA_ID));

        if (category != null) {
          navigationView.setCheckedItem(category.getItemId());
        }

        intent.removeExtra(Kolibri.EXTRA_DEEPLINK);
        notifyComponenets(intent);
        return;
      } else {

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
            navigationView.setCheckedItem(defaultItem.getItemId());
            return;
          }
        } else {
          webView.goBack();
          return;
        }
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
}
