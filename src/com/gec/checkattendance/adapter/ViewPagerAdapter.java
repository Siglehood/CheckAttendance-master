package com.gec.checkattendance.adapter;

import java.util.List;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

public class ViewPagerAdapter extends PagerAdapter {
	private List<View> mViewList = null;

	public ViewPagerAdapter(List<View> list) {
		mViewList = list;
	}

	@Override
	public int getCount() {
		return mViewList.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}

	@Override
	public void destroyItem(View view, int position, Object object) {
		((ViewPager) view).removeView(mViewList.get(position));
	}

	@Override
	public Object instantiateItem(View view, int position) {
		((ViewPager) view).addView(mViewList.get(position), position);
		return mViewList.get(position);
	}
}
