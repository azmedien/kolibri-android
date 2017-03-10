package ch.yanova.kolibri.components;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

/**
 * Created by mmironov on 3/10/17.
 */

public class KolibriFloatingActionButton extends FloatingActionButton implements KolibriComponent {

    public static final String URI_SHOW = "kolibri://fab/show";
    public static final String URI_HIDE = "kolibri://fab/hide";

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

    @Override
    public void handleIntent(Intent intent) {

        if (URI_SHOW.equals(intent.getDataString())) {
            show();
        }

        if (URI_HIDE.equals(intent.getDataString())) {
            hide();
        }
    }
}
