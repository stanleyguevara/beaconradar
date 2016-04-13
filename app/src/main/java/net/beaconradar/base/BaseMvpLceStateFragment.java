package net.beaconradar.base;

import android.os.Bundle;
import android.view.View;

import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import com.hannesdorfmann.mosby.mvp.lce.MvpLceView;
import com.hannesdorfmann.mosby.mvp.viewstate.lce.MvpLceViewStateFragment;
import net.beaconradar.dagger.App;

public abstract class BaseMvpLceStateFragment<CV extends View, M, V extends MvpLceView<M>, P extends MvpPresenter<V>>
        extends MvpLceViewStateFragment<CV, M, V, P> {

    protected final String TAG = getClass().getName();

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        injectDependencies( ((App) getActivity().getApplication()).component() );
        super.onViewCreated(view, savedInstanceState);
    }

    protected abstract void injectDependencies(App.AppComponent component);
}
