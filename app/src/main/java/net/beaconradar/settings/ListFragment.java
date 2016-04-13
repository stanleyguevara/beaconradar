package net.beaconradar.settings;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.ArrayRes;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import net.beaconradar.base.BaseDialogFragment;
import net.beaconradar.dagger.App;

import javax.inject.Inject;

import hugo.weaving.DebugLog;

//TODO this class should not implement SettingsView, nor extend BaseDialogFragment (too much unused interface methods, just contact mPresenter once)
public class ListFragment
        extends BaseDialogFragment<SettingsView, SettingsPresenter>
        implements SettingsView {

    private String mKey;
    private int[] mValues;
    private String[] mOptions;

    @Inject SettingsPresenterImpl mPresenter;

    public static ListFragment newInstance(String key, int selected, String title, @ArrayRes int values, @ArrayRes int options, int def) {
        Bundle args = new Bundle();
        args.putString("key", key);
        args.putInt("selected", selected);
        args.putString("title", title);
        args.putInt("values", values);
        args.putInt("options", options);
        args.putInt("default", def);

        ListFragment fragment = new ListFragment();
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
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        onViewCreated(null, savedInstanceState);        //Workaround for Mosby
        return null;
    }

    @Override @DebugLog
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mValues = getResources().getIntArray(getArguments().getInt("values"));
        mOptions = getResources().getStringArray(getArguments().getInt("options"));

        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .title(getArguments().getString("title"))
                .items(mOptions)
                .itemsCallbackSingleChoice(getArguments().getInt("selected"), new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                        mPresenter.storeListSelection(
                                getArguments().getString("key"),
                                mValues[i],
                                getArguments().getInt("defaults"));
                        return true;
                    }
                })
                .build();
        DisplayMetrics DM = getResources().getDisplayMetrics();
        int padBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, DM);
        TextView title = dialog.getTitleView();
        title.setPadding(title.getPaddingLeft(), title.getPaddingTop(), title.getPaddingRight(), padBottom);
        //TODO ROLL MY OWN TEXT STYLES
        title.setTextAppearance(getContext(), android.R.style.TextAppearance_Material_Title);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

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
