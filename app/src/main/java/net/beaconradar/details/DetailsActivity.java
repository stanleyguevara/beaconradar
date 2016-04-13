package net.beaconradar.details;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.hannesdorfmann.mosby.mvp.viewstate.ViewState;
import net.beaconradar.R;
import net.beaconradar.base.BaseMvpViewStateActivity;
import net.beaconradar.dagger.App;
import net.beaconradar.dialogs.IconDialog;
import net.beaconradar.dialogs.TextDialog;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;
import net.beaconradar.service.id.altbeacon.ALT;
import net.beaconradar.service.id.eddystone.EDD;
import net.beaconradar.service.id.eddystone.TLM;
import net.beaconradar.service.id.eddystone.UID;
import net.beaconradar.service.id.eddystone.URL;
import net.beaconradar.service.id.ibeacon.IBC;
import net.beaconradar.utils.CircleImageView;
import net.beaconradar.utils.ColorChooserNoTitle;
import net.beaconradar.utils.Const;
import net.beaconradar.utils.ParamCheckboxLayout;
import net.beaconradar.utils.ParamLayout;
import net.beaconradar.fab.ProgressFAB;
import net.beaconradar.utils.TimeFormat;
import net.beaconradar.utils.TintableImageView;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hugo.weaving.DebugLog;

//TODO this grew quite big. Could split it (chart/fields maybe)
public class DetailsActivity
        extends BaseMvpViewStateActivity<DetailsView, DetailsPresenter>
        implements DetailsView, ParamLayout.ToastListener,
        TextDialog.NameChangedListener, IconDialog.IconChangedListener, ColorChooserNoTitle.ColorCallback {

    @Inject DetailsPresenterImpl mPresenter;

    //General Views
    @Bind(R.id.chart) BarChart mChart;
    @Bind(R.id.fab) ProgressFAB mFab;
    @Bind(R.id.coordinator) CoordinatorLayout mCoordinator;
    @Bind(R.id.container) NestedScrollView mContainer;
    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(R.id.footer) View mFooter;

    //Param Views
    @Bind(R.id.param_container) LinearLayout mParamContainer;
    @Bind(R.id.tlm_param_container) LinearLayout mContainerTLM;
    @Bind(R.id.header_tlm_text) TextView mHeaderTextTLM;
    @Bind(R.id.big_value) TextView mBig;

    //IBC
    private ParamLayout mParamMajorMinor;
    private ParamLayout mParamMinor;
    private ParamLayout mParamUUID;
    //URL
    private ParamLayout mParamLink;
    //UID
    private ParamLayout mParamNamespace;
    private ParamLayout mParamInstance;
    //TLM
    private ParamLayout mParamBatt;
    private ParamLayout mParamTemp;
    private ParamLayout mParamCPDU;
    private ParamLayout mParamCUPT;
    //Common
    private ParamLayout mParamTx;
    private ParamLayout mParamMAC;

    //DB only sourced params
    @Bind(R.id.param_beacon_icon_value)
    TintableImageView mParamIconValue;
    @Bind(R.id.param_beacon_name_value) TextView mParamNameText;
    @Bind(R.id.param_beacon_color_value) View mParamColorValue;

    //Header
    @Bind(R.id.beacon_name) TextView mNameText;
    @Bind(R.id.beacon_subtext) TextView mSeenText;
    @Bind(R.id.beacon_icon)
    CircleImageView mIcon;

    //Broadcast intent Views
    @Bind(R.id.header_intent) FrameLayout mHeaderIntent;
    @Bind(R.id.param_beacon_appeared)
    ParamCheckboxLayout mParamAppeared;
    @Bind(R.id.param_beacon_appeared_name) TextView mParamAppearedText;
    @Bind(R.id.param_beacon_appeared_value) SwitchCompat mParamAppearedSwitch;
    @Bind(R.id.param_beacon_visible) ParamCheckboxLayout mParamVisible;
    @Bind(R.id.param_beacon_visible_name) TextView mParamVisibleText;
    @Bind(R.id.param_beacon_visible_value) SwitchCompat mParamVisibleSwitch;
    @Bind(R.id.param_beacon_disappeared) ParamCheckboxLayout mParamDisappeared;
    @Bind(R.id.param_beacon_disappeared_name) TextView mParamDisappearedText;
    @Bind(R.id.param_beacon_disappeared_value) SwitchCompat mParamDisappearedSwitch;

    private ID mID;             //This should act only as a key
    private DetailsData mData;

    private Runnable mTickRunnable;
    private final Handler mTickHandler = new Handler();

    @Override
    protected void injectDependencies(App.AppComponent component) {
        component.inject(this);
    }

    //-----------------AS CALLED-----------------

    @Override @DebugLog
    protected void onCreate(Bundle savedInstanceState) {
        mID = getIntent().getParcelableExtra("identifier");
        super.onCreate(savedInstanceState);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override @NonNull @DebugLog
    public DetailsPresenter createPresenter() {
        return mPresenter;
    }

    @Override @DebugLog
    public ID getIdentificator() {
        return mID;
    }

    @Override @DebugLog
    public void inflateUI() {
        setContentView(R.layout.activity_details);
        ButterKnife.bind(this);
        mFab.setCoordinator(mCoordinator);
        mFooter.setVisibility(mFab.needsFooter() ? View.VISIBLE : View.GONE);
        inflateParams(mID);
        initChart(mChart);
    }

    /**
     * Procedure for initial filling non-live params from DB
     * @param beacon
     */
    @DebugLog
    public void updateBeaconFixed(@Nullable Beacon beacon) {
        //Always present, strictly from DB.
        if(beacon == null) return;
        mParamColorValue.setBackgroundColor(beacon.getColor());
        mIcon.setCircleColor(beacon.getColor());
        mIcon.setImageResource(beacon.getIcon());
        mParamNameText.setText(beacon.getName());
        mNameText.setText(beacon.getName());
        mParamIconValue.setImageResource(beacon.getIcon());

        mParamAppearedSwitch.setChecked(beacon.getOnAppeared());
        mParamAppearedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                mPresenter.setOnAppeared(checked);
                setResult(1);
            }
        });
        mParamVisibleSwitch.setChecked(beacon.getOnVisible());
        mParamVisibleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                mPresenter.setOnVisible(checked);
                setResult(1);
            }
        });
        mParamDisappearedSwitch.setChecked(beacon.getOnDisappeared());
        mParamDisappearedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                mPresenter.setOnDisappeared(checked);
                setResult(1);
            }
        });
    }

    @Override @DebugLog
    public void updateBeaconDynamic(@Nullable Beacon beacon) {
        //Fields
        if(beacon != null) {
            ID id = beacon.getId();
            switch (id.getType()) {
                case ID.T_IBC:
                    updateIBC((IBC) id);
                    break;
                case ID.T_UID:
                    UID uid = (UID) id;
                    updateUID(uid);
                    if(uid.beaconTLM != null) {
                        updateTLM((TLM) uid.beaconTLM.getId());
                    }
                    break;
                case ID.T_URL:
                    URL url = (URL) id;
                    updateURL(url);
                    if(url.beaconTLM != null) {
                        updateTLM((TLM) url.beaconTLM.getId());
                    }
                    break;
                case ID.T_TLM:
                    updateTLM((TLM) id);
                    break;
                case ID.T_ALT:
                    updateALT((ALT) id);
                    break;
                case ID.T_EDD:
                    EDD edd = (EDD) id;
                    updateEDD(edd);
                    if(edd.frameTLM != null) {
                        updateTLM((TLM) edd.frameTLM);
                    }
                    break;
            }
            updateCommon(beacon);
        }
    }

    @Override
    public void updateBeaconTick(@Nullable Beacon beacon) {
        if(beacon != null) {
            updateSecondCounter(beacon);
            if(beacon.live) {
                String currVal;
                if(mPresenter.getMode() == MODE_DISTANCE) {
                    currVal = String.format("%.2f", beacon.getDist());
                } else {
                    currVal = String.valueOf(beacon.getRssi());
                }
                mBig.setText(currVal);
                if(beacon.isHot()) {
                    mBig.setTextColor(ContextCompat.getColor(this, R.color.white));
                } else {
                    mBig.setTextColor(ContextCompat.getColor(this, R.color.gray600));
                }
            } else {
                mBig.setTextColor(ContextCompat.getColor(this, R.color.gray600));
                mBig.setText("N/A");
            }
        } else {
            mBig.setTextColor(ContextCompat.getColor(this, R.color.gray600));
            mBig.setText("N/A");
        }
    }

    private void updateSecondCounter(@Nullable Beacon beacon) {
        if(beacon != null) {
            long seen = beacon.getLastSeen();
            long now = System.currentTimeMillis();
            mSeenText.setText(TimeFormat.getTimeAgo(seen, now - seen));
            int type = beacon.getId().getType();
            if(type == ID.T_UID) {
                UID uid = (UID) beacon.getId();
                Beacon tlm = uid.beaconTLM;
                if(tlm != null) {
                    mHeaderTextTLM.setText(
                            "TLM "+tlm.getId().getMac()+" | "+
                            "Seen "+TimeFormat.getTimeAgoShort(
                                    tlm.getLastSeen(),
                                    now - tlm.getLastSeen()));
                }
            } else if(type == ID.T_URL) {
                URL url = (URL) beacon.getId();
                Beacon tlm = url.beaconTLM;
                if(tlm != null) {
                    mHeaderTextTLM.setText(
                            "TLM "+tlm.getId().getMac()+" | "+
                            "Seen "+TimeFormat.getTimeAgoShort(
                                    tlm.getLastSeen(),
                                    now - tlm.getLastSeen()));
                }
            } else if(type == ID.T_EDD) {
                EDD edd = (EDD) beacon.getId();
                if(edd.frameTLM != null) {
                    TLM tlm = (TLM) edd.frameTLM;
                    mHeaderTextTLM.setText(
                            "TLM Seen "+TimeFormat.getTimeAgoShort(
                                    tlm.getSpotted(),
                                    now - tlm.getSpotted()));
                }
            }
        }
    }

    @Override @DebugLog
    public ViewState<DetailsView> createViewState() {
        return new DetailsViewState2();
    }

    @Override @DebugLog
    public void onNewViewStateInstance() {
        //mFab.setScaleX(0f); mFab.setScaleY(0f); mFab.setAlpha(0f);
        //mFab.setVisibility(View.GONE);

        DetailsData data = new DetailsData(mID, mPresenter.getSamplesCount());
        //Insert first beacon sample if available
        Beacon beacon = mPresenter.getStickyBeacon();
        if(beacon != null) data.insertAndUpdate(beacon);
        setChartData(data);
    }

    @Override @DebugLog
    protected void onResume() {
        super.onResume();
        ((App) getApplication()).onResume();
        ColorChooserNoTitle color = (ColorChooserNoTitle) ColorChooserNoTitle.findVisible(this, ColorChooserDialog.TAG_PRIMARY);
        if(color != null) { color.setCallback(this); return; }
        TextDialog name = TextDialog.findVisible(this, "dialog_beacon_name");
        if(name != null) { name.setChangedListener(this); return; }
        IconDialog icon = IconDialog.findVisible(this, "dialog_beacon_icon");
        if(icon != null) { icon.setSelectionListener(this); return; }
        mFab.onResume();
        //Start ticker
        if(mPresenter.getBeacon() != null) {
            updateSecondCounter(mPresenter.getBeacon());
        }
        //updateTicker();
    }

    //-----------------AS CALLED END-------------

    @Override @DebugLog
    protected void onPause() {
        super.onPause();
        mFab.onPause();
        //mTickHandler.removeCallbacks(mTickRunnable);
        ((App) getApplication()).onPause();
    }

    private void updateTicker() {
        /*mTickRunnable = new Runnable() {
            @Override
            public void run() {
                updateTicker();
            }
        };
        updateSecondCounter(mPresenter.getBeacon());
        mTickHandler. postDelayed(mTickRunnable, 1000);*/
    }

    /**
     *
     * @param beacon live beacon.
     */
    @Override
    public void updateChartTick(@Nullable Beacon beacon) {
        if(mData.isInitialized()) {
            if(beacon != null && beacon.isHot()) {
                insertChartSample(mData, mChart, beacon);
            } else {
                if(!mPresenter.getExcludeMissing()) insertChartSample(mData, mChart, null);
            }
        } else if(beacon != null && beacon.isHot()) {
            mData.insertAndUpdate(beacon);
            setChartData(mData);
        }
    }

    private void updateIBC(@Nullable IBC ibc) {
        if(ibc != null) {
            mParamUUID.value.setText(ibc.prox_uuid);
            mParamMajorMinor.value.setText(
                    String.valueOf(ibc.major)+" / "+
                    String.valueOf(ibc.minor));
        } else {
            mParamUUID.value.setText("N/A");
            mParamMajorMinor.value.setText("N/A");
        }
    }

    private void updateUID(@Nullable UID uid) {
        if(uid != null) {
            mParamNamespace.value.setText(uid.namespace);
            mParamInstance.value.setText(uid.instance);
        } else {
            mParamNamespace.value.setText("N/A");
            mParamInstance.value.setText("N/A");
        }
    }

    private void updateURL(@Nullable URL url) {
        if(url != null) {
            mParamLink.value.setText(url.url);
        } else {
            mParamLink.value.setText("N/A");
        }
    }

    private void updateEDD(@Nullable EDD edd) {
        if(edd != null) {
            if(edd.namespace != null && edd.instance != null) {
                mParamNamespace.value.setText(edd.namespace);
                mParamInstance.value.setText(edd.instance);
            } else {
                mParamNamespace.value.setText("N/A");
                mParamInstance.value.setText("N/A");
            }
            if(edd.url != null) {
                mParamLink.value.setText(edd.url);
            } else {
                mParamLink.value.setText("N/A");
            }
        } else {
            mParamNamespace.value.setText("N/A");
            mParamInstance.value.setText("N/A");
            mParamLink.value.setText("N/A");
        }
    }

    private void updateTLM(@Nullable TLM tlm) {
        if(tlm != null) {
            mParamBatt.value.setText(tlm.getBattString());
            mParamTemp.value.setText(tlm.getTempString());
            mParamCPDU.value.setText(tlm.getCPDUString());
            mParamCUPT.value.setText(tlm.getCUPTString());
        } else {
            mParamBatt.value.setText("N/A");
            mParamTemp.value.setText("N/A");
            mParamCPDU.value.setText("N/A");
            mParamCUPT.value.setText("N/A");
        }
    }

    private void updateALT(@Nullable ALT alt) {
        if(alt != null) {

        } else {

        }
    }

    private void updateCommon(@Nullable Beacon beacon) {
        if(beacon != null) {
            mParamMAC.value.setText(beacon.getId().getMac());
            int tx = beacon.getTx();
            if(tx != ID.NO_TX_POWER) mParamTx.value.setText(String.valueOf(tx));
            else mParamTx.value.setText("N/A");
        } else {
            mParamMAC.value.setText("N/A");
            mParamTx.value.setText("N/A");
        }
    }

    /**
     * Procedure for inflating params and filling it with fixed content.
     * @param id
     */
    private void inflateParams(ID id) {
        switch (id.getType()) {
            case ID.T_UID:
                mParamNamespace = inflateParamRow(mParamContainer, 0);
                mParamInstance = inflateParamRow(mParamContainer, 0);
                mParamNamespace.name.setText("Namespace");
                mParamInstance.name.setText("Instance");
                inflateParamsTLM(mContainerTLM);
                mContainerTLM.setVisibility(View.VISIBLE);
                mHeaderTextTLM.setText("TLM");
                break;
            case ID.T_URL:
                mParamLink = inflateParamRow(mParamContainer, 0);
                mParamLink.name.setText("Link");
                mParamLink.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickLink(mParamLink.value.getText().toString());
                    }
                });
                inflateParamsTLM(mContainerTLM);
                mContainerTLM.setVisibility(View.VISIBLE);
                mHeaderTextTLM.setText("TLM");
                break;
            case ID.T_IBC:
                mParamMajorMinor = inflateParamRow(mParamContainer, 0);
                mParamUUID = inflateParamRow(mParamContainer, 0);
                mParamMajorMinor.name.setText("Major / Minor");
                mParamUUID.name.setText("UUID");
                break;
            case ID.T_TLM:
                inflateParamsTLM(mParamContainer);
                break;
            case ID.T_EDD:
                mParamNamespace = inflateParamRow(mParamContainer, 0);
                mParamInstance = inflateParamRow(mParamContainer, 0);
                mParamNamespace.name.setText("Namespace");
                mParamInstance.name.setText("Instance");
                mParamLink = inflateParamRow(mParamContainer, 0);
                mParamLink.name.setText("Link");
                mParamLink.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickLink(mParamLink.value.getText().toString());
                    }
                });
                inflateParamsTLM(mContainerTLM);
                mContainerTLM.setVisibility(View.VISIBLE);
                mHeaderTextTLM.setText("TLM");
                break;
        }
        mParamMAC = inflateParamRow(mParamContainer, 0);
        mParamMAC.name.setText("MAC Address");
        mParamTx = inflateParamRow(mParamContainer, 0);
        mParamTx.name.setText("Tx Power (dBm)");

        mParamAppearedText.setText("Appeared");
        mParamAppeared.setToastListener(this);
        mParamAppeared.setClipboardData("Intent action", Const.INTENT_APPEARED);

        mParamVisibleText.setText("Visible");
        mParamVisible.setToastListener(this);
        mParamVisible.setClipboardData("Intent action", Const.INTENT_VISIBLE);
        mParamDisappearedText.setText("Disappeared");
        mParamDisappeared.setToastListener(this);
        mParamDisappeared.setClipboardData("Intent action", Const.INTENT_DISAPPEARED);
    }

    private void inflateParamsTLM(ViewGroup container) {
        mParamBatt = inflateParamRow(container, 0);
        mParamTemp = inflateParamRow(container, 0);
        mParamCPDU = inflateParamRow(container, 0);
        mParamCUPT = inflateParamRow(container, 0);
        mParamBatt.name.setText("Battery");
        mParamTemp.name.setText("Temperature");
        mParamCPDU.name.setText("PDU Count");
        mParamCUPT.name.setText("Uptime");
        updateTLM(null);
    }

    private ParamLayout inflateParamRow(ViewGroup parent, int id) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ParamLayout layout = (ParamLayout) inflater.inflate(R.layout.item_details, parent, false);
        layout.setToastListener(this);
        parent.addView(layout);
        layout.setId(id);
        return layout;
    }

    @Override
    public void setName(String name) {
        mNameText.setText(name);
        mParamNameText.setText(name);
    }

    @Override
    public void setIcon(int icon) {
        mIcon.setImageResource(icon);
        mParamIconValue.setImageResource(icon);
    }

    @Override
    public void setColor(@ColorInt int color) {
        mIcon.setCircleColor(color);
        mParamColorValue.setBackgroundColor(color);
        if(mChart.getData() != null && mChart.getData().getDataSetByIndex(0) != null) {
            mChart.getData().getDataSetByIndex(0).setColor(color);
            mChart.invalidate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.details, menu);
        MenuItem item = menu.findItem(R.id.action_toggle_rssi_distance);
        item.setIcon(getIconForMode(mPresenter.getMode()));
        item = menu.findItem(R.id.action_show_average);
        item.setChecked(mPresenter.getAverage());
        item = menu.findItem(R.id.action_exclude_missing);
        item.setChecked(mPresenter.getExcludeMissing());
        item = menu.findItem(R.id.action_autoscale);
        item.setChecked(mPresenter.getAutoscale());
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_toggle_rssi_distance:
                setChartMode(!mPresenter.getMode());
                item.setIcon(getIconForMode(mPresenter.getMode()));
                return true;
            case R.id.action_show_average:
                setChartAverage(!mPresenter.getAverage());
                item.setChecked(mPresenter.getAverage());
                return true;
            case R.id.action_exclude_missing:
                setChartExclude(!mPresenter.getExcludeMissing());
                item.setChecked(mPresenter.getExcludeMissing());
                break;
            case R.id.action_autoscale:
                setChartAutoscale(!mPresenter.getAutoscale());
                item.setChecked(mPresenter.getAutoscale());
                break;
            case R.id.action_forget:
                mPresenter.forgetBeacon();
                setResult(1);
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //----------------UTIL METHODS----------------

    @Override
    public DetailsViewState2 getViewState() {
        return (DetailsViewState2) super.getViewState();
    }

    private int getIconForMode(boolean mode) {
        if(mode) return R.drawable.ic_access_point;
        else     return R.drawable.ic_ruler;
    }

    private Toast mToast;

    @Override
    public void showToast(String message) {
        if(mToast != null) mToast.cancel();
        mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    @OnClick(R.id.param_beacon_color)
    public void clickColor() {
        ColorChooserNoTitle.Builder builder = new ColorChooserNoTitle.Builder(this, R.string.color_picker_title);
        //builder.titleSub(R.string.color_picker_sub);
        builder.doneButton(R.string.positive_button);
        builder.cancelButton(R.string.negative_button);
        builder.customButton(R.string.color_picker_custom);
        builder.presetsButton(R.string.color_picker_presets);
        builder.allowUserColorInputAlpha(false);
        builder.dynamicButtonColor(false);
        builder.preselect(mPresenter.getColor());
        ColorChooserNoTitle dialog = builder.build();
        dialog.setBeacon(mPresenter.getName(), mPresenter.getColor(), mPresenter.getIcon());
        dialog.setCallback(this);
        dialog.show(this);
    }

    @OnClick(R.id.param_beacon_icon)
    public void clickIcon() {
        IconDialog dialog = IconDialog.newInstance(
                mPresenter.getName(),
                R.layout.icon_grid_2,
                mPresenter.getIcon(),
                mPresenter.getColor());
        FragmentManager FM = getSupportFragmentManager();
        dialog.show(FM, "dialog_beacon_icon");
        dialog.setSelectionListener(this);
    }

    @OnClick(R.id.param_beacon_name)
    public void clickName() {
        TextDialog dialog = TextDialog.newInstance(
                mPresenter.getName(),
                mPresenter.getColor(),
                mPresenter.getIcon());
        FragmentManager FM = getSupportFragmentManager();
        dialog.show(FM, "dialog_beacon_name");
        dialog.setChangedListener(this);
    }

    @OnClick(R.id.header_intent)
    public void clickIntentHeader() {
        String message =
                "<p>You can broadcast Intents to 3rd party apps such as Tasker.</p>" +
                        "<p>Intents are sent when beacon appears, disappears or was seen in current scan cycle " +
                        "(last option not recommended, will generate lots of data).</p>" +
                        "<p>3rd party app identifies Intent via action string. " +
                        "Tap Intent row to copy said action string.</p>" +
                        "<p>Every Intent contains extra data as listed below:</p>" +
                        "<p>Common for all beacons:<br>" +
                        "<b>type</b> either: <font color='#D32F2F'><b>IBC UID URL EDD TLM ALT</b><br></font>" +
                        "<b>eq_mode</b> either: FRM, MAC, FRMAC, MERGE<br>" +
                        "<b>name</b> as given by user<br>" +
                        "<b>mac</b> address<br>" +
                        "<b>tx</b> integer, power in dBm<br>" +
                        "<b>rssi</b> integer, signal in dBm</p>" +
                        "<p>iBeacon <font color='#D32F2F'><b>" + Const.EXTRA_TYPE_VAL_IBC +
                        "</b></font> extra:<br>" +
                        "<b>major</b> integer<br>" +
                        "<b>minor</b> integer<br>" +
                        "<b>uuid</b><br>" +
                        "<b>TLM extras</b> if present</p>" +
                        "<p>Eddystone <font color='#D32F2F'><b>" + Const.EXTRA_TYPE_VAL_UID +
                        "</b></font> extra:<br>" +
                        "<b>namespace</b><br>" +
                        "<b>instance</b><br>" +
                        "<b>TLM extras</b> if present</p>" +
                        "<p>Eddystone <font color='#D32F2F'><b>" + Const.EXTRA_TYPE_VAL_URL +
                        "</b></font> extra:<br>" +
                        "<b>url</b><br>" +
                        "<b>TLM extras</b> if present</p>" +
                        "<p>Eddystone merged <font color='#D32F2F'><b>" + Const.EXTRA_TYPE_VAL_EDD +
                        "</b></font> extra:<br>" +
                        "<b>namespace</b> if present<br>" +
                        "<b>instance</b> if present<br>" +
                        "<b>url</b> if present<br>" +
                        "<b>TLM extras</b> if present</p>" +
                        "<p>Eddystone <font color='#D32F2F'><b>" + Const.EXTRA_TYPE_VAL_TLM +
                        "</b></font> extra:<br>" +
                        "<b>batt</b> integer, mV<br>" +
                        "<b>temp</b> float, °C<br>" +
                        "<b>cpdu</b> long, sent packet count<br>" +
                        "<b>cupt</b> long, uptime, 0.1s resolution</p>" +
                        "<p>Altbeacon <font color='#D32F2F'><b>" + Const.EXTRA_TYPE_VAL_ALT +
                        "</b></font> extra:<br>" +
                        "<b>beacon_id</b> identifier</p>"+
                        "<p>All extras are strings unless denoted otherwise. " +
                        "Remember to prepend extra name with '%' for Tasker.</p>";
        new MaterialDialog.Builder(this)
                .title("Broadcasting Intents")
                .content(Html.fromHtml(message))
                .positiveText("Got it")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                        materialDialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog colorChooserDialog, int color) {
        mPresenter.setColor(color);
        setResult(1);
    }

    @Override
    public void onIconChanged(int icon) {
        mPresenter.setIcon(icon);
        setResult(1);
    }

    @Override
    public void onNameChanged(String name) {
        mPresenter.setName(name);
        setResult(1);
    }

    //----------------CHART RELATED---------------

    @Override
    public void setChartData(@NonNull DetailsData data) {
        mData = data;
        getViewState().setData(data);
        if(!data.isInitialized()) return;

        //Rewrite DetailsData to BarDataSet
        boolean mode = mPresenter.getMode();
        ArrayList<String> xVals = new ArrayList<>();
        ArrayList<BarEntry> yVals = new ArrayList<>();
        BarEntry newEntry;
        int counter = 0;
        for (Sample sample : data.getSamples()) {
            if (mode == MODE_DISTANCE) {
                newEntry = new BarEntry(sample.distance, counter);
            } else {
                newEntry = new BarEntry(sample.rssi, counter);
            }
            xVals.add(" ");
            yVals.add(newEntry);
            counter++;
        }
        BarDataSet newSet = new BarDataSet(yVals, "DataSet");
        newSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        newSet.setBarSpacePercent(40f);
        newSet.setDrawValues(false);
        newSet.setColor(mPresenter.getColor());
        BarData newData = new BarData(xVals, newSet);
        YAxisValueFormatter formatter;
        formatter = new ZeroFormatter(data.getZeroNorm(mode), mode);
        mChart.getAxisRight().setValueFormatter(formatter);
        mChart.setData(newData);

        setChartRange(mChart, data, mPresenter.getAutoscale());
        setChartAverage(mChart, data, mPresenter.getAverage());

        mChart.notifyDataSetChanged();
        mChart.invalidate();
    }

    /**
     * Procedure to add last DetailsData sample to chart.
     * @param data
     * @param chart
     */
    private void insertChartSample(DetailsData data, BarChart chart, @Nullable Beacon beacon) {
        data.insertAndUpdate(beacon);
        BarData barData = chart.getData();
        BarDataSet set = barData.getDataSetByIndex(0);
        if(set.getEntryCount() == mPresenter.getSamplesCount()) {
            barData.removeXValue(0);
            set.removeEntry(0);
            for (Entry entry : set.getYVals()) {
                entry.setXIndex(entry.getXIndex() - 1);
            }
        }
        barData.addXValue(" ");
        BarEntry newEntry = new BarEntry(
                data.getCurrentValue(mPresenter.getMode()),
                set.getEntryCount());
        barData.addEntry(newEntry, 0);
        setChartRange(chart, data, mPresenter.getAutoscale());
        setChartAverage(chart, data, mPresenter.getAverage());

        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    /**
     * Needs calling chart.invalidate() afterwards
     * @param chart
     * @param data
     * @param average
     */
    private void setChartAverage(BarChart chart, DetailsData data, boolean average) {
        YAxis axis = chart.getAxisRight();
        if(axis.getLimitLines().size() > 0) {
            //If line exists remove and make new one
            //(can't simply change value, 3rd party limitation)
            axis.getLimitLines().remove(0);
        }
        if(average) {
            boolean mode = mPresenter.getMode();
            LimitLine line = getChartLimitLine(data.getAverage(mode), data.getAverageLabel(mode));
            axis.addLimitLine(line);
            line.setEnabled(true);
        }
    }

    private LimitLine getChartLimitLine(float value, String label) {
        LimitLine line = new LimitLine(value, label);
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        line.setLineWidth(2f);
        line.enableDashedLine(12f, 12f, 0);
        line.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_BOTTOM);
        line.setTextSize(10 * scaledDensity);
        line.setTextColor(ContextCompat.getColor(this, R.color.text));
        return line;
    }

    /**
     * Needs calling chart.invalidate() afterwards
     * @param chart
     * @param data
     * @param autoscale
     */
    private void setChartRange(BarChart chart, DetailsData data, boolean autoscale) {
        YAxis axis = chart.getAxisRight();
        if(autoscale) {
            axis.setStartAtZero(false);
            if(data.getWeightedCount() == 0) { //Data is empty (contains no non-zero samples)
                setChartFullRange(chart, data);
            } else {
                boolean mode = mPresenter.getMode();
                if(mode == MODE_RSSI) {
                    float max = data.getMax(mode)+1;  //+1 in case min=max and max % 6 == 0
                    float min = data.getMin(mode)-1;  //-1 in case min=max and min % 6 == 0
                    if(min < 0) min = 0;              //We don't want to go below 0 (results in floating bar)
                    max = (float) (5*Math.ceil(max/5));
                    min = (float) (5*Math.floor(min/5));
                    axis.setLabelCount(6, true);
                    axis.setAxisMinValue(min);
                    axis.setAxisMaxValue(max);
                } else {
                    float max = data.getMax(mode);
                    float min = data.getMin(mode);
                    float range = max - min;
                    float space;
                    if(range == 0.0f) {
                        setChartFullRange(chart, data);         //TODO could be better
                    } else {
                        if(range < 0.1f) space = 0.01f;         //TODO via normal formula
                        else if(range < 0.2f) space = 0.02f;
                        else if(range < 0.5f) space = 0.05f;
                        else if(range < 1.0f) space = 0.1f;
                        else if(range < 2.5f) space = 0.2f;
                        else if(range < 5.0f) space = 0.5f;
                        else space = 1.0f;
                        max = max + space;
                        min = min - space;
                        max = ((float) Math.ceil(max/(5*space)))*space*5;
                        min = ((float) Math.floor(min/(5*space)))*space*5;
                        if(min < 0) min = 0;
                        if(max > data.getMaxEver(mode)) max = data.getMaxEver(mode);
                        axis.setLabelCount(6, true);
                        axis.setAxisMinValue(min);
                        axis.setAxisMaxValue(max);
                    }
                }
            }
        } else {
            axis.setStartAtZero(true);
            setChartFullRange(chart, data);
        }
    }

    private void setChartFullRange(BarChart chart, DetailsData data) {
        boolean mode = mPresenter.getMode();
        YAxis axis = chart.getAxisRight();
        if(mode == MODE_RSSI) {
            float min = data.getMinEver(mode);
            float max = data.getMaxEver(mode);
            float range = max - min;
            min = (float) (5*Math.floor(min/5));
            max = max + 0.1f * range;
            max = (float) (5*Math.ceil(max/5));
            range = max - min;
            int count = (int) Math.ceil(range/10.0f);
            if(count < 4) count = 4;
            else if (count > 10) count = 10;
            axis.setLabelCount(count, true);
            axis.setAxisMinValue(min);
            axis.setAxisMaxValue(max);
        } else {
            float min = data.getMinEver(mode);
            float max = data.getMaxEver(mode);
            float range = max - min;
            int count = (int) Math.ceil(range/5.0f);
            if(count < 4) count = 4;
            else if (count > 10) count = 10;
            axis.setLabelCount(count, true);
            axis.setAxisMinValue(min);
            axis.setAxisMaxValue(max);

        }
    }

    private void initChart(BarChart chart) {
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        chart.setDragEnabled(true);
        chart.setScaleYEnabled(false);
        chart.setScaleXEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setPinchZoom(false);
        chart.setHighlightPerDragEnabled(false);
        chart.setHighlightPerTapEnabled(false);

        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setDrawValueAboveBar(false);
        chart.getAxisLeft().setEnabled(false);
        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(false);
        xAxis.setDrawLimitLinesBehindData(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis yAxis = chart.getAxisRight();
        yAxis.setDrawAxisLine(false);
        yAxis.setStartAtZero(false);
        yAxis.setSpaceTop(10f);
        yAxis.setSpaceBottom(0f);
        yAxis.setTextSize(10 * scaledDensity);
        yAxis.setTextColor(ContextCompat.getColor(this, R.color.text));
        chart.getLegend().setEnabled(false);
        chart.setDescription(" ");

        chart.setNoDataText("Can't see sh*t captain!");
        Paint p = chart.getPaint(Chart.PAINT_INFO);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18, dm); //TODO use styles
        p.setTextSize(size);
        p.setColor(ContextCompat.getColor(this, R.color.gray600));
        p.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
    }

    private void setChartExclude(boolean exclude) {
        mPresenter.setExcludeMissing(exclude);
    }

    private void setChartAutoscale(boolean scale) {
        mPresenter.setAutoscale(scale);
        if(mData.isInitialized()) {
            setChartRange(mChart, mData, mPresenter.getAutoscale());
            mChart.invalidate();
        }
    }

    private void setChartAverage(boolean show) {
        mPresenter.setAverage(show);
        if(mData.isInitialized()) {
            setChartAverage(mChart, mData, mPresenter.getAverage());
            mChart.invalidate();
        }
    }

    private void setChartMode(boolean mode) {
        mPresenter.setMode(mode);
        if(mPresenter.getMode() == DetailsView.MODE_RSSI) {
            showToast("Received signal (dBm)");
        } else {
            showToast("Distance to Beacon (meters)");
        }
        updateBeaconTick(mPresenter.getBeacon());
        setChartData(mData);
    }

    private void clickLink(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
}
