package com.mopub.simpleadsdemo;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

class DetailFragmentViewHolder {
    final TextView mDescriptionView;
    final Button mLoadButton;
    final TextView mAdUnitIdView;
    final EditText mKeywordsField;

    DetailFragmentViewHolder(final TextView descriptionView, final TextView adUnitIdView,
            final EditText keywordsField, final Button loadButton) {
        mDescriptionView = descriptionView;
        mAdUnitIdView = adUnitIdView;
        mKeywordsField = keywordsField;
        mLoadButton = loadButton;
    }

    static DetailFragmentViewHolder fromView(final View view) {
        final TextView descriptionView = (TextView) view.findViewById(R.id.description);
        final TextView adUnitIdView = (TextView) view.findViewById(R.id.ad_unit_id);
        final EditText keywordsField = (EditText) view.findViewById(R.id.keywords_field);
        final Button loadButton = (Button) view.findViewById(R.id.load_button);

        return new DetailFragmentViewHolder(descriptionView, adUnitIdView,
                keywordsField, loadButton);
    }
}
