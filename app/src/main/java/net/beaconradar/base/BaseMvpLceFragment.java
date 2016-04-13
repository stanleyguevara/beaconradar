package net.beaconradar.base;

import android.os.Bundle;
import android.view.View;

import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import com.hannesdorfmann.mosby.mvp.lce.MvpLceFragment;
import com.hannesdorfmann.mosby.mvp.lce.MvpLceView;
import net.beaconradar.dagger.App;

public abstract class BaseMvpLceFragment <CV extends View, M, V extends MvpLceView<M>, P extends MvpPresenter<V>>
        extends MvpLceFragment<CV, M, V, P> {

    protected final String TAG = getClass().getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies(((App) getActivity().getApplication()).component());
    }

    //Hannes dorfmann way. Read blog post again.
    /*@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        injectDependencies( ((App) getActivity().getApplication()).component() );
        super.onViewCreated(view, savedInstanceState);
    }*/

    /*@Nullable @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        injectDependencies( ((App) getActivity().getApplication()).component() );
        return super.onCreateView(inflater, container, savedInstanceState);
    }*/

    protected abstract void injectDependencies(App.AppComponent component);
}
