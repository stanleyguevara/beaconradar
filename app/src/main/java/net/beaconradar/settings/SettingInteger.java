package net.beaconradar.settings;

import net.beaconradar.dagger.App;

import javax.inject.Inject;

public class SettingInteger extends SettingIcon {

    @Inject SettingsPresenterImpl mPresenter;
    public final String title;
    //public final int def;

    public SettingInteger(String key, String title, int icon) {
        super(key, TYPE_DIALOG_INTEGER, title, icon);
        App.component().inject(this);
        this.title = title;
        //this.def = def;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean isWarning() {
        return false;
    }

    @Override
    public String getSummary() {
        return "After "+mPresenter.getCleanupDays()+" days";
    }

    public int getCleanupDays() {
        return mPresenter.getCleanupDays();
    }

}
