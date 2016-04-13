package net.beaconradar.settings;

import android.support.annotation.ArrayRes;

import net.beaconradar.dagger.App;

import javax.inject.Inject;

public class SettingList extends SettingIcon {

    @Inject SettingsPresenterImpl mPresenter;
    public final String title;
    public final int val;
    public final int txt;
    public final int def;

    public SettingList(String key, String title, int icon, @ArrayRes int val, @ArrayRes int txt, int def) {
        super(key, TYPE_DIALOG_LIST, title, icon);
        App.component().inject(this);
        this.title = title;
        this.val = val;
        this.txt = txt;
        this.def = def;
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
        return mPresenter.getListSelectedText(key, val, txt, def);
    }

}
