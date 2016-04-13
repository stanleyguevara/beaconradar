package net.beaconradar.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * TextView that breaks displayed text in half if measured text is too wide for one line.
 * If the text is too wide for two lines it behaves normally.
 * Accounts padding, reacts to onSizeChanged events.
 */
public class HalfBreakTextView extends TextView {

    public HalfBreakTextView(Context context) {
        super(context);
    }

    public HalfBreakTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HalfBreakTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HalfBreakTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Reacts to size changes breaking text if needed.
     * @param w {@inheritDoc}
     * @param h {@inheritDoc}
     * @param oldw {@inheritDoc}
     * @param oldh {@inheritDoc}
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setText(super.getText()); //This calls to setText(CharSequence text, BufferType type) internally.
    }

    /**
     * Measures text and splits it in two even (or almost even) parts if measured length of text
     * is greater than one width of this TextView and less than two widths (accounts padding).
     * @param text text to be broken.
     * @return text broken in half if there was no newline in input, otherwise unchanged input text.
     */
    private CharSequence getBrokenText(CharSequence text) {
        if(text.toString().contains("\n")) return text; //Already broken
        int available = getWidth() - getPaddingLeft() - getPaddingRight();
        int measured = (int) getPaint().measureText(text.toString());
        int len = text.length();
        if(len > 1 && measured > available && measured < 2*available) {
            int half = (len+1)/2;
            String str = text.toString();
            return str.substring(0, half)+"\n"+str.substring(half, len);
        } else {
            return text;
        }
    }

    /**
     * Filters input text to be broken in half if needed.
     * @param text Input text, if already contains newline this is same as superclass.
     * @param type {@inheritDoc}
     */
    @Override
    public void setText(CharSequence text, BufferType type) {
        CharSequence broken = getBrokenText(text);
        super.setText(broken, type);
    }

    /**
     * Prevents returning text with newline character
     * @return getText without \n
     */
    @Override
    public CharSequence getText() {
        return super.getText().toString().replace("\n", "");
    }
}
