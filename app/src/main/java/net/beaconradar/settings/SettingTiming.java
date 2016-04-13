package net.beaconradar.settings;

import net.beaconradar.dagger.App;

import javax.inject.Inject;

public class SettingTiming extends SettingIcon {

    @Inject
    SettingsPresenterImpl mPresenter;

    public SettingTiming(String key, String title, int icon) {
        super(key, TYPE_DIALOG_TIMING, title, icon);
        App.component().inject(this);
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean isWarning() {
        return mPresenter.isWarning(FragmentSettings.keyToMode(key));
    }

    @Override
    public String getSummary() {
        return mPresenter.getSummaryString(FragmentSettings.keyToMode(key));
    }
}
