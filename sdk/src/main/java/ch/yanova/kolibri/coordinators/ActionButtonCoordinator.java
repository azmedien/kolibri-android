package ch.yanova.kolibri.coordinators;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import ch.yanova.kolibri.KolibriCoordinator;

/**
 * Created by lekov on 3/21/17.
 */

public class ActionButtonCoordinator extends KolibriCoordinator<FloatingActionButton> {

    public static final String URI_SHOW = "kolibri://fab/show";
    public static final String URI_HIDE = "kolibri://fab/hide";

    private static String[] sURIs = new String[]{URI_SHOW, URI_HIDE};

    @Override
    public void handleIntent(FloatingActionButton view, Intent intent) {
        if (intent.getDataString().startsWith(URI_SHOW)) {
            view.show();
        } else if (intent.getDataString().startsWith(URI_HIDE)) {
            view.hide();
        }
    }

    @Override
    public String[] kolibriUris() {
        return sURIs;
    }
}
