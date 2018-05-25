package com.mopub.simpleadsdemo;

public class LeaderboardDetailFragment extends AbstractBannerDetailFragment {

    @Override
    public int getWidth() {
        return (int) getResources().getDimension(R.dimen.leaderboard_width);
    }

    @Override
    public int getHeight() {
        return (int) getResources().getDimension(R.dimen.leaderboard_height);
    }
}
