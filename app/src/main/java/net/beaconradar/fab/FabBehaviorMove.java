package net.beaconradar.fab;

import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.View;

public class FabBehaviorMove extends FabBehavior {
    private float mHideTranslationY = 350;    //TODO
    private boolean mIsHiding = false;
    private boolean mIsShowing = false;
    private boolean mIsVisible = true;      //Is in place, on top.
    private FabBehaviorListener mListener;

    public FabBehaviorMove() {
        super();
    }

    public void setListener(FabBehaviorListener listener){
        this.mListener = listener;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout parent, FloatingActionButton fab, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(parent, fab, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
        if (!mIsHiding && mIsVisible && (dyConsumed > 0 || dyUnconsumed > 0)) {
            hideTranslate(parent, fab);
            mListener.onHide();
        } else if (!mIsShowing && !mIsVisible && (dyConsumed < 0 || dyUnconsumed < 0)) {
            showTranslate(parent, fab);
            mListener.onShow();
        }
    }

    private void hideTranslate(CoordinatorLayout parent, FloatingActionButton fab) {
        ViewCompat.animate(fab).cancel();
        ViewCompat.animate(fab)
                .translationY(getFabTranslationYForSnackbar(parent, fab, null)+mHideTranslationY)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override public void onAnimationStart(View view) { mIsVisible = false; mIsShowing = false; mIsHiding = true; }
                    @Override public void onAnimationEnd(View view) { mIsHiding = false; }
                    @Override public void onAnimationCancel(View view) { mIsHiding = false; }
                });
    }

    private void showTranslate(CoordinatorLayout parent, FloatingActionButton fab) {
        ViewCompat.animate(fab).cancel();
        ViewCompat.animate(fab)
                .translationY(getFabTranslationYForSnackbar(parent, fab, null))
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override public void onAnimationStart(View view) { mIsVisible = false; mIsShowing = true; mIsHiding = false; }
                    @Override public void onAnimationEnd(View view) { mIsShowing = false; mIsVisible = true; }
                    @Override public void onAnimationCancel(View view) { mIsShowing = false; }
                });
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton fab, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            if(mIsVisible || mIsShowing) updateFabTranslationForSnackbar(parent, fab, (Snackbar.SnackbarLayout) dependency);
        } else super.onDependentViewChanged(parent, fab, dependency);
        return true;
    }

    @Override
    public void hideNow(CoordinatorLayout parent, FloatingActionButton fab) {
        ViewCompat.animate(fab).cancel();
        fab.setTranslationY(getFabTranslationYForSnackbar(parent, fab, null) + mHideTranslationY);
        mIsVisible = false;
        mIsShowing = false; mIsHiding = false;
    }

    @Override
    public void showNow(CoordinatorLayout parent, FloatingActionButton fab) {
        ViewCompat.animate(fab).cancel();
        fab.setTranslationY(getFabTranslationYForSnackbar(parent, fab, null));
        mIsVisible = true;
        mIsShowing = false; mIsHiding = false;
    }
}