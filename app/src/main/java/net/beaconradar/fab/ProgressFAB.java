package net.beaconradar.fab;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.hannesdorfmann.mosby.mvp.delegate.BaseMvpDelegateCallback;
import com.hannesdorfmann.mosby.mvp.delegate.ViewGroupMvpDelegateImpl;
import net.beaconradar.R;
import net.beaconradar.dagger.App;

import javax.inject.Inject;

import hugo.weaving.DebugLog;

public class ProgressFAB extends FloatingActionButton
        implements BaseMvpDelegateCallback<ProgressFABView, ProgressFABPresenter>,
        ProgressFABView, FabBehavior.FabBehaviorListener {

    private CoordinatorLayout mCoordinator;
    private Paint mProgressPaint = new Paint();
    private Paint mIncompletePaint = new Paint();
    private final RectF mOval = new RectF();
    private final float mStartAngle = 90.0f;
    private float mStrokeWidth = 8.0f;
    private float mSweepAngle = 0.0f;
    private float mPadH = 0.0f; //Horizontal padding
    private float mPadV = 0.0f; //Vertical padding
    private float mSize = 0.0f;
    private float mPadI = 0.0f; //Internal padding
    private boolean mReverse = false;

    private FabBehavior mBehavior;

    protected final String TAG = getClass().getName();
    protected ViewGroupMvpDelegateImpl<ProgressFABView, ProgressFABPresenter> mDelegate = new ViewGroupMvpDelegateImpl<>(this);

    @Inject public ProgressFABPresenterImpl mPresenter;

    public ProgressFAB(Context context) {
        super(context);
        App.component().inject(this);
    }

    public ProgressFAB(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ProgressFAB(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {  //TODO notify presenter to stop anim
        super.onVisibilityChanged(changedView, visibility);
    }

    public void setCoordinator(CoordinatorLayout coordinator) {
        this.mCoordinator = coordinator;
    }

    @NonNull
    @Override
    public ProgressFABPresenter createPresenter() {
        return this.mPresenter;
    }

    @Override
    public ProgressFABPresenter getPresenter() {
        return this.mPresenter;
    }

    @Override
    public void setPresenter(ProgressFABPresenter presenter) {
        this.mPresenter = (ProgressFABPresenterImpl) presenter;
    }

    @Override @DebugLog
    public ProgressFABView getMvpView() {
        return this;
    }

    @Override
    public boolean isRetainInstance() {
        return false;
    }

    @Override
    public void setRetainInstance(boolean retainingInstance) {

    }

    @Override
    public boolean shouldInstanceBeRetained() {
        return false;
    }

    public void onPause() {
        mDelegate.onDetachedFromWindow();
    }

    public void onResume() {
        mDelegate.onAttachedToWindow();
    }

    @DebugLog
    private void init(Context context, AttributeSet attrs) {
        App.component().inject(this);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ProgressFAB,
                0, 0);

        int progressColor;
        int incompleteColor;

        mSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());
        mPadI = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
        try {
            mStrokeWidth = a.getDimension(R.styleable.ProgressFAB_pfab_strokeWidth, 30.0f);
            progressColor = a.getColor(R.styleable.ProgressFAB_pfab_progressColor, ContextCompat.getColor(context, R.color.primary));
            incompleteColor = a.getColor(R.styleable.ProgressFAB_pfab_incompleteProgressColor, ContextCompat.getColor(context, R.color.translucent));
        } finally {
            a.recycle();
        }

        mProgressPaint.setColor(progressColor);
        mProgressPaint.setStrokeWidth(mStrokeWidth);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        mIncompletePaint.setColor(incompleteColor);
        mIncompletePaint.setStrokeWidth(mStrokeWidth);
        mIncompletePaint.setStyle(Paint.Style.STROKE);
        mIncompletePaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        showState(mPresenter.getState());
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.setState(!mPresenter.getState());
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mPadH = (getMeasuredWidth() - mSize) / 2;
        mPadV = (getMeasuredHeight() - mSize) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mOval.set(
                mStrokeWidth / 2 + mPadH + mPadI,
                mStrokeWidth / 2 + mPadV + mPadI,
                mSize - (mStrokeWidth / 2) + mPadH - mPadI,
                mSize - (mStrokeWidth / 2) + mPadV - mPadI);
        if(mReverse) {
            canvas.drawArc(mOval,
                    -mStartAngle + (mSweepAngle * 360),
                    360 - (mSweepAngle * 360),
                    false,
                    mProgressPaint);
        } else {
            canvas.drawArc(mOval,
                    -mStartAngle,
                    (mSweepAngle * 360),
                    false,
                    mProgressPaint);
        }
        //Incomplete progress arc
        //canvas.drawArc(mOval, mSweepAngle * 360- mStartAngle, 360 - (mSweepAngle * 360), false, mIncompletePaint);
    }

    @Override
    public void showState(boolean scanning) {
        if(scanning) setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_pause));
        else         setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_play));
    }

    @Override
    public void setProgress(float progress, boolean reverse) {
        this.mReverse = reverse;
        this.mSweepAngle = progress;
        this.invalidate();
    }

    public void restore() {
        ViewCompat.animate(this)
                .translationY(mBehavior.getFabTranslationYForSnackbar(mCoordinator, this, null))
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {
                        setVisibility(VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        mBehavior.showNow(mCoordinator, ProgressFAB.this);
                    }

                    @Override
                    public void onAnimationCancel(View view) {
                        mBehavior.showNow(mCoordinator, ProgressFAB.this);
                    }
                });
    }

    @Override
    public void setBehavior(FabBehavior behavior, boolean animate) {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getLayoutParams();
        params.setBehavior(behavior);
        mBehavior = behavior;
        mBehavior.setListener(this);
        setLayoutParams(params);
        if(!animate) return;
        restore();
    }

    public void resetBehavior() {
        mBehavior.showNow(mCoordinator, this);
        /*setTranslationY(mBehavior.getFabTranslationYForSnackbar(mCoordinator, this, null));
        setScaleX(1f);
        setScaleY(1f);
        setAlpha(1f);
        setVisibility(View.VISIBLE);*/
    }

    @DebugLog
    public void hideNow() {
        mBehavior.hideNow(mCoordinator, this);
    }

    @DebugLog
    public void showNow() {
        mBehavior.showNow(mCoordinator, this);
    }

    @Override
    public void onHide() {
        mPresenter.onHide();
    }

    @Override
    public void onShow() {
        mPresenter.onShow();
    }

    public void setProgressColor(int color) {
        mProgressPaint.setColor(color);
    }

    public void setIncompleteColor(int color) {
        mIncompletePaint.setColor(color);
    }

    public boolean needsFooter() {
        return mPresenter.needsFooter();
    }
}
