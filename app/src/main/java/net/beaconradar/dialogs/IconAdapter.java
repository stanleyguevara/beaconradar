package net.beaconradar.dialogs;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.ColorInt;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import net.beaconradar.R;
import net.beaconradar.utils.IconsPalette;

import java.util.ArrayList;

import javax.inject.Inject;

public class IconAdapter extends BaseAdapter implements View.OnClickListener {
    private final Context context;
    private int mBackgroundId;
    private SingleChoiceListener listener;
    private final int color;
    private final int size;
    private final int padding;
    private ArrayList<Integer> mIcons;
    @Inject IconsPalette mPalette;

    public IconAdapter(Context context, @ColorInt int color) {
        this.context = context;
        this.color = color;
        //noinspection AccessStaticViaInstance
        this.mIcons = new ArrayList<>(mPalette.map.values());

        /*TypedArray ta = context.obtainStyledAttributes(new int[]{R.attr.selectableItemBackground});
        mBackground = ta.getDrawable(0);
        ta.recycle();*/

        TypedValue typedVal = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, typedVal, true);
        mBackgroundId = typedVal.resourceId;

        DisplayMetrics DM = context.getResources().getDisplayMetrics();
        size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, DM);
        padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, DM);
    }

    public void setSelectionListener(SingleChoiceListener listener) {
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return mIcons.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView image;
        if(convertView == null) {
            image = new ImageView(context);
            GridView.LayoutParams lp = new GridView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    size);
            image.setLayoutParams(lp);
            image.setPadding(padding, padding, padding, padding);   //PUDDI PUDDI, GIGA PUDDI!
            image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            image.setOnClickListener(this);
            image.setTag(new ViewHolder(mIcons.get(position)));
            image.setColorFilter(R.color.graphite, PorterDuff.Mode.SRC_IN);
            image.setBackgroundResource(mBackgroundId);
        } else {
            image = (ImageView) convertView;
            ((ViewHolder) image.getTag()).icon = mIcons.get(position);
        }
        image.setImageResource(mIcons.get(position));
        image.getDrawable().mutate();
        return image;
    }

    @Override
    public void onClick(View v) {
        listener.onSelection(((ViewHolder) v.getTag()).icon);
    }

    private static class ViewHolder {
        int icon;
        ViewHolder(int icon) {
            this.icon = icon;
        }
    }
}