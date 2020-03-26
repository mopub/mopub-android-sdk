// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;

import static android.view.View.GONE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.mopub.mobileads.MoPubView.BannerAdListener;
import static com.mopub.simpleadsdemo.Utils.hideSoftKeyboard;
import static com.mopub.simpleadsdemo.Utils.logToast;

/**
 * A base class for creating banner style ads with various height and width dimensions.
 * <p>
 * A subclass simply needs to specify the height and width of the ad in pixels, and this class will
 * inflate a layout containing a programmatically rescaled {@link MoPubView} that will be used to
 * display the ad.
 */
public abstract class AbstractBannerDetailFragment extends Fragment implements BannerAdListener {
    @Nullable private MoPubView mMoPubView;
    private MoPubSampleAdUnit mMoPubSampleAdUnit;
    private DetailFragmentViewHolder mViewHolder;
    @Nullable private CallbacksAdapter mCallbacksAdapter;

    public abstract MoPubView.MoPubAdSize getAdSize();
    protected MoPubAdSizeSettings mMoPubAdSizeSettings;

    private enum AdSizeOption {
        EXACT(0),
        MATCH_PARENT(ViewGroup.LayoutParams.MATCH_PARENT), // -1
        WRAP_CONTENT(ViewGroup.LayoutParams.WRAP_CONTENT); // -2

        final int layoutParamValue;

        AdSizeOption(final int layoutParamValue) {
            this.layoutParamValue = layoutParamValue;
        }

        public int getLayoutParamsValue() {
            // getLayoutParamsValue returns the height or width value equivalent from ViewGroup.LayoutParams
            // ViewGroup.LayoutParams.MATCH_PARENT
            return layoutParamValue;
        }

        public static AdSizeOption fromLayoutParamsValue(final int layoutParamsValue) {
            switch(layoutParamsValue) {
                case ViewGroup.LayoutParams.MATCH_PARENT:
                    return MATCH_PARENT;
                case ViewGroup.LayoutParams.WRAP_CONTENT:
                    return WRAP_CONTENT;
                default:
                    return EXACT;
            }
        }
    }


    private enum BannerCallbacks {
        LOADED("onBannerLoaded"),
        FAILED("onBannerFailed"),
        CLICKED("onBannerClicked"),
        EXPANDED("onBannerExpanded"),
        COLLAPSED("onBannerCollapsed");

        BannerCallbacks(@NonNull final String name) {
            this.name = name;
        }

        @NonNull
        private final String name;

        @Override
        @NonNull
        public String toString() {
            return name;
        }
    }

    private static class MoPubAdSizeSettings {
        MoPubView.MoPubAdSize adSize;
        int width;
        int height;

        private static final String DETAIL_STRING = "MoPub Ad Size:\nadSize=%s\nw=%s\nh=%s";

        MoPubAdSizeSettings(MoPubView.MoPubAdSize adSize,
                                   int width,
                                   int height) {
            this.adSize = adSize;
            this.width = width;
            this.height = height;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(DETAIL_STRING,
                    getAdSizeString(),
                    getLayoutParamsString(width),
                    getLayoutParamsString(height));
        }

        private String getAdSizeString() {
            return (adSize != null) ? adSize.toString() : null;
        }

        private String getLayoutParamsString(int dimension) {
            switch (dimension) {
                case MATCH_PARENT:
                    return "MATCH_PARENT";
                case WRAP_CONTENT:
                    return "WRAP_CONTENT";
                default:
                    return "" + dimension;
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mMoPubView.loadAd();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.banner_detail_fragment, container, false);
        mViewHolder = DetailFragmentViewHolder.fromView(view);

        mMoPubSampleAdUnit = MoPubSampleAdUnit.fromBundle(getArguments());
        mMoPubView = view.findViewById(R.id.banner_mopubview);
        final LinearLayout.LayoutParams layoutParams =
                (LinearLayout.LayoutParams) mMoPubView.getLayoutParams();
        mMoPubView.setLayoutParams(layoutParams);
        mMoPubView.setAdSize(getAdSize());

        mMoPubAdSizeSettings = new MoPubAdSizeSettings(mMoPubView.getAdSize(),
                layoutParams.width,
                layoutParams.height);
        mViewHolder.mAdSizeInfoView.setText(mMoPubAdSizeSettings.toString());

        mViewHolder.mKeywordsField.setText(getArguments().getString(MoPubListFragment.KEYWORDS_KEY, ""));
        mViewHolder.mUserDataKeywordsField.setText(getArguments().getString(MoPubListFragment.USER_DATA_KEYWORDS_KEY, ""));
        hideSoftKeyboard(mViewHolder.mKeywordsField);

        final String adUnitId = mMoPubSampleAdUnit.getAdUnitId();
        mViewHolder.mDescriptionView.setText(mMoPubSampleAdUnit.getDescription());
        mViewHolder.mAdUnitIdView.setText(adUnitId);
        mViewHolder.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String keywords = mViewHolder.mKeywordsField.getText().toString();
                final String userDataKeywords = mViewHolder.mUserDataKeywordsField.getText().toString();
                setupMoPubView(adUnitId, keywords, userDataKeywords);
                mMoPubView.loadAd();
            }
        });

        final RecyclerView callbacksView = view.findViewById(R.id.callbacks_recycler_view);
        final Context context = getContext();
        if (callbacksView != null && context != null) {
            callbacksView.setLayoutManager(new LinearLayoutManager(context));
            mCallbacksAdapter = new CallbacksAdapter(context);
            mCallbacksAdapter.generateCallbackList(BannerCallbacks.class);
            callbacksView.setAdapter(mCallbacksAdapter);
        }

        mViewHolder.mChangeAdSizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAdSizeClicked();
            }
        });

        mMoPubView.setBannerAdListener(this);
        setupMoPubView(adUnitId, null, null);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mMoPubView != null) {
            mMoPubView.destroy();
            mMoPubView = null;
        }
    }

    private void setupMoPubView(final String adUnitId, final String keywords, final String userDataKeywords) {
        mMoPubView.setAdUnitId(adUnitId);
        mMoPubView.setKeywords(keywords);
        mMoPubView.setUserDataKeywords(userDataKeywords);
        if (mCallbacksAdapter != null) {
            mCallbacksAdapter.generateCallbackList(BannerCallbacks.class);
        }
    }

    private String getName() {
        if (mMoPubSampleAdUnit == null) {
            return MoPubSampleAdUnit.AdType.BANNER.getName();
        }
        return mMoPubSampleAdUnit.getHeaderName();
    }

    // BannerAdListener
    @Override
    public void onBannerLoaded(MoPubView banner) {
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), getName() + " loaded.");
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(BannerCallbacks.LOADED.toString());
    }

    @Override
    public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) {
        final String errorMessage = (errorCode != null) ? errorCode.toString() : "";
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), getName() + " failed to load: " + errorMessage);
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(BannerCallbacks.FAILED.toString(), errorMessage);
    }

    @Override
    public void onBannerClicked(MoPubView banner) {
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), getName() + " clicked.");
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(BannerCallbacks.CLICKED.toString());
    }

    @Override
    public void onBannerExpanded(MoPubView banner) {
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), getName() + " expanded.");
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(BannerCallbacks.EXPANDED.toString());
    }

    @Override
    public void onBannerCollapsed(MoPubView banner) {
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), getName() + " collapsed.");
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(BannerCallbacks.COLLAPSED.toString());
    }

    @Override
    public void onBannerRequest(MoPubView moPubView){
        logToast(getActivity(), getName() + " request.");
    }

    private void onAdSizeClicked() {
        final MoPubAdSizeFragment dialogFragment = MoPubAdSizeFragment.newInstance(mMoPubAdSizeSettings);
        dialogFragment.setTargetFragment(this, 0);
        dialogFragment.show(getActivity().getSupportFragmentManager(), "adSize");
    }

    private void onAdSizeSettingsChanged(final MoPubAdSizeSettings moPubAdSizeSettings) {
        mMoPubAdSizeSettings = moPubAdSizeSettings;

        float density = getResources().getDisplayMetrics().density;

        ViewGroup.LayoutParams params = mMoPubView.getLayoutParams();
        params.height = (int) (mMoPubAdSizeSettings.height * density);
        params.width = (int) (mMoPubAdSizeSettings.width * density);
        mMoPubView.setLayoutParams(params);

        mMoPubView.setAdSize(mMoPubAdSizeSettings.adSize);
        mViewHolder.mAdSizeInfoView.setText(mMoPubAdSizeSettings.toString());
    }

    public static class MoPubAdSizeFragment extends DialogFragment {
        @Nullable Spinner adSizeSpinner;
        @Nullable Spinner adWidthSpinner;
        @Nullable Spinner adHeightSpinner;

        @Nullable EditText adWidthEditText;
        @Nullable EditText adHeightEditText;

        MoPubAdSizeSettings adSizeSettings;

        static AbstractBannerDetailFragment.MoPubAdSizeFragment newInstance(@NonNull final MoPubAdSizeSettings adSizeSettings) {
            final AbstractBannerDetailFragment.MoPubAdSizeFragment fragment =
                    new AbstractBannerDetailFragment.MoPubAdSizeFragment();
            fragment.adSizeSettings = adSizeSettings;
            return fragment;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.ad_size_dialog_title)
                    .setPositiveButton(R.string.ad_size_dialog_save_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, final int i) {
                            ((AbstractBannerDetailFragment) getTargetFragment())
                                    .onAdSizeSettingsChanged(adSizeSettings);
                            dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, final int i) {
                            dismiss();
                        }
                    })
                    .setCancelable(true)
                    .create();

            // Inflate and add our custom layout to the dialog.
            final View view = dialog.getLayoutInflater()
                    .inflate(R.layout.ad_size_dialog, null);

            adSizeSpinner = view.findViewById(R.id.ad_size_spinner);
            adWidthSpinner = view.findViewById(R.id.ad_size_width_spinner);
            adHeightSpinner = view.findViewById(R.id.ad_size_height_spinner);

            adWidthEditText = view.findViewById(R.id.ad_size_width_edit_text);
            adHeightEditText = view.findViewById(R.id.ad_size_height_edit_text);

            final ArrayAdapter dimensionArrayAdapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item,
                    android.R.id.text1,
                    AdSizeOption.values());

            adWidthEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void afterTextChanged(Editable editable) {
                    try {
                        adSizeSettings.width = Integer.parseInt(editable.toString());
                    } catch (Exception e) {
                        // Couldn't parse. Likely from a backspace or illegal character.
                    }
                }
            });

            adHeightEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void afterTextChanged(Editable editable) {
                    try {
                        adSizeSettings.height = Integer.parseInt(editable.toString());
                    } catch (Exception e) {
                        // Couldn't parse. Likely from a backspace or illegal character.
                    }
                }
            });

            final AdapterView.OnItemSelectedListener adSizeSelectionListener =
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            adSizeSettings.adSize = (MoPubView.MoPubAdSize) adapterView.getSelectedItem();

                            updateViews();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {
                            // STUB
                        }
                    };

            final AdapterView.OnItemSelectedListener adWidthSelectionListener =
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            final AdSizeOption selected = AdSizeOption.values()[i];
                            if(!AdSizeOption.EXACT.equals(selected)) { // "MATCH_PARENT" or "WRAP_CONTENT"
                                adSizeSettings.width = selected.getLayoutParamsValue();
                            } else if (adSizeSettings.width < 0) {
                                adSizeSettings.width = 0;
                            }

                            updateViews();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {
                            // STUB
                        }
                    };

            final AdapterView.OnItemSelectedListener adHeightSelectionListener =
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            final AdSizeOption selected = AdSizeOption.values()[i];
                            if(!AdSizeOption.EXACT.equals(selected)) { // "MATCH_PARENT" or "WRAP_CONTENT"
                                adSizeSettings.height = selected.getLayoutParamsValue();
                            } else if (adSizeSettings.height < 0) {
                                adSizeSettings.height = 0;
                            }

                            updateViews();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {
                            // STUB
                        }
                    };

            adSizeSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item,
                    android.R.id.text1,
                    MoPubView.MoPubAdSize.values()));
            adSizeSpinner.setOnItemSelectedListener(adSizeSelectionListener);

            adWidthSpinner.setAdapter(dimensionArrayAdapter);
            adWidthSpinner.setOnItemSelectedListener(adWidthSelectionListener);

            adHeightSpinner.setAdapter(dimensionArrayAdapter);
            adHeightSpinner.setOnItemSelectedListener(adHeightSelectionListener);

            updateViews();

            dialog.setView(view);
            return dialog;
        }

        private void updateViews() {
            // MoPubAdSize
            adSizeSpinner.setSelection(adSizeSettings.adSize.ordinal());

            // View Width
            final int width = AdSizeOption.fromLayoutParamsValue(adSizeSettings.width).ordinal();
            adWidthSpinner.setSelection(width);

            if (adSizeSettings.width == MATCH_PARENT || adSizeSettings.width == WRAP_CONTENT) {
                adWidthEditText.setVisibility(GONE);
            } else {
                adWidthEditText.setVisibility(View.VISIBLE);
                adWidthEditText.setText("" + adSizeSettings.width);
            }

            // View Height
            final int height = AdSizeOption.fromLayoutParamsValue(adSizeSettings.height).ordinal();
            adHeightSpinner.setSelection(height);

            if (adSizeSettings.height == MATCH_PARENT || adSizeSettings.height == WRAP_CONTENT) {
                adHeightEditText.setVisibility(GONE);
            } else {
                adHeightEditText.setVisibility(View.VISIBLE);
                adHeightEditText.setText("" + adSizeSettings.height);
            }
        }
    }
}
