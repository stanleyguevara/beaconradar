package net.beaconradar.settings;

import android.content.SharedPreferences;

import net.beaconradar.dagger.App;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class SettingCheckbox extends SettingIcon {

    @Inject SharedPreferences mPrefs;
    @Inject EventBus mBus;
    private final String sub;
    private final boolean def;

    public SettingCheckbox(String key, String title, String sub, int icon, boolean def) {
        super(key, TYPE_CHECKBOX, title, icon);
        App.component().inject(this);
        this.sub = sub;
        this.def = def;
    }

    public boolean getChecked() {
        return mPrefs.getBoolean(key, def);
    }

    public void setChecked(boolean checked) {
        mPrefs.edit().putBoolean(key, checked).apply();
        //mBus.post(new SettingToggledEvent(key, checked));
    }

    @Override
    public String getSummary() {
        return this.sub;
    }

    @Override
    public boolean isWarning() {
        return false;
    }
}
