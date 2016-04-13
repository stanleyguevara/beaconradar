package net.beaconradar.base;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import com.hannesdorfmann.mosby.mvp.MvpView;
import com.hannesdorfmann.mosby.mvp.delegate.BaseMvpDelegateCallback;
import com.hannesdorfmann.mosby.mvp.delegate.ViewGroupMvpDelegate;
import net.beaconradar.dagger.App;

public abstract class BaseMvpView<V extends MvpView, P extends MvpPresenter<V>>
        extends View implements BaseMvpDelegateCallback<V, P> {

    protected final String TAG = getClass().getName();
    protected ViewGroupMvpDelegate<V, P> delegate;
    protected P presenter;

    public BaseMvpView(Context context) {
        super(context);
        injectDependencies(App.component());
    }

    public BaseMvpView(Context context, AttributeSet attrs) {
        super(context, attrs);
        injectDependencies(App.component());
    }

    public BaseMvpView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        injectDependencies(App.component());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BaseMvpView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        injectDependencies(App.component());
    }

    protected abstract void injectDependencies(App.AppComponent component);

    //----------------BaseMvpDelegateCallback methods
    @NonNull
    @Override public P getPresenter() {
        return presenter;
    }

    @Override public void setPresenter(@NonNull P presenter) {
        this.presenter = presenter;
    }

    @Override public boolean isRetainInstance() {
        return false;
    }

    @Override public boolean shouldInstanceBeRetained() {
        return false;
    }

    @NonNull @Override public V getMvpView() {
        return (V) this;
    }
    //----------------BaseMvpDelegateCallback methods end

    //----------------Delegate methods
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        delegate.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        delegate.onDetachedFromWindow();
    }
    //----------------Delegate methods end
}
