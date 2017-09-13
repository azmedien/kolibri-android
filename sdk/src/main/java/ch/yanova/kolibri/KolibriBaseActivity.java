package ch.yanova.kolibri;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;

import ch.yanova.kolibri.components.KolibriWebView;
import ch.yanova.kolibri.coordinators.ClientWebViewCoordinator;


public abstract class KolibriBaseActivity extends KolibriNavigationActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onPreInitialize() {
    }

    @Override
    public Fragment onPostInitialize() {
        return WebViewFragment.newInstance("");
    }

    @Override
    public void onBindComponents() {

        Kolibri.bind(getWebView(), new KolibriProvider() {
            @Nullable
            @Override
            public KolibriCoordinator provideCoordinator(View view) {
                return new ClientWebViewCoordinator(getMainWebViewFragment(), getMainWebViewFragment());
            }
        });
    }

    public KolibriWebView getWebView() {
        final WebViewFragment webViewFragment = (WebViewFragment) getMainFragment();
        return webViewFragment.getWebView();
    }

    public WebViewFragment getMainWebViewFragment() {
        return (WebViewFragment) getMainFragment();
    }

    @Override
    public void onBackPressed() {
        if (getWebView().canGoBack()) {

            final MenuItem selectedItem = getSelectedMenuItem();
            final String selectedItemurl = selectedItem.getIntent()
                    .getData().getQueryParameter("url");
            final String currentUrl = getWebView().getOriginalUrl();

            //If the url we are going back from
            //came from a menu item click, we clear the history
            //and go back home
            if (currentUrl.equals(selectedItemurl)) {
                final MenuItem item = getMenu().getItem(0);
                final String url = item.getIntent().getData().getQueryParameter("url");

                //If there is url on item with index 0, then we load this url and clear that history
                //so that the next time the user presses back they are redirected out
                //of the app
                if (url != null) {
                    getWebView().setClearHistory(true);
                    getWebView().loadUrl(url);
                    setActionBarTitle(item.getTitle().toString());
                    unselectAllMenuItemsExcept(item);

                } else {
                    super.onBackPressed();
                }
            } else {
                getWebView().goBack();
            }
        } else {
            super.onBackPressed();
        }
    }
}
