package net.beaconradar.utils;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * FrameLayout for displaying simple name-value pairs.
 * Has to have exactly two TextView children, first being name, second value.
 * First view hast to have WRAP_CONTENT width, second can have any.
 * Also sets default onClickListener which copies value to clipboard
 * and shows Toast to notify the user.
 */
public class ParamCheckboxLayout extends FrameLayout {
    public TextView name;
    public SwitchCompat check;
    private String mClipData, mClipName;
    private ParamLayout.ToastListener mToastListener;

    public ParamCheckboxLayout(Context context) {
        super(context);
        init();
    }

    public ParamCheckboxLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ParamCheckboxLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ParamCheckboxLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                copyClipboard(name.getText().toString()+" "+mClipName, mClipData);
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        name = (TextView) getChildAt(0);
        check = (SwitchCompat) getChildAt(1);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int x = getPaddingLeft();
        int y = getPaddingTop();
        placeChild(name, x, y);
        x += widthWithMargins(name);
        placeChild(check, x, y);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthUsed = 0;
        int heightUsed = 0;

        measureChildWithMargins(check, widthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed);
        widthUsed += widthWithMargins(check);
        measureChildWithMargins(name, widthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed);
        widthUsed += widthWithMargins(name);
        heightUsed += Math.max(heightWithMargins(name), heightWithMargins(check));
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

    private void placeChild(View child, int left, int top) {
        MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
        child.layout(
                left + lp.leftMargin,
                top + lp.topMargin,
                left + lp.leftMargin + child.getMeasuredWidth(),
                top + lp.topMargin + child.getMeasuredHeight());
    }

    public void setClipboardData(String name, String data) {
        this.mClipName = name;
        this.mClipData = data;
    }

    public void setToastListener(ParamLayout.ToastListener listener) {
        this.mToastListener = listener;
    }

    public void copyClipboard(String label, String data) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, data);
        clipboard.setPrimaryClip(clip);
        showToast(label + " in clipboard");
    }

    private void showToast(String text) {
        if(mToastListener != null) {
            mToastListener.showToast(text);
        } else {
            Toast toast = Toast.makeText(getContext(), text, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }
}
