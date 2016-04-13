package net.beaconradar.dialogs;

import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.beaconradar.R;
import net.beaconradar.utils.CircleImageView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OptionsDialog extends AppCompatDialogFragment {
    private ColorFilter mFilterAccent;
    @Bind(R.id.context_first) TextView first;
    @Bind(R.id.context_second) TextView second;
    @Bind(R.id.context_third) TextView third;
    @Bind(R.id.beacon_title) TextView mTitle;
    @Bind(R.id.beacon_icon) CircleImageView mIcon;
    private SingleChoiceListener mListener;



    public static OptionsDialog newInstance(String title, int[] options, int[] icons, int color, int icon) {
        OptionsDialog instance = new OptionsDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putIntArray("options", options);
        args.putIntArray("icons", icons);
        args.putInt("color", color);
        args.putInt("icon", icon);
        instance.setArguments(args);
        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setStyle(STYLE_NO_TITLE, 0);
        mFilterAccent = new PorterDuffColorFilter(ContextCompat.getColor(getContext(), R.color.accent), PorterDuff.Mode.SRC_IN);
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.beacon_context, container);
        ButterKnife.bind(this, layout);

        mTitle.setText(getArguments().getString("title"));
        mIcon.setCircleColor(getArguments().getInt("color"));
        mIcon.setImageResource(getArguments().getInt("icon"));

        //Icons have to be built from code due to
        //https://code.google.com/p/android/issues/detail?id=191111
        //TODO When issue is fixed just tint icons here, create them in XML for consistency.
        Drawable icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_pencil);
        icon.mutate();
        icon.setColorFilter(mFilterAccent);
        first.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);

        icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_invert_colors);
        icon.mutate();
        icon.setColorFilter(mFilterAccent);
        second.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);

        icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_image);
        icon.mutate();
        icon.setColorFilter(mFilterAccent);
        third.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);

        return layout;
    }

    /*@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        return dialog;
    }*/

    public void setSelectionListener(SingleChoiceListener listener) {
        mListener = listener;
    }

    @OnClick(R.id.context_first)
    public void selectFirst() {
        getDialog().dismiss();
        mListener.onSelection(0);
    }

    @OnClick(R.id.context_second)
    public void selectSecond() {
        getDialog().dismiss();
        mListener.onSelection(1);
    }

    @OnClick(R.id.context_third)
    public void selectThird() {
        getDialog().dismiss();
        mListener.onSelection(2);
    }

    @Nullable
    public static OptionsDialog findVisible(@NonNull AppCompatActivity context, String tag) {
        Fragment frag = context.getSupportFragmentManager().findFragmentByTag(tag);
        return (frag != null && frag instanceof OptionsDialog) ? (OptionsDialog) frag : null;
    }
}
