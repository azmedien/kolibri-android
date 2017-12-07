package ch.yanova.kolibri.prototype;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriCoordinator;
import ch.yanova.kolibri.KolibriNavigationActivity;
import ch.yanova.kolibri.KolibriProvider;
import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.components.KolibriWebViewClient;
import ch.yanova.kolibri.coordinators.ActionButtonCoordinator;
import ch.yanova.kolibri.coordinators.SearchWebviewCoordinator;

public class PrototypeActivity extends KolibriNavigationActivity {

  private NetworkChangeReceiver networkChangeReceiver;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    networkChangeReceiver = new NetworkChangeReceiver(getWebView());

    getWebView().addKolibriWebViewClient(new KolibriWebViewClient() {
      @Override
      protected boolean onCustomTarget(Uri link, String target) {

        if ("360player".equals(target)) {
          startActivity(CardboardActivity.createIntent(PrototypeActivity.this, link));
          return true;
        }

        return super.onCustomTarget(link, target);
      }
    });

    Kolibri.bind(getWebView(), new KolibriProvider() {
      @Nullable
      @Override
      public KolibriCoordinator provideCoordinator(@NonNull View view) {
        return new SearchWebviewCoordinator() {
          @Override
          protected void handleIntent(KolibriWebView view, Intent intent) {
            super.handleIntent(view, intent);
          }
        };
      }
    });

    Kolibri.bind(getFloatingActionButton(), new KolibriProvider() {
      @Nullable
      @Override
      public KolibriCoordinator provideCoordinator(@NonNull View view) {
        return new ActionButtonCoordinator();
      }
    });

    getFloatingActionButton().setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar
            .make(view, "Content added to favorites list", BaseTransientBottomBar.LENGTH_INDEFINITE)
            .setAction("Check it out", new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                finish();
              }
            }).show();
      }
    });

    getWebView().setHandleInternal(true);
  }

  @Override
  protected void onStart() {
    super.onStart();
    registerReceiver(networkChangeReceiver,
        new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
  }

  @Override
  protected void onStop() {
    super.onStop();
    unregisterReceiver(networkChangeReceiver);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {

    boolean res = super.onPrepareOptionsMenu(menu);

    MenuItem searchItem = menu.findItem(R.id.action_search);
    SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        getWebViewCoordinator().onQueryByText(query);
        return true;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        return false;
      }
    });

    return res;
  }

  private SearchWebviewCoordinator getWebViewCoordinator() {
    return ((SearchWebviewCoordinator) getWebView().getTag(R.id.coordinator));
  }
}
