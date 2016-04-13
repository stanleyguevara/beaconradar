package net.beaconradar.main;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import net.beaconradar.history.HistoryFragment;
import net.beaconradar.nearby.FragmentNearby;
import net.beaconradar.settings.FragmentSettings;

import hugo.weaving.DebugLog;

public class ViewPagerAdapter extends FragmentPagerAdapter {   //FragmentStatePagerAdapter is alternative to using getChildFragmentManager
    private final FragmentManager FM;
    private final int COUNT = 3;
    //private Fragment[] mFragments = new Fragment[COUNT];
    private String[] mTags = new String[COUNT];

    public ViewPagerAdapter(FragmentManager manager) {
        super(manager);
        this.FM = manager;
    }

    @Nullable
    public Fragment getFragment(int position) {
        String tag = mTags[position];
        Fragment fragment = null;
        if(tag != null) fragment = FM.findFragmentByTag(tag);
        return fragment;
    }

    public int getPositionForFragment(String tag) {
        for (int i = 0; i < COUNT; i++) {
            if(tag.equals(mTags[i])) return i;
        }
        return POSITION_NONE;
    }

        /*@Override
        public int getItemPosition(Object object) {
            for (int i = 0; i < COUNT; i++) {
                Fragment fragment = mFragments[i];
                if(fragment != null && fragment == object) {
                    return i;
                }
            }
            return ViewPagerAdapter.POSITION_NONE;
        }*/

    /**
     * DO NOT use this to retrieve existing fragment position. Use getFragment(int position) instead
     * @param position
     * @return
     */
    @Override @DebugLog
    public Fragment getItem(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0: fragment = FragmentNearby.newInstance(); break;
            case 1: fragment = HistoryFragment.newInstance(); break;
            case 2: fragment = FragmentSettings.newInstance(); break;
        }
        return fragment;
    }

    @Override @DebugLog
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        mTags[position] = fragment.getTag();
        //mFragments[position] = fragment;
        return fragment;
    }

    @Override
    public int getCount() {
        return COUNT;
    }
}
