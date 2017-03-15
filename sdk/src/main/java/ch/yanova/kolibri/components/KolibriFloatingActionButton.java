package ch.yanova.kolibri.components;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

import ch.yanova.kolibri.FloatingActionListener;

/**
 * Created by mmironov on 3/10/17.
 */

public class KolibriFloatingActionButton extends FloatingActionButton implements KolibriComponent {

    public static final String URI_SHOW = "kolibri://fab/show";
    public static final String URI_HIDE = "kolibri://fab/hide";

    private String urlToBookmark;

    public void setFloatingActionListener(FloatingActionListener listener) {
        this.listener = listener;
    }

    private FloatingActionListener listener;

    public KolibriFloatingActionButton(Context context) {
        super(context);
        init();
    }

    public KolibriFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KolibriFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        if (!isInEditMode()) {

        }
    }

    public String getUrlToBookmark() {
        return urlToBookmark;
    }

    @Override
    public void handleIntent(Intent intent) {

        urlToBookmark = intent.getData().getQueryParameter("url");

        if (intent.getDataString().startsWith(URI_SHOW)) {
            show();

            if (listener != null) {
                listener.onVisibilityChanged(true);
            }
        }

        if (intent.getDataString().startsWith(URI_HIDE)) {
            hide();

            if (listener != null) {
                listener.onVisibilityChanged(false);
            }
        }
    }

}
