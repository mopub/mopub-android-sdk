package com.mopub.mobileads.util.vast;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;

import com.mopub.common.HttpClient;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Strings;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class VastXmlManagerAggregator extends AsyncTask<String, Void, List<VastXmlManager>> {
    interface VastXmlManagerAggregatorListener {
        void onAggregationComplete(final List<VastXmlManager> vastXmlManagers);
    }

    // More than reasonable number of nested VAST urls to follow
    static final int MAX_TIMES_TO_FOLLOW_VAST_REDIRECT = 20;
    private final WeakReference<VastXmlManagerAggregatorListener> mVastXmlManagerAggregatorListener;
    private int mTimesFollowedVastRedirect;

    VastXmlManagerAggregator(final VastXmlManagerAggregatorListener vastXmlManagerAggregatorListener) {
        super();
        mVastXmlManagerAggregatorListener =
                new WeakReference<VastXmlManagerAggregatorListener>(vastXmlManagerAggregatorListener);
    }

    @Override
    protected List<VastXmlManager> doInBackground(String... strings) {
        List<VastXmlManager> vastXmlManagers = null;
        AndroidHttpClient httpClient = null;
        try {
            httpClient = HttpClient.getHttpClient();
            if (strings != null && strings.length > 0) {
                String vastXml = strings[0];

                vastXmlManagers = new ArrayList<VastXmlManager>();
                while (vastXml != null && vastXml.length() > 0 && !isCancelled()) {
                    final VastXmlManager xmlManager = new VastXmlManager();
                    xmlManager.parseVastXml(vastXml);
                    vastXmlManagers.add(xmlManager);
                    vastXml = followVastRedirect(httpClient, xmlManager.getVastAdTagURI());
                }
            }
        } catch (Exception e) {
            MoPubLog.d("Failed to parse VAST XML", e);
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
        }

        return vastXmlManagers;
    }

    @Override
    protected void onPostExecute(final List<VastXmlManager> vastXmlManagers) {
        final VastXmlManagerAggregatorListener listener = mVastXmlManagerAggregatorListener.get();
        if (listener != null) {
            listener.onAggregationComplete(vastXmlManagers);
        }
    }

    @Override
    protected void onCancelled() {
        final VastXmlManagerAggregatorListener listener = mVastXmlManagerAggregatorListener.get();
        if (listener != null) {
            listener.onAggregationComplete(null);
        }
    }

    String followVastRedirect(final AndroidHttpClient httpClient, final String redirectUrl) throws Exception {
        if (redirectUrl != null && mTimesFollowedVastRedirect < MAX_TIMES_TO_FOLLOW_VAST_REDIRECT) {
            mTimesFollowedVastRedirect++;

            final HttpGet httpget = new HttpGet(redirectUrl);
            final HttpResponse response = httpClient.execute(httpget);
            final HttpEntity entity = response.getEntity();
            return (entity != null) ? Strings.fromStream(entity.getContent()) : null;
        }
        return null;
    }

    @Deprecated
    void setTimesFollowedVastRedirect(final int timesFollowedVastRedirect) {
        mTimesFollowedVastRedirect = timesFollowedVastRedirect;
    }
}
