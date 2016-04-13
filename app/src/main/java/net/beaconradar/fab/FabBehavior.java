package net.beaconradar.fab;

import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.View;

import java.util.List;

public abstract class FabBehavior extends FloatingActionButton.Behavior {
    public static final int FAB_BEHAVIOR_SCALE = 0;
    public static final int FAB_BEHAVIOR_MOVE = 1;
    public static final int FAB_BEHAVIOR_FIXED = 2;

    protected float mFabTranslationY;

    public FabBehavior() {
        super();
    }

    public interface FabBehaviorListener {
        void onShow();
        void onHide();
    }

    public abstract void setListener(FabBehaviorListener listener);
    public abstract void hideNow(CoordinatorLayout coordinator, FloatingActionButton child);
    public abstract void showNow(CoordinatorLayout coordinator, FloatingActionButton child);

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child, View directTargetChild, View target, int nestedScrollAxes) {
        return  nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL ||
                super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes);
    }

    //Bug fix from original impl (23.1.0) removed [if (fab.getVisibility() != View.VISIBLE) { return; } ]
    protected void updateFabTranslationForSnackbar(CoordinatorLayout parent, FloatingActionButton fab, @Nullable Snackbar.SnackbarLayout snack) {
        final float targetTransY = getFabTranslationYForSnackbar(parent, fab, snack);
        if (mFabTranslationY == targetTransY) {
            // We're already at (or currently animating to) the target value, return...
            return;
        }
        mFabTranslationY = targetTransY;
        final float currentTransY = ViewCompat.getTranslationY(fab);
        final float dy = currentTransY - targetTransY;

        if (Math.abs(dy) > (fab.getHeight() * 0.667f)) {
            // If the FAB will be travelling by more than 2/3 of it's height, let's animate
            // it instead
            ViewCompat.animate(fab)
                    .translationY(targetTransY)
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .setListener(null);
        } else {
            // Make sure that any current animation is cancelled
            ViewCompat.animate(fab).cancel();
            // Now update the translation Y
            ViewCompat.setTranslationY(fab, targetTransY);
        }
    }

    public float getFabTranslationYForSnackbar(CoordinatorLayout parent, FloatingActionButton fab, @Nullable Snackbar.SnackbarLayout snack) {
        float minOffset = 0;
        if(snack == null) {
            final List<View> dependencies = parent.getDependencies(fab);
            for (int i = 0, z = dependencies.size(); i < z; i++) {
                final View view = dependencies.get(i);
                if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(fab, view)) {
                    minOffset = Math.min(minOffset,
                            ViewCompat.getTranslationY(view) - view.getHeight());
                }
            }
        } else {
            if (parent.doViewsOverlap(fab, snack)) {
                minOffset = Math.min(minOffset, ViewCompat.getTranslationY(snack) - snack.getHeight());
            }
        }
        return minOffset;
    }
}
