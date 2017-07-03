package ch.yanova.kolibri;

import android.animation.Animator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;

import static android.support.v4.graphics.ColorUtils.RGBToHSL;

/**
 * Created by lekov on 7/3/17.
 */

public class TintUtils {

    public static void tintSearchView(SearchView searchView, int color) {

        ImageView searchBtnClose = (ImageView) searchView.findViewById(R.id.search_close_btn);
        final Drawable searchDrawable = searchBtnClose.getDrawable();
        if (searchDrawable != null) {
            final Drawable wrapped = DrawableCompat.wrap(searchDrawable);
            searchDrawable.mutate();
            DrawableCompat.setTint(wrapped, color);
            searchBtnClose.setImageDrawable(searchDrawable);
        }

        SearchView.SearchAutoComplete searchText = (SearchView.SearchAutoComplete) searchView.findViewById(R.id.search_src_text);
        searchText.setTextColor(color);
        searchText.setHintTextColor(color);
    }

    public static void tintToolbar(final Activity activity, final Toolbar toolbar, final int color, final int colorDark, boolean animated) {
        final int textColor = getTextColor(color);
        final PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(textColor, PorterDuff.Mode.MULTIPLY);

        for (int i = 0; i < toolbar.getChildCount(); i++) {
            final View v = toolbar.getChildAt(i);

            //Step 1 : Changing the color of back button (or open drawer button).
            if (v instanceof ImageButton) {
                //Action Bar back button
                ((ImageButton) v).getDrawable().setColorFilter(colorFilter);
            }

            if (v instanceof ActionMenuView) {
                for (int j = 0; j < ((ActionMenuView) v).getChildCount(); j++) {

                    //Step 2: Changing the color of any ActionMenuViews - icons that are not back button, nor text, nor overflow menu icon.
                    //Colorize the ActionViews -> all icons that are NOT: back button | overflow menu
                    final View innerView = ((ActionMenuView) v).getChildAt(j);
                    if (innerView instanceof ActionMenuItemView) {
                        for (int k = 0; k < ((ActionMenuItemView) innerView).getCompoundDrawables().length; k++) {
                            if (((ActionMenuItemView) innerView).getCompoundDrawables()[k] != null) {
                                final int finalK = k;

                                //Important to set the color filter in seperate thread, by adding it to the message queue
                                //Won't work otherwise.
                                innerView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((ActionMenuItemView) innerView).getCompoundDrawables()[finalK].setColorFilter(colorFilter);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }

        Window window = activity.getWindow();

        // get the center for the clipping circle
        int cx = toolbar.getWidth() / 2;
        int cy = toolbar.getHeight() / 2;

        // get the final radius for the clipping circle
        float finalRadius = (float) Math.hypot(cx, cy);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (animated) {
                Animator anim = ViewAnimationUtils.createCircularReveal(toolbar, cx, cy, 0, finalRadius);
                anim.start();
            }
            window.setStatusBarColor(colorDark);
        }
        toolbar.setBackgroundColor(color);

        //Step 3: Changing the color of title and subtitle.
        toolbar.setTitleTextColor(textColor);
        toolbar.setSubtitleTextColor(textColor);

        //Step 4: Changing the color of the Overflow Menu icon.
        setOverflowButtonColor(activity, toolbar, textColor);

        tintAllIcons(toolbar.getMenu(), textColor);

        for (int i = 0; i < toolbar.getMenu().size(); i++) {
            MenuItem item = toolbar.getMenu().getItem(i);

            if (MenuItemCompat.getActionView(item) instanceof SearchView) {
                MenuItemCompat.setOnActionExpandListener(item, new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        tintSearchView((SearchView) MenuItemCompat.getActionView(item), textColor);
                        toolbar.post(new Runnable() {
                            @Override
                            public void run() {
                                tintToolbar(activity, toolbar, color, colorDark, false);
                            }
                        });
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        return true;
                    }
                });
            }
        }
    }

    private static float getLightness(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        float hsl[] = new float[3];
        RGBToHSL(red, green, blue, hsl);
        return hsl[2];
    }

    private static boolean isDarkColor(int color) {
        return getLightness(color) < 0.5f;
    }


    public static int getTextColor(int background) {
        return isDarkColor(background) ? Color.WHITE : Color.BLACK;
    }

    private static void tintAllIcons(Menu menu, final int color) {
        for (int i = 0; i < menu.size(); ++i) {
            final MenuItem item = menu.getItem(i);
            tintMenuItemIcon(color, item);
        }
    }

    private static void tintMenuItemIcon(int color, MenuItem item) {
        final Drawable drawable = item.getIcon();
        if (drawable != null) {
            final Drawable wrapped = DrawableCompat.wrap(drawable);
            drawable.mutate();
            DrawableCompat.setTint(wrapped, color);
            item.setIcon(drawable);
        }
    }

    /**
     * It's important to set overflowDescription atribute in styles, so we can grab the reference
     * to the overflow icon. Check: res/values/styles.xml
     *
     * @param activity
     */
    private static void setOverflowButtonColor(final Activity activity, final Toolbar toolbar, final int toolbarIconsColor) {
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                if (toolbar != null && toolbar.getOverflowIcon() != null) {
                    Drawable bg = DrawableCompat.wrap(toolbar.getOverflowIcon());
                    DrawableCompat.setTint(bg, toolbarIconsColor);
                }

                removeOnGlobalLayoutListener(decorView, this);
            }
        });
    }

    private static void removeOnGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            v.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
        } else {
            v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        }
    }
}
