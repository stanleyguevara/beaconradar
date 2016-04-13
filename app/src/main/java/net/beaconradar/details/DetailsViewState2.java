package net.beaconradar.details;

import android.os.Bundle;

import com.hannesdorfmann.mosby.mvp.viewstate.RestorableViewState;

public class DetailsViewState2 implements RestorableViewState<DetailsView> {

    private final String KEY_DATA = "samples";

    public DetailsData samples;

    @Override public void saveInstanceState(Bundle out) {
        out.putParcelable(KEY_DATA, samples);
    }

    @Override
    public RestorableViewState<DetailsView> restoreInstanceState(Bundle in) {
        samples = in.getParcelable(KEY_DATA);
        return this;
    }

    @Override public void apply(DetailsView view, boolean retained) {
        view.setChartData(samples);
    }

    public void setData(DetailsData data){
        this.samples = data;
    }

}
