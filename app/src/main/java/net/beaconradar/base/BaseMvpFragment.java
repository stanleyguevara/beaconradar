package net.beaconradar.base;

import android.os.Bundle;

import com.hannesdorfmann.mosby.mvp.MvpFragment;
import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import com.hannesdorfmann.mosby.mvp.MvpView;
import net.beaconradar.dagger.App;

public abstract class BaseMvpFragment <V extends MvpView, P extends MvpPresenter<V>>
        extends MvpFragment<V, P> {

    protected final String TAG = getClass().getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies(((App) getActivity().getApplication()).component());
    }

    protected abstract void injectDependencies(App.AppComponent component);
}
