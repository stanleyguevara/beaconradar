package net.beaconradar.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import net.beaconradar.utils.BaseLayout;
import net.beaconradar.utils.TintableImageView;

public class IconCheckboxLayout extends BaseLayout {
    private TintableImageView mIcon;
    private TextView mText, mSub;
    private CheckBox mBox;

    public IconCheckboxLayout(Context context) {
        super(context);
    }

    public IconCheckboxLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IconCheckboxLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public IconCheckboxLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    //Factory-inflating method. Pass the parent and layout, get back inflated BeaconLayout in place.
    public static IconCheckboxLayout inflateLayoutInto(int layout, ViewGroup root) {
        LayoutInflater inflater = LayoutInflater.from(root.getContext());
        IconCheckboxLayout inflated = (IconCheckboxLayout) inflater.inflate(layout, root, false);
        return inflated;
    }

    @Override
    public void initViews() {
        mIcon = (TintableImageView) getChildAt(0);
        mText = (TextView) getChildAt(1);
        mSub = (TextView) getChildAt(2);
        mBox = (CheckBox) getChildAt(3);
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
        measureChildWithMargins(mBox, wSpec, wUsed, hSpecIcon, hUsed);
        wUsed += widthWithMargins(mBox);
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
        //x += widthWithMargins(mText);
        placeChild(mBox, x + widthWithMargins(mText), y);
        y += heightWithMargins(mText);
        placeChild(mSub, x, y);
    }
}
