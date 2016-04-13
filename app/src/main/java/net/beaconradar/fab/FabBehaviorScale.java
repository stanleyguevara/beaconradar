package net.beaconradar.fab;

import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.View;

public class FabBehaviorScale extends FabBehavior {
    private boolean mIsHiding = false;
    private boolean mIsShowing = false;
    private boolean mIsVisible = true;      //Is in place, with scale 1.0 and alpha 1.0
    private FabBehaviorListener mListener;

    public FabBehaviorScale() {
        super();
    }

    public void setListener(FabBehaviorListener listener){
        this.mListener = listener;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout parent, FloatingActionButton fab, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(parent, fab, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
        if (!mIsHiding && mIsVisible && (dyConsumed > 0 || dyUnconsumed > 0)) {
            hideScale(fab);
            mListener.onHide();
        } else if (!mIsShowing && !mIsVisible && (dyConsumed < 0 || dyUnconsumed < 0)) {
            showScale(fab);
            mListener.onShow();
        }
    }

    private void hideScale(final FloatingActionButton fab) {
        ViewCompat.animate(fab)
                .scaleX(0.0f)
                .scaleY(0.0f)
                .alpha(0.0f)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override public void onAnimationStart(View view) { mIsVisible = false; mIsShowing = false; mIsHiding = true;}
                    @Override public void onAnimationEnd(View view) { mIsHiding = false; fab.setVisibility(View.GONE); }
                    @Override public void onAnimationCancel(View view) { mIsHiding = false; }
                });
    }

    private void showScale(final FloatingActionButton fab) {
        ViewCompat.animate(fab)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override public void onAnimationStart(View view) { mIsVisible = false; mIsShowing = true; mIsHiding = false; fab.setVisibility(View.VISIBLE); }
                    @Override public void onAnimationEnd(View view) { mIsShowing = false; mIsVisible = true; }
                    @Override public void onAnimationCancel(View view) { mIsShowing = false; }
                });
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton fab, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            updateFabTranslationForSnackbar(parent, fab, (Snackbar.SnackbarLayout) dependency);
        } else super.onDependentViewChanged(parent, fab, dependency);
        return true;
    }

    @Override
    public void hideNow(CoordinatorLayout parent, FloatingActionButton fab) {
        ViewCompat.animate(fab).cancel();
        fab.setScaleX(0.0f);
        fab.setScaleY(0.0f);
        fab.setAlpha(0.0f);
        fab.setVisibility(View.GONE);
        mIsVisible = false; mIsHiding = false; mIsShowing = false;
    }

    @Override
    public void showNow(CoordinatorLayout parent, FloatingActionButton fab) {
        ViewCompat.animate(fab).cancel();
        fab.setScaleX(1.0f);
        fab.setScaleY(1.0f);
        fab.setAlpha(1.0f);
        fab.setVisibility(View.VISIBLE);
        mIsVisible = true; mIsHiding = false; mIsShowing = false;
    }
}