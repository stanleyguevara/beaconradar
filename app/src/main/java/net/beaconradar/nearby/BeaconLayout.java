package net.beaconradar.nearby;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.beaconradar.utils.CircleImageView;


//import de.hdodenhof.circleimageview.CircleImageView;

public class BeaconLayout extends ViewGroup {
    private String TAG = getClass().getName();
    private CircleImageView mProfilePhoto;
    private View mDistanceIndicator, mSignalIndicator;
    private TextView mTitle, mSubtitle, mDistanceText, mSignalText;

    //Factory-inflating method. Pass the parent and layout, get back inflated BeaconLayout in place.
    public static BeaconLayout inflateInto(int layout, ViewGroup root) {
        LayoutInflater inflater = LayoutInflater.from(root.getContext());
        BeaconLayout inflatedView = (BeaconLayout) inflater.inflate(layout, root, false);
        //Log.v("ViewHolder", "InflateInto");
        //root.addView(inflatedView);   //TODO just deleted it. Looks like wasn't nescessary.
        return inflatedView;
    }

    public BeaconLayout(Context context) {
        super(context);
    }

    public BeaconLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BeaconLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //Assign member views when inflated from xml.
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //Log.v("ViewHolder", "Here 1");
        initViews();
    }

    public void initViews() {
        mProfilePhoto = (CircleImageView) getChildAt(0);
        mTitle = (TextView) getChildAt(1);
        mSubtitle = (TextView) getChildAt(2);
        mDistanceIndicator = getChildAt(3);
        mSignalIndicator = getChildAt(4);
        mDistanceText = (TextView) getChildAt(5);
        mSignalText = (TextView) getChildAt(6);
    }

    public View getDistanceIndicator() {
        return mDistanceIndicator;
    }

    public View getSignalIndicator() {
        return mSignalIndicator;
    }

    public TextView getTitle() {
        return mTitle;
    }

    public TextView getSubtitle() {
        return mSubtitle;
    }

    public CircleImageView getIcon() {
        return mProfilePhoto;
    }

    public TextView getMetersTxt() {
        return mDistanceText;
    }

    public TextView getSignalTxt() {
        return mSignalText;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Log.v("ViewHolder", "onMeasure");
        int widthUsed = 0;
        int heightUsed = 0;

        measureChildWithMargins(mProfilePhoto, widthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed);
        widthUsed += widthWithMargins(mProfilePhoto);

        measureChildWithMargins(mDistanceIndicator, widthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed);
        measureChildWithMargins(mSignalIndicator, widthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed);
        measureChildWithMargins(mDistanceText, widthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed);
        measureChildWithMargins(mSignalText, widthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed);
        widthUsed += widthWithMargins(mDistanceIndicator);

        //Log.v(TAG, "widthUsed: " + widthUsed + " spec: " + MeasureSpec.getSize(widthMeasureSpec) + " lp.width: " + mTitle.getLayoutParams().width);
        measureChildWithMargins(mTitle, widthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed);

        heightUsed += heightWithMargins(mTitle);

        measureChildWithMargins(mSubtitle, widthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed);
        heightUsed += heightWithMargins(mSubtitle);
        widthUsed += Math.max(widthWithMargins(mTitle), widthWithMargins(mSubtitle));

        // handle the case where the image is taller than the texts combined
        heightUsed = Math.max(heightWithMargins(mProfilePhoto), heightUsed);

        widthUsed += getPaddingLeft() + getPaddingRight();
        heightUsed += getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(
                resolveSize(widthUsed, widthMeasureSpec),
                resolveSize(heightUsed, heightMeasureSpec));
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
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                lp.topMargin + lp.bottomMargin + heightUsed,
                lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }


    private int widthWithMargins(View child) {
        MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
        return child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
    }

    private int heightWithMargins(View child) {
        MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
        return child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        //Log.v("ViewHolder", "onLayout");
        //Log.v("BeaconLayout", "changed: "+changed+" left: "+left+" top: "+top+" right: "+right+" bottom: "+bottom);
        int x = getPaddingLeft();
        int y = getPaddingTop();
        int innerHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        placeChild(mProfilePhoto, x, y);
        x += widthWithMargins(mProfilePhoto);

        placeChild(mDistanceIndicator, right - widthWithMargins(mDistanceIndicator), y);
        placeChild(mSignalIndicator, right - widthWithMargins(mSignalIndicator), y + heightWithMargins(mDistanceIndicator));
        placeChild(mDistanceText, right - widthWithMargins(mDistanceText), y);
        placeChild(mSignalText, right - widthWithMargins(mSignalText), y + heightWithMargins(mDistanceIndicator));

        placeChild(mTitle, x, y);
        y += heightWithMargins(mTitle);
        placeChild(mSubtitle, x, y);
    }

    private void placeChild(View child, int left, int top) {
        MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
        child.layout(
                left + lp.leftMargin,
                top + lp.topMargin,
                left + lp.leftMargin + child.getMeasuredWidth(),
                top + lp.topMargin + child.getMeasuredHeight());
    }

    @Override
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
    }
}
