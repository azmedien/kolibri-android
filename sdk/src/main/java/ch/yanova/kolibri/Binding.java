package ch.yanova.kolibri;

import android.support.annotation.NonNull;
import android.view.View;

import ch.yanova.kolibri.components.KolibriComponent;

final class Binding implements View.OnAttachStateChangeListener {

  private KolibriCoordinator kolibriCoordinator;
  private KolibriComponent component;

  Binding(KolibriCoordinator kolibriCoordinator, KolibriComponent component) {
    this.kolibriCoordinator = kolibriCoordinator;
    this.component = component;
  }
  
  @Override public void onViewAttachedToWindow(@NonNull View v) {
    // ...
    kolibriCoordinator.attach(component);
//    view.setTag(R.id.kolibriCoordinator, kolibriCoordinator);  // <------- !!!
  }

  @Override public void onViewDetachedFromWindow(@NonNull View v) {
    // ...
    kolibriCoordinator.detach(component);
//    view.setTag(R.id.kolibriCoordinator, null);  // <------- !!!
  }
}