package net.beaconradar.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CircleImageView extends ImageView {
    Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CircleImageView(Context context) {
        super(context);
        init();
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setCircleBorder(float width) {
        mPaint.setStrokeWidth(width);
        if(width == 0) mPaint.setStyle(Paint.Style.FILL);
        else mPaint.setStyle(Paint.Style.STROKE);
    }

    public void setCircleColor(@ColorInt int color) {
        mPaint.setColor(color);
        invalidate();
    }

    private void init() {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        float radius_width = (getWidth()/* - getPaddingStart() - getPaddingEnd()*/) * 0.5f;
        float radius_height = (getHeight()/* - getPaddingTop() - getPaddingBottom()*/) * 0.5f;
        canvas.drawCircle(
                radius_width /*+ getPaddingStart()*/,
                radius_height /*+ getPaddingTop()*/,
                Math.min(radius_height, radius_width) - (mPaint.getStrokeWidth() * 0.5f),
                mPaint);
        super.onDraw(canvas);
    }
}
