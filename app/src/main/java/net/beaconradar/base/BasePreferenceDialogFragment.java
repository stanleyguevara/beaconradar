package net.beaconradar.base;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;

import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import com.hannesdorfmann.mosby.mvp.MvpView;
import com.hannesdorfmann.mosby.mvp.delegate.BaseMvpDelegateCallback;
import com.hannesdorfmann.mosby.mvp.delegate.FragmentMvpDelegate;
import com.hannesdorfmann.mosby.mvp.delegate.FragmentMvpDelegateImpl;
import net.beaconradar.dagger.App;

public abstract class BasePreferenceDialogFragment<V extends MvpView, P extends MvpPresenter<V>>
        extends PreferenceDialogFragmentCompat implements BaseMvpDelegateCallback<V, P> {

    protected final String TAG = getClass().getName();
    protected FragmentMvpDelegate<V, P> delegate;
    protected P presenter;

    public abstract P createPresenter();

    @NonNull protected FragmentMvpDelegate<V, P> getMvpDelegate() {
        if (delegate == null)  delegate = new FragmentMvpDelegateImpl<>(this);
        return delegate;
    }

    protected abstract void injectDependencies(App.AppComponent component);

    //----------------BaseMvpDelegateCallback methods
    @NonNull @Override public P getPresenter() {
        return presenter;
    }

    @Override public void setPresenter(@NonNull P presenter) {
        this.presenter = presenter;
    }

    @Override public boolean isRetainInstance() {
        return getRetainInstance();
    }

    @Override public boolean shouldInstanceBeRetained() {
        FragmentActivity activity = getActivity();
        boolean changingConfig = activity != null && activity.isChangingConfigurations();
        return getRetainInstance() && changingConfig;
    }

    @NonNull @Override public V getMvpView() {
        return (V) this;
    }
    //----------------BaseMvpDelegateCallback methods end


    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        injectDependencies( ((App) getActivity().getApplication()).component() );
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        getMvpDelegate().onViewCreated(null, savedInstanceState);       //TODO test if passing null here is not leaking
        return dialog;
    }

    @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {       //WARNING! Never called in PreferenceDialogFragmentCompat
        //injectDependencies( ((App) getActivity().getApplication()).component() );               //Using onCreateDialog instead
        //super.onViewCreated(view, savedInstanceState);
        //getMvpDelegate().onViewCreated(view, savedInstanceState);
    }
    //----------------Delegate methods
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMvpDelegate().onCreate(savedInstanceState);
    }
    @Override public void onDestroy() {
        super.onDestroy();
        getMvpDelegate().onDestroy();
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        getMvpDelegate().onDestroyView();
    }
    @Override public void onPause() {
        super.onPause();
        getMvpDelegate().onPause();
    }
    @Override public void onResume() {
        super.onResume();
        getMvpDelegate().onResume();
    }
    @Override public void onStart() {
        super.onStart();
        getMvpDelegate().onStart();
    }
    @Override public void onStop() {
        super.onStop();
        getMvpDelegate().onStop();
    }
    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getMvpDelegate().onActivityCreated(savedInstanceState);
    }
    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        getMvpDelegate().onAttach(activity);
    }
    @Override public void onDetach() {
        super.onDetach();
        getMvpDelegate().onDetach();
    }
    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getMvpDelegate().onSaveInstanceState(outState);
    }
    //----------------Delegate methods end

}
