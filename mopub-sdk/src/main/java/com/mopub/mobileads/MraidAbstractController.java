package com.mopub.mobileads;


class MraidAbstractController {
    private final MraidView mMraidView;

    MraidAbstractController(MraidView view) {
        super();
        mMraidView = view;
    }
    
    public MraidView getMraidView() {
        return mMraidView;
    }
}