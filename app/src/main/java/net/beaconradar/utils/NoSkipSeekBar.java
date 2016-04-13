package net.beaconradar.utils;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class NoSkipSeekBar extends AppCompatSeekBar {
    private boolean isDragging;

    public NoSkipSeekBar(Context context) {
        super(context);
    }

    public NoSkipSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoSkipSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private boolean isWithinThumb(MotionEvent event) {
        Rect bounds = getThumb().getBounds();
        bounds.top = bounds.top - 12;       //TODO improve
        bounds.bottom = bounds.bottom + 12;
        bounds.left = bounds.left - 12;
        bounds.right = bounds.right + 12;
        return bounds.contains((int)event.getX(), (int)event.getY());
    }

    private void increment(int direction) {
        if (direction != 0) {
            if(direction > 0) setProgress(getProgress() + 1);
            else setProgress(getProgress() - 1);
            /*final KeyEvent key = new KeyEvent(KeyEvent.ACTION_DOWN,
                    direction < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
            onKeyDown(key.getKeyCode(), key);*/
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {        //TODO bug somewhere here - when dragged outside material thumb stays big
        if (!isEnabled() || getThumb() == null) return super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isWithinThumb(event)) {
                    isDragging = true;
                    return super.onTouchEvent(event);
                } else {
                    isDragging = false;
                    return true;
                }

            case MotionEvent.ACTION_UP:
                isDragging = false;
                if (isWithinThumb(event)) {
                    return super.onTouchEvent(event);
                } else {
                    final Rect r = getThumb().getBounds();
                    increment((int)event.getX() - (r.left + r.right) / 2);
                    super.setPressed(false);
                    return true;
                }

            case MotionEvent.ACTION_MOVE:
                if (!isDragging) return true;
                break;

            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                super.setPressed(false);
                break;
        }

        return super.onTouchEvent(event);
    }
}