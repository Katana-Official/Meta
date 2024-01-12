package com.wallet.crypto.trustapp.ui.widget.adapter;

import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.List;

public class TabPagerAdapter extends FragmentPagerAdapter {

    private final List<Pair<String, Fragment>> pages;

    public TabPagerAdapter(FragmentManager fm, List<Pair<String, Fragment>> pages) {
        super(fm);

        this.pages = pages;
    }

    // Return fragment with respect to position.
    @Override
    public Fragment getItem(int position) {
        return pages.get(position).second;
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    // This method returns the title of the tab according to its position.
    @Override
    public CharSequence getPageTitle(int position) {
        return pages.get(position).first;
    }

}