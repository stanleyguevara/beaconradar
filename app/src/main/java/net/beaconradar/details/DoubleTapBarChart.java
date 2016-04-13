package net.beaconradar.details;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.github.mikephil.charting.charts.BarChart;

/**
 * Modifies BarChart behavior on double tap
 * (Zoom out full instaed of zooming in)
 */
public class DoubleTapBarChart extends BarChart {
    private GestureDetector detector;

    public DoubleTapBarChart(Context context) {
        super(context);
        detector = new GestureDetector(context, new GestureListener());
    }

    public DoubleTapBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        detector = new GestureDetector(context, new GestureListener());
    }

    public DoubleTapBarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        detector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = detector.onTouchEvent(event);
        if(result) return result;
        else return  super.onTouchEvent(event);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            while (!isFullyZoomedOut()) {
                zoom(0f, 0f, 0f, 0f);
            }
            return super.onDoubleTap(e);
        }
    }
}
