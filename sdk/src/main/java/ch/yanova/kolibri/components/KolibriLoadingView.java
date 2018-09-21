package ch.yanova.kolibri.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import ch.yanova.kolibri.R;

public class KolibriLoadingView extends FrameLayout {

  private View mLayoutOverlay;
  private View mLayoutError;

  private View mainContentView;

  private String errorMessage;
  private String buttonText;
  private Drawable drawable;

  public KolibriLoadingView(Context context) {
    super(context);
    init(null, 0);
  }

  public KolibriLoadingView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs, 0);
  }

  public KolibriLoadingView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(attrs, defStyle);
  }

  private void init(AttributeSet attrs, int defStyle) {
    // Load attributes
    final TypedArray a = getContext().obtainStyledAttributes(
        attrs, R.styleable.KolibriLoadingView, defStyle, 0);

    errorMessage = a.getString(R.styleable.KolibriLoadingView_errorMessage);
    buttonText = a.getString(R.styleable.KolibriLoadingView_buttonText);

    if (a.hasValue(R.styleable.KolibriLoadingView_drawable)) {
      drawable = a.getDrawable(R.styleable.KolibriLoadingView_drawable);
    }

    a.recycle();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    if (getChildCount() > 0) {
      mainContentView = getChildAt(0);
    }

    mLayoutOverlay = LayoutInflater.from(getContext()).inflate(R.layout.overlay, this, false);
    mLayoutError = mLayoutOverlay.findViewById(R.id.error);

    if (errorMessage != null) {
      ((TextView) mLayoutError.findViewById(R.id.error_text)).setText(errorMessage);
    }

    if (buttonText != null) {
      ((Button) mLayoutError.findViewById(R.id.error_button)).setText(buttonText);
    }

    if (drawable != null) {
      ((TextView) mLayoutError.findViewById(R.id.error_text))
          .setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
    }

    addView(mLayoutOverlay);
  }

  public void showError(String text, String buttonText, @DrawableRes Integer resDrawable) {
    mLayoutOverlay.setVisibility(View.VISIBLE);
    mainContentView.setVisibility(View.GONE);

    if (text != null) {
      ((TextView) mLayoutError.findViewById(R.id.error_text)).setText(text);
    }

    if (buttonText != null) {
      ((Button) mLayoutError.findViewById(R.id.error_button)).setText(buttonText);
    }

    if (resDrawable != null) {
      ((TextView) mLayoutError.findViewById(R.id.error_text))
          .setCompoundDrawablesWithIntrinsicBounds(0, resDrawable, 0, 0);
    }

    mLayoutError.setVisibility(View.VISIBLE);
  }

  public void showError(String text) {
    showError(text, null, null);
  }

  public void showError() {
    showError(null, null, null);
  }

  public void showView() {
    mLayoutError.setVisibility(View.GONE);
    mLayoutOverlay.setVisibility(View.GONE);
    mainContentView.setVisibility(View.VISIBLE);
  }
}
