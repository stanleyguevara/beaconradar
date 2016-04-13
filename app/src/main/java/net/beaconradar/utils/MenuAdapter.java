package net.beaconradar.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.ArrayRes;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.beaconradar.R;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuHolder> {
    String[] mText;
    int[] mIcons;
    MenuClickListener mClickListener;
    int mSelection = NO_SELECTION;

    public static final int NO_SELECTION = -1;

    public interface MenuClickListener {
        void onMenuClick(View view, int position, MenuHolder holder);
    }

    public MenuAdapter(Context context, @ArrayRes int options, @ArrayRes int icons) {
        mText = context.getResources().getStringArray(options);
        TypedArray arr = context.getResources().obtainTypedArray(icons);
        mIcons = new int[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            mIcons[i] = arr.getResourceId(i, -1);
        }
        arr.recycle();
        if(mText.length != mIcons.length) {
            throw new IllegalArgumentException("Options length and icons length must be the same.");
        }
    }

    public MenuAdapter(Context context, String[] options, @DrawableRes int[] icons) {
        if(options.length != icons.length) {
            throw new IllegalArgumentException("Options length and icons length must be the same.");
        }
        mText = options;
        mIcons = icons;
    }

    public void setMenuClickListener(MenuClickListener listener) {
        this.mClickListener = listener;
    }

    public void setSelected(int selected) {
        if(selected != mSelection) {
            if(mSelection != NO_SELECTION) {
                //notifyItemChanged(mSelection);
            }
            //notifyItemChanged(selected);
            mSelection = selected;
        }
    }

    @Override
    public MenuHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        MenuHolder holder = new MenuHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(MenuHolder holder, int position) {
        holder.text.setText(mText[position]);
        holder.icon.setImageResource(mIcons[position]);

        if(position == mSelection) {
            holder.icon.setSelected(true);
            holder.itemView.setSelected(true);
        } else {
            holder.icon.setSelected(false);
            holder.itemView.setSelected(false);
        }
    }

    @Override
    public int getItemCount() {
        return mText.length;
    }

    public class MenuHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TintableImageView icon;
        public TextView text;
        public View clickable;

        public MenuHolder(View itemView) {
            super(itemView);
            icon = (TintableImageView) itemView.findViewById(R.id.icon);
            text = (TextView) itemView.findViewById(R.id.text);
            clickable = itemView.findViewById(R.id.clickable);
            clickable.setOnClickListener(MenuHolder.this);
        }

        @Override
        public void onClick(View view) {
            if(mClickListener != null) mClickListener.onMenuClick(view, MenuHolder.this.getLayoutPosition(), MenuHolder.this);
        }
    }
}
