package net.beaconradar.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import net.beaconradar.R;
import net.beaconradar.utils.CircleImageView;

public class IconDialog extends AppCompatDialogFragment implements SingleChoiceListener {
    private IconChangedListener mListener;

    public interface IconChangedListener {
        void onIconChanged(int icon);
    }

    public static IconDialog newInstance(String title, int layout, int icon, @ColorInt int color) {
        IconDialog instance = new IconDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putInt("layout", layout);
        args.putInt("icon", icon);
        args.putInt("color", color);
        instance.setArguments(args);
        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setStyle(STYLE_NO_TITLE, 0);
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(getArguments().getInt("layout"), container);

        CircleImageView icon = (CircleImageView) layout.findViewById(R.id.beacon_icon);
        icon.setCircleColor(getArguments().getInt("color"));
        icon.setImageResource(getArguments().getInt("icon"));

        TextView title = (TextView) layout.findViewById(R.id.beacon_title);
        title.setText(getArguments().getString("title"));

        GridView grid = (GridView) layout.findViewById(R.id.grid);
        IconAdapter mAdapter = new IconAdapter(getContext(), getArguments().getInt("color"));
        mAdapter.setSelectionListener(this);
        grid.setAdapter(mAdapter);

        return layout;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(getArguments().getString("title"));
        return dialog;
    }

    public void setSelectionListener(IconChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void onSelection(int icon) {
        getDialog().dismiss();
        mListener.onIconChanged(icon);
    }

    @Nullable
    public static IconDialog findVisible(@NonNull AppCompatActivity context, String tag) {
        Fragment frag = context.getSupportFragmentManager().findFragmentByTag(tag);
        return (frag != null && frag instanceof IconDialog) ? (IconDialog) frag : null;
    }
}
