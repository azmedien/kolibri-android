package ch.yanova.kolibri;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by mmironov on 5/7/17.
 */

public abstract class KolibriLoadingFragment extends KolibriFragment {

    private View mLayoutError;
    private View mLayoutLoading;
    private View mLayoutOverlay;

    private View mainContentView;

    @NonNull
    protected abstract View getMainContentView(LayoutInflater inflater, ViewGroup container);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.loading_layout, container, false);

        mainContentView = getMainContentView(inflater, container);
        layout.addView(mainContentView, 0);

        mLayoutError = layout.findViewById(R.id.error);
        mLayoutLoading = layout.findViewById(R.id.progress);
        mLayoutOverlay = layout.findViewById(R.id.overlay);

        return layout;
    }

    public void showPageLoading() {

        mLayoutOverlay.setVisibility(View.VISIBLE);
        mLayoutError.setVisibility(View.GONE);
        mainContentView.setVisibility(View.GONE);
        mLayoutLoading.setVisibility(View.VISIBLE);
    }

    public void showPageError(String text, String buttonText, @DrawableRes Integer resDrawable) {

        mLayoutOverlay.setVisibility(View.VISIBLE);
        mLayoutLoading.setVisibility(View.GONE);
        mainContentView.setVisibility(View.GONE);

        if (text != null) {
            ((TextView) mLayoutError.findViewById(R.id.error_text)).setText(text);
        }

        if (buttonText != null) {
            ((Button) mLayoutError.findViewById(R.id.error_button)).setText(buttonText);
        }

        if (resDrawable != null) {
            ((TextView)mLayoutError.findViewById(R.id.error_text)).setCompoundDrawablesWithIntrinsicBounds(0, resDrawable, 0, 0);
        }

        mLayoutError.setVisibility(View.VISIBLE);

    }

    public void showPage() {
        mLayoutLoading.setVisibility(View.GONE);
        mLayoutError.setVisibility(View.GONE);
        mLayoutOverlay.setVisibility(View.GONE);
        mainContentView.setVisibility(View.VISIBLE);
    }

    public void setProgressColor(int color) {
        ProgressBar v = (ProgressBar) mLayoutLoading;
        v.getIndeterminateDrawable().setColorFilter(color,
                android.graphics.PorterDuff.Mode.MULTIPLY);
    }
}
