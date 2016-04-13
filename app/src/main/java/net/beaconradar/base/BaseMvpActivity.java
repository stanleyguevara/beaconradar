package net.beaconradar.base;

import android.os.Bundle;

import com.hannesdorfmann.mosby.mvp.MvpActivity;
import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import com.hannesdorfmann.mosby.mvp.MvpView;
import net.beaconradar.dagger.App;

public abstract class BaseMvpActivity<V extends MvpView, P extends MvpPresenter<V>> extends MvpActivity<V, P> {
    protected final String TAG = getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        injectDependencies( ((App) getApplication()).component() );
        super.onCreate(savedInstanceState);
    }

    protected abstract void injectDependencies(App.AppComponent component);
}