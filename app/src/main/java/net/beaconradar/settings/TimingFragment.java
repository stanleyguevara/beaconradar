package net.beaconradar.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import net.beaconradar.R;
import net.beaconradar.base.BaseDialogFragment;
import net.beaconradar.dagger.App;
import net.beaconradar.utils.NoSkipSeekBar;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

public class TimingFragment
        extends BaseDialogFragment<SettingsView, SettingsPresenter>
        implements SettingsView {

    @Bind(R.id.dialogTitle) TextView mDialogTitle;
    @Bind(R.id.seekDuration) NoSkipSeekBar mSeekDuration;
    @Bind(R.id.seekInterval) NoSkipSeekBar mSeekInterval;
    @Bind(R.id.seekRemove) NoSkipSeekBar mSeekRemove;
    @Bind(R.id.seekSplit) NoSkipSeekBar mSeekSplit;
    @Bind(R.id.textDuration) TextView mTextDuration;
    @Bind(R.id.textInterval) TextView mTextInterval;
    @Bind(R.id.textRemove) TextView mTextRemove;
    @Bind(R.id.textSplit) TextView mTextSplit;
    private int mIntervalProgress;
    private int mDurationProgress;
    private int mRemoveProgress;
    private int mSplitProgress;
    private long mDurationTime;
    private long mIntervalTime;
    private long mRemoveTime;
    private long mSplitTo;
    private int mIntervalColorOriginal, mDurationColorOriginal, mRemoveColorOriginal, mSplitColorOriginal, mColorWarning;

    private int mMode;
    private String mKey;

    @Inject
    SettingsPresenterImpl mPresenter;

    public static TimingFragment newInstance(String key, int mode,
                                             long interval, long duration, long remove, long split,
                                             int intervalProgress, int durationProgress, int removeProgress, int splitProgress) {
        Bundle args = new Bundle();
        args.putString("key", key);
        args.putInt("scan_mode", mode);
        args.putLong("interval", interval);
        args.putLong("duration", duration);
        args.putLong("remove", remove);
        args.putLong("split", split);
        args.putInt("interval_progress", intervalProgress);
        args.putInt("duration_progress", durationProgress);
        args.putInt("remove_progress", removeProgress);
        args.putInt("split_progress", splitProgress);

        TimingFragment fragment = new TimingFragment();
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
        //AppCompatDialog dialog = (AppCompatDialog) super.onCreateDialog(savedInstanceState);
        //dialog.setTitle("WTF");

        View layout = View.inflate(getActivity(), R.layout.preference_doubleseekbar, null);
        ButterKnife.bind(this, layout);
        init();

        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .customView(layout, true)
                .positiveText("ACCEPT")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                        mPresenter.storeIntervalDuration(mMode, mIntervalTime, mDurationTime, mRemoveTime, mSplitTo);
                    }
                })
                .negativeText("CANCEL")
                .build();

        return dialog;

        /*AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppCompatAlertDialogStyle);
        builder.setTitle("AppCompatDialog");
        builder.setCancelable(true);
        builder.setMessage("Lorem ipsum dolor...");
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", null);
        //setStyle(STYLE_NORMAL, R.style.AppCompatAlertDialogStyle);
        AppCompatDialog dialog = builder.show();
        //dialog.requestWindowFeature(STYLE_NORMAL);*/

        /*AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppCompatAlertDialogStyle);
        builder.setTitle("AppCompatDialog");
        builder.setMessage("Lorem ipsum dolor...");
        builder.setPositiveButton("OK",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //((FragmentAlertDialog)getActivity()).doPositiveClick();
                }
            }
        );
        builder.setNegativeButton("CANCEL",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //((FragmentAlertDialog)getActivity()).doNegativeClick();
                }
            }
        );
        AlertDialog dialog = builder.show();
        dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
        dialog.getWindow().setTitle("Some title");
        dialog.getWindow().requestFeature(STYLE_NORMAL);*/
        //return dialog;
        //return dialog;
    }

    private void init() {
        mColorWarning = ContextCompat.getColor(getActivity(), R.color.warningText);

        mKey = getArguments().getString("key");
        mMode = getArguments().getInt("scan_mode");
        mIntervalTime = getArguments().getLong("interval");
        mDurationTime = getArguments().getLong("duration");
        mRemoveTime = getArguments().getLong("remove");
        mSplitTo = getArguments().getLong("split");
        mIntervalProgress = getArguments().getInt("interval_progress");
        mDurationProgress = getArguments().getInt("duration_progress");
        mRemoveProgress = getArguments().getInt("remove_progress");
        mSplitProgress = getArguments().getInt("split_progress");

        mDialogTitle.setText(StringUtils.capitalize(mKey.toLowerCase())+" scan timing");

        //Interval Views setup
        mIntervalColorOriginal = mTextInterval.getCurrentTextColor();
        mTextInterval.setText("Interval: " + mPresenter.getInterval(mMode, mIntervalProgress));
        mSeekInterval.setProgress(mIntervalProgress);
        mSeekInterval.setMax(mPresenter.getIntervalsCount(mMode) - 1);
        mSeekInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mIntervalTime = mPresenter.getIntervalLong(mMode, progress);
                mTextInterval.setText("Interval: " + mPresenter.getInterval(mMode, progress));
                mIntervalProgress = progress;
                showWarning(mTextInterval, mIntervalColorOriginal, !mPresenter.verifyInterval(mMode, mIntervalTime));
                showWarning(mTextRemove, mRemoveColorOriginal, !mPresenter.verifyRemove(mMode, mRemoveTime, mIntervalTime, mDurationTime));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        showWarning(mTextInterval, mIntervalColorOriginal, !mPresenter.verifyInterval(mMode, mIntervalTime));

        //Duration Views setup
        mDurationColorOriginal = mTextDuration.getCurrentTextColor();
        mTextDuration.setText("Duration: " + mDurationTime + " ms");
        mSeekDuration.setProgress(mDurationProgress);
        mSeekDuration.setMax(mPresenter.getDurationsCount(mMode) - 1);
        mSeekDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mDurationTime = (progress * 100) + 100;
                mTextDuration.setText("Duration: " + mDurationTime + " ms");
                mDurationProgress = progress;
                showWarning(mTextDuration, mDurationColorOriginal, !mPresenter.verifyDuration(mMode, mDurationTime, mSplitTo));
                showWarning(mTextRemove, mRemoveColorOriginal, !mPresenter.verifyRemove(mMode, mRemoveTime, mIntervalTime, mDurationTime));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        showWarning(mTextDuration, mDurationColorOriginal, !mPresenter.verifyDuration(mMode, mDurationTime, mSplitTo));

        //Remove Views setup
        mRemoveColorOriginal = mTextRemove.getCurrentTextColor();
        mTextRemove.setText("Remove: " + mPresenter.getRemove(mMode, mRemoveProgress));
        mSeekRemove.setProgress(mRemoveProgress);
        mSeekRemove.setMax(mPresenter.getRemovesCount(mMode) - 1);
        mSeekRemove.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mRemoveTime = mPresenter.getRemoveLong(mMode, progress);
                mTextRemove.setText("Remove: " + mPresenter.getRemove(mMode, progress));
                mRemoveProgress = progress;
                showWarning(mTextRemove, mRemoveColorOriginal, !mPresenter.verifyRemove(mMode, mRemoveTime, mIntervalTime, mDurationTime));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        showWarning(mTextRemove, mRemoveColorOriginal, !mPresenter.verifyRemove(mMode, mRemoveTime, mIntervalTime, mDurationTime));

        //Split Views setup
        mSplitColorOriginal = mTextSplit.getCurrentTextColor();
        mTextSplit.setText("Split duration to "+ mSplitTo +" scans");
        mSeekSplit.setProgress(mSplitProgress);
        mSeekSplit.setMax(mPresenter.getSplitsCount(mMode) - 1);
        mSeekSplit.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSplitTo = progress + 1;
                mTextSplit.setText("Split duration to "+ mSplitTo +" scans");
                mSplitProgress = progress;
                showWarning(mTextSplit, mSplitColorOriginal, !mPresenter.verifySplit(mMode, mDurationTime, mSplitTo));
                showWarning(mTextDuration, mDurationColorOriginal, !mPresenter.verifyDuration(mMode, mDurationTime, mSplitTo));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        showWarning(mTextSplit, mSplitColorOriginal, !mPresenter.verifySplit(mMode, mDurationTime, mSplitTo));
    }

    private void showWarning(TextView textView, int originalColor, boolean warn) {
        if(warn) textView.setTextColor(mColorWarning);
        else textView.setTextColor(originalColor);
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
