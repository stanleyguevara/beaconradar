package net.beaconradar.base;

import android.os.Bundle;
import android.view.View;

import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import com.hannesdorfmann.mosby.mvp.MvpView;
import com.hannesdorfmann.mosby.mvp.viewstate.MvpViewStateFragment;
import net.beaconradar.dagger.App;

public abstract class BaseMvpStateFragment <V extends MvpView, P extends MvpPresenter<V>>
        extends MvpViewStateFragment<V, P> {

    protected final String TAG = getClass().getName();

    //Hannes dorfmann way. Read blog post again.
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        injectDependencies( ((App) getActivity().getApplication()).component() );
        super.onViewCreated(view, savedInstanceState);
    }

    protected abstract void injectDependencies(App.AppComponent component);
}
