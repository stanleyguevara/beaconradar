package net.beaconradar.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import net.beaconradar.R;
import net.beaconradar.base.BaseDialogFragment;
import net.beaconradar.dagger.App;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

//TODO this class should not implement SettingsView, nor extend BaseDialogFragment (too much unused interface methods, just contact mPresenter once)
public class IntegerFragment
        extends BaseDialogFragment<SettingsView, SettingsPresenter>
        implements SettingsView {

    @Bind(R.id.picker) NumberPicker mPicker;
    @Bind(R.id.title) TextView mTitle;

    @Inject SettingsPresenterImpl mPresenter;

    public static IntegerFragment newInstance(String key, String title, int current, int min, int max) {
        Bundle args = new Bundle();
        args.putString("key", key);
        args.putString("title", title);
        args.putInt("current", current);
        args.putInt("min", min);
        args.putInt("max", max);

        IntegerFragment fragment = new IntegerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public SettingsPresenter createPresenter() {
        return mPresenter;
    }

    @Override
    protected void injectDependencies(App.AppComponent component) {
        component.inject(this);
    }

    @Nullable
    @Override @DebugLog
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
        super.onCreateView(inflater, container, inState);
        onViewCreated(null, inState);        //Workaround for Mosby
        return null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("CURR_VAL", mPicker.getValue());
    }

    @Override @DebugLog
    public Dialog onCreateDialog(Bundle inState) {
        View layout = View.inflate(getActivity(), R.layout.integer_picker, null);
        ButterKnife.bind(this, layout);

        int value;
        if(inState != null) value = inState.getInt("CURR_VAL");
        else value = getArguments().getInt("current");
        mPicker.setMinValue(getArguments().getInt("min"));
        mPicker.setMaxValue(getArguments().getInt("max"));
        mPicker.setValue(value);
        mTitle.setText(getArguments().getString("title"));

        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .customView(layout, true)
                .positiveText("ACCEPT")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                        mPresenter.setCleanupDays(mPicker.getValue());
                    }
                })
                .negativeText("CANCEL")
                .build();

        return dialog;
    }

    @Override
    public String getTitle() {
        return "Unused";
    }

    @Override
    public void setSummary(int mode, String summary) {
        //Unused
    }

    @Override
    public void showWarning(int key, String message, int duration) {
        //Unused
    }

    @Override
    public void dismissWarning(int mode) {
        //Unused
    }

    @Override
    public void showIntervalWarning(int mode, String message) {
        //Log.v(TAG, "-------WARNING----- "+message);
    }

    @Override
    public void showDurationWarning(int mode, String message) {
        //Log.v(TAG, "-------WARNING----- "+message);
    }

    @Override
    public void showRemoveWarning(int mode, String message) {
        //Log.v(TAG, "-------WARNING----- "+message);
    }

    @Override
    public void showSplitWarning(int mode, String message) {
        //Unused
    }
}
