package net.beaconradar.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.support.v7.widget.AppCompatPopupWindow;
import android.util.AttributeSet;
import android.view.View;

import net.beaconradar.R;

public class CustomAppCompatPopupWindow extends AppCompatPopupWindow {
    //mOffsetX and mOffsetY are offsets to accommodate for shadow, which below lollipop is part of popup window.
    public int mOffsetX, mOffsetY;
    private Handler mHandler = new Handler();
    private static final int DISMISS_DELAY = 300;

    public CustomAppCompatPopupWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomAppCompatPopupWindow, defStyleAttr, 0);
        mOffsetX = a.getDimensionPixelOffset(R.styleable.CustomAppCompatPopupWindow_popupOffsetX, 0);
        mOffsetY = a.getDimensionPixelOffset(R.styleable.CustomAppCompatPopupWindow_popupOffsetY, 0);
        a.recycle();
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff) {
        super.showAsDropDown(anchor, xoff+mOffsetX, yoff+mOffsetY);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        super.showAsDropDown(anchor, xoff+mOffsetX, yoff+mOffsetY, gravity);
    }

    @Override
    public void update(View anchor, int xoff, int yoff, int width, int height) {
        super.update(anchor, xoff+mOffsetX, yoff+mOffsetY, width, height);
    }

    public void dismissDelayed() {
        dismissDelayed(DISMISS_DELAY);
    }

    public void dismissDelayed(int delay) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                CustomAppCompatPopupWindow.super.dismiss();
            }
        }, delay);
    }
}
