package net.beaconradar.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public abstract class BaseLayout extends FrameLayout {
    public BaseLayout(Context context) {
        super(context);
    }

    public BaseLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BaseLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public abstract void initViews();

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
    }

    @Override
    protected void measureChildWithMargins(View child,
                                           int parentWidthMeasureSpec,  int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        final int childWidthMeasureSpec = getChildMeasureSpec(
                parentWidthMeasureSpec,
                lp.leftMargin + lp.rightMargin + widthUsed,
                lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(
                parentHeightMeasureSpec,
                lp.topMargin + lp.bottomMargin + heightUsed,
                lp.height);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    protected int widthWithMargins(View child) {
        MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
        return child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
    }

    protected int heightWithMargins(View child) {
        MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
        return child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
    }

    protected void placeChild(View child, int left, int top) {
        MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
        child.layout(
                left + lp.leftMargin,
                top + lp.topMargin,
                left + lp.leftMargin + child.getMeasuredWidth(),
                top + lp.topMargin + child.getMeasuredHeight());
    }

    /*@Override
    public MarginLayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    // Someone called addView() but forgot to specify layout params.
    @Override
    protected MarginLayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT);
    }

    // These two methods are used to convert layout params of an incorrect type.
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

    @Override
    protected MarginLayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new MarginLayoutParams(p);
    }*/
}
