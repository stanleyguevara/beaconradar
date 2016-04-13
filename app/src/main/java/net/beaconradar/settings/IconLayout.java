package net.beaconradar.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import net.beaconradar.utils.BaseLayout;
import net.beaconradar.utils.TintableImageView;

public class IconLayout extends BaseLayout {
    private TintableImageView mIcon;
    private TextView mText, mSub;

    public IconLayout(Context context) {
        super(context);
    }

    public IconLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IconLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public IconLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    //Factory-inflating method. Pass the parent and layout, get back inflated BeaconLayout in place.
    public static IconLayout inflateLayoutInto(int layout, ViewGroup root) {
        LayoutInflater inflater = LayoutInflater.from(root.getContext());
        IconLayout inflated = (IconLayout) inflater.inflate(layout, root, false);
        return inflated;
    }

    @Override
    public void initViews() {
        mIcon = (TintableImageView) getChildAt(0);
        mText = (TextView) getChildAt(1);
        mSub = (TextView) getChildAt(2);
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int wUsed = getPaddingStart() + getPaddingEnd();
        int hUsed = getPaddingTop() + getPaddingBottom();
        int hIcon = 0;
        int hSpecIcon;

        measureChildWithMargins(mIcon, wSpec, wUsed, hSpec, hUsed);
        wUsed += widthWithMargins(mIcon);
        hIcon = heightWithMargins(mIcon) + getPaddingTop() + getPaddingBottom();
        hSpecIcon = MeasureSpec.makeMeasureSpec(hIcon, MeasureSpec.EXACTLY);
        measureChildWithMargins(mText, wSpec, wUsed, hSpec, hUsed);
        hUsed += heightWithMargins(mText);
        measureChildWithMargins(mSub, wSpec, wUsed, hSpecIcon, hUsed);

        setMeasuredDimension(resolveSize(wUsed, wSpec), resolveSize(hIcon, hSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int x = getPaddingLeft();
        int y = getPaddingTop();
        placeChild(mIcon, x, y);
        x += widthWithMargins(mIcon);
        placeChild(mText, x, y);
        y += heightWithMargins(mText);
        placeChild(mSub, x, y);
    }
}
