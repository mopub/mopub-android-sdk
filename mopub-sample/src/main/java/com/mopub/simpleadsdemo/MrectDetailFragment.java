package com.mopub.simpleadsdemo;

public class MrectDetailFragment extends AbstractBannerDetailFragment {

    @Override
    public int getWidth() {
        return (int) getResources().getDimension(R.dimen.mrect_width);
    }

    @Override
    public int getHeight() {
        return (int) getResources().getDimension(R.dimen.mrect_height);
    }
}
