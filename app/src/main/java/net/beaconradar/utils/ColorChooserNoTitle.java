package net.beaconradar.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import net.beaconradar.R;

import java.lang.reflect.Field;

public class ColorChooserNoTitle extends ColorChooserDialog {
    private ColorChooserDialog.ColorCallback mSelectCallback;
    private DismissCallback mDismissCallback;

    private int beaconColor;
    private int beaconIcon;
    private String beaconTitle;

    public interface DismissCallback {
        public void onColorDismiss();
    }

    @NonNull @Override
    public Dialog onCreateDialog(Bundle inState) {
        Dialog dialog = super.onCreateDialog(inState);
        LinearLayout titleFrame = (LinearLayout) dialog.findViewById(R.id.titleFrame);
        //titleFrame.setVisibility(View.GONE);
        if(inState != null) {
            beaconTitle = inState.getString("beacon_title");
            beaconColor = inState.getInt("beacon_color");
            beaconIcon = inState.getInt("beacon_icon");
        }
        //Hide old title
        View oldIcon = dialog.findViewById(R.id.icon);
        View oldTitle = dialog.findViewById(R.id.title);
        oldIcon.setVisibility(View.GONE);
        oldTitle.setVisibility(View.GONE);

        //Inflate new
        View.inflate(getContext(), R.layout.beacon_identifier_small, titleFrame);
        CircleImageView icon = (CircleImageView) titleFrame.findViewById(R.id.beacon_icon);
        TextView text = (TextView) titleFrame.findViewById(R.id.beacon_title);
        titleFrame.setGravity(Gravity.CENTER);
        text.setText(beaconTitle);
        icon.setCircleColor(beaconColor);
        icon.setImageResource(beaconIcon);

        return dialog;
    }

    public void setBeacon(String title, int color, int icon) {
        beaconTitle = title;
        beaconColor = color;
        beaconIcon = icon;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("beacon_title", beaconTitle);
        outState.putInt("beacon_color", beaconColor);
        outState.putInt("beacon_icon", beaconIcon);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if(mDismissCallback != null) mDismissCallback.onColorDismiss();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setCallbackReflection(mSelectCallback);
    }

    public void setCallback(ColorChooserDialog.ColorCallback callback) {
        this.mSelectCallback = callback;
        setCallbackReflection(mSelectCallback);
    }

    public void setDismissCallback(DismissCallback callback) {
        this.mDismissCallback = callback;
    }

    private boolean setCallbackReflection(ColorChooserDialog.ColorCallback callback) {
        try {
            Field f = this.getClass().getSuperclass().getDeclaredField("mCallback");
            f.setAccessible(true);
            f.set(this, callback);
            return true;
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class Builder extends ColorChooserDialog.Builder {

        public <ActivityType extends AppCompatActivity & ColorCallback> Builder(@NonNull ActivityType context, int title) {
            super(context, title);
        }

        @NonNull @Override
        public ColorChooserNoTitle build() {
            ColorChooserNoTitle dialog = new ColorChooserNoTitle();
            Bundle args = new Bundle();
            args.putSerializable("builder", this);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull @Override
        public ColorChooserNoTitle show() {
            ColorChooserNoTitle dialog = this.build();
            dialog.show(this.mContext);
            return dialog;
        }
    }
}
