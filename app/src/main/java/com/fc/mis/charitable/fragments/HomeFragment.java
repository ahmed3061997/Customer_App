package com.fc.mis.charitable.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fc.mis.charitable.activities.CaseActivity;
import com.fc.mis.charitable.activities.EventActivity;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fc.mis.charitable.R;
import com.fc.mis.charitable.activities.MainActivity;
import com.fc.mis.charitable.adapters.SectionsPagerAdapter;
import com.google.android.material.tabs.TabLayout.ViewPagerOnTabSelectedListener;

public class HomeFragment extends Fragment {
    // view pager & sections pager adapter
    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    // tab layout
    private TabLayout mTabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // view pager
        mViewPager = (ViewPager) view.findViewById(R.id.home_tab_pager);

        // view pager adapter
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // tab layout
        mTabLayout = (TabLayout) view.findViewById(R.id.home_tabs);
        mTabLayout.setupWithViewPager(mViewPager);

        mTabLayout.getTabAt(0).setIcon(R.drawable.ic_heart_hands_icon);
        mTabLayout.getTabAt(1).setIcon(R.drawable.ic_event);

        mTabLayout.addOnTabSelectedListener(new ViewPagerOnTabSelectedListener(mViewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                super.onTabSelected(tab);
                try {

                    BottomNavigationView bnv = getActivity().findViewById(R.id.main_bottom_nav_bar);
                    HideBottomViewOnScrollBehavior behavior =
                            (HideBottomViewOnScrollBehavior) ((
                                    (CoordinatorLayout.LayoutParams) bnv.getLayoutParams())
                                    .getBehavior());
                    behavior.onNestedScroll(null, bnv,
                            null, 0, -1, 0, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set title bar
        ((MainActivity) getActivity()).setActionBarTitle("Home");
        ((MainActivity) getActivity()).setActionBarShadow(false);
    }
}
