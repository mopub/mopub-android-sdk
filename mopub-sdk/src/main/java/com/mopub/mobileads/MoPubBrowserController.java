package com.mopub.mobileads;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.mopub.common.MoPubBrowser;
import com.mopub.common.util.IntentUtils;
import com.mopub.mobileads.util.Utils;

class MoPubBrowserController extends MraidAbstractController {
    private static final String LOGTAG = "MoPubBrowserController";
    private Context mContext;

    MoPubBrowserController(MraidView view) {
        super(view);
        mContext = view.getContext();
    }
    
    protected void open(String url) {
        Log.d(LOGTAG, "Opening url: " + url);
        
        final MraidView mraidView = getMraidView();
        if (mraidView.getMraidListener() != null) {
            mraidView.getMraidListener().onOpen(mraidView);
        }

        // this is added because http/s can also be intercepted
        if (!isWebSiteUrl(url) && IntentUtils.canHandleApplicationUrl(mContext, url)) {
            launchApplicationUrl(url);
            return;
        }

        Intent i = new Intent(mContext, MoPubBrowser.class);
        i.putExtra(MoPubBrowser.DESTINATION_URL_KEY, url);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }


    private boolean launchApplicationUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        String errorMessage = "Unable to open intent.";

        return Utils.executeIntent(getMraidView().getContext(), intent, errorMessage);
    }

    private boolean isWebSiteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
