package net.beaconradar.fab;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.animation.LinearInterpolator;

import com.hannesdorfmann.mosby.mvp.MvpBasePresenter;
import net.beaconradar.dagger.ForApplication;
import net.beaconradar.events.FabBehaviorChangedEvent;
import net.beaconradar.events.ScanEndEvent;
import net.beaconradar.events.ScanRequestEvent;
import net.beaconradar.events.ScanStartEvent;
import net.beaconradar.events.ShowFabEvent;
import net.beaconradar.main.ServiceManager;
import net.beaconradar.utils.DefaultFabBehavior;
import net.beaconradar.utils.Defaults;
import net.beaconradar.utils.Prefs;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

@Singleton
public class ProgressFABPresenterImpl extends MvpBasePresenter<ProgressFABView>
        implements ProgressFABPresenter, FabBehavior.FabBehaviorListener {

    private final ServiceManager mManager;
    private final EventBus mBus;
    private final SharedPreferences mPrefs;

    private ValueAnimator mProgressAnim;
    private boolean mReverse;

    private int mBehavior;

    private boolean mShowing = true;

    @DebugLog
    @Inject
    public ProgressFABPresenterImpl(@ForApplication Context context, ServiceManager manager, EventBus bus, SharedPreferences prefs) {
        mManager = manager;
        mBus = bus;
        mPrefs = prefs;
        mBehavior = mPrefs.getInt(Prefs.KEY_FAB_BEHAVIOR, Defaults.FAB_BEHAVIOR);

        mProgressAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
        mProgressAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                if(isViewAttached()) getView().setProgress(va.getAnimatedFraction(), mReverse);
            }
        });
        mProgressAnim.setInterpolator(new LinearInterpolator());
    }

    //This is implemented to call from onResume to avoid unnecessary animations
    @Override @DebugLog
    public void attachView(ProgressFABView view) {
        super.attachView(view);
        if(!mBus.isRegistered(this)) mBus.register(this);
        if(isViewAttached()) {
            //getView().showState(mManager.isScanning());
            getView().setBehavior(resolveFabBehavior(mBehavior), false);
            getView().showState(mManager.isScanRequested());
            if(mShowing) getView().showNow();
            else getView().hideNow();
            //TODO Get current scan timing and translate it to animation state
        }
    }

    //This is implemented to call from onPause to avoid unnecessary animations
    @Override @DebugLog
    public void detachView(boolean retainInstance) {
        super.detachView(retainInstance);
        if(mBus.isRegistered(this)) mBus.unregister(this);
        //mProgressAnim.cancel();
        //TODO or end & reset FAB state?
    }

    public void onEvent(ShowFabEvent event) {
        if(event.show && isViewAttached()) {
            getView().restore();
            mShowing = true;
        }
    }

    @DebugLog
    public void onEvent(ScanEndEvent event) {
        mProgressAnim.cancel();
        mProgressAnim.setDuration(event.interval);
        this.mReverse = true;
        mProgressAnim.start();
    }

    @DebugLog
    public void onEvent(ScanStartEvent event){
        mProgressAnim.cancel();
        mProgressAnim.setDuration(event.duration);
        this.mReverse = false;
        mProgressAnim.start();
    }

    public void onEvent(FabBehaviorChangedEvent event) {
        mBehavior = event.behavior;
        if(isViewAttached()) {
            FabBehavior behavior = resolveFabBehavior(event.behavior);
            getView().setBehavior(behavior, true);
        }
    }

    public void onEvent(ScanRequestEvent event) {
        if(isViewAttached()) {
            getView().showState(event.scan);
        }
    }

    @Override @DebugLog
    public void setState(boolean scan) {
        if(scan) mManager.startScan();
        else {
            mManager.pauseScan();
            mProgressAnim.end();
        }
        if(isViewAttached()) getView().showState(getState());
    }

    @DebugLog
    public boolean getState() {
        return mManager.isScanRequested();
    }

    @Override
    public boolean needsFooter() {
        return mBehavior == FabBehavior.FAB_BEHAVIOR_FIXED;
    }

    private FabBehavior resolveFabBehavior(int code) {
        FabBehavior behavior;
        switch (code) {
            case FabBehavior.FAB_BEHAVIOR_SCALE: behavior = new FabBehaviorScale();   break;
            case FabBehavior.FAB_BEHAVIOR_MOVE:  behavior = new FabBehaviorMove();    break;
            case FabBehavior.FAB_BEHAVIOR_FIXED: behavior = new DefaultFabBehavior(); break;
            default:                             behavior = new DefaultFabBehavior();
        }
        return behavior;
    }

    @Override @DebugLog
    public void onShow() {
        mShowing = true;
    }

    @Override @DebugLog
    public void onHide() {
        mShowing = false;
    }
}
