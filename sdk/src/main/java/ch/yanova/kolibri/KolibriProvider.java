package ch.yanova.kolibri;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * Created by lekov on 3/21/17.
 */

public interface KolibriProvider {
    @Nullable
    KolibriCoordinator provideCoordinator(@NonNull View view);
}
