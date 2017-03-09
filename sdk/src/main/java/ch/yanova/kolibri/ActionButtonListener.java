package ch.yanova.kolibri;

import android.view.View;

/**
 * Created by lekov on 3/9/17.
 */

public interface ActionButtonListener {

    void showActionButton();
    void hideActionButton();
    void onActionButtonClick(View.OnClickListener listener);
}
