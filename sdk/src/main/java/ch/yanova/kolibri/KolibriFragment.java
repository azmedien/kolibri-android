package ch.yanova.kolibri;

import android.content.Context;
import android.support.v4.app.Fragment;

/**
 * Created by lekov on 4/2/17.
 */

public class KolibriFragment extends Fragment {
    KolibriInitializeListener initializer;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof KolibriInitializeListener) {
            initializer = (KolibriInitializeListener) context;
        } else {
            throw new IllegalAccessError("context must be instance of KolibriNavigationActivity or must implement KolibriInitializeListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        initializer = null;
    }


    protected void bindComponents() {
        initializer.onBindComponents();
    }

}