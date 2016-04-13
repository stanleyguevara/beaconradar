package net.beaconradar.history;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public interface RecyclerViewCursorIdClickListener {
    void onClick(int type, View view, int position, long id, RecyclerView.ViewHolder holder);
    void onLongClick(int type, View view, int position, long id, RecyclerView.ViewHolder holder);
}
