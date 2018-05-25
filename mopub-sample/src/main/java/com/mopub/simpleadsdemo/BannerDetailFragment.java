package com.mopub.simpleadsdemo;

public class BannerDetailFragment extends AbstractBannerDetailFragment {

    @Override
    public int getWidth() {
        return (int) getResources().getDimension(R.dimen.banner_width);
    }

    @Override
    public int getHeight() {
        return (int) getResources().getDimension(R.dimen.banner_height);
    }
}
