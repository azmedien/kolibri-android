package ch.yanova.kolibri.components;

import android.net.Uri;
import android.os.Build;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import ch.yanova.kolibri.Kolibri;
import ch.yanova.kolibri.KolibriApp;

import static ch.yanova.kolibri.Kolibri.TARGET_EXTERNAL;

/**
 * Created by mmironov on 8/23/17.
 */

public class KolibriWebChromeClient extends WebChromeClient {

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && newProgress == 100) {
            final String url = view.getUrl();
            if ("about:blank".equals(url)) {
                return;
            }

            final Uri link = Uri.parse(url);
            final String target = Kolibri.getInstance(view.getContext()).getTarget(link);

            // Skip external targets when reporting to netmetrix
            if (!TARGET_EXTERNAL.equals(target)) {
                KolibriApp.getInstance().logEvent(null, link.toString());
            }
        }
    }
}
