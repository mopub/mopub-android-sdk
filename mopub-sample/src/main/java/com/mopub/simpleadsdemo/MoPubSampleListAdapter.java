package com.mopub.simpleadsdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

class MoPubSampleListAdapter extends ArrayAdapter<MoPubSampleAdUnit> {
    private final TrashCanClickListener mListener;

    static class ViewHolder {
        TextView separator;
        TextView description;
        TextView adUnitId;
        ImageView trashCan;
    }

    private final LayoutInflater mLayoutInflater;

    MoPubSampleListAdapter(final Context context, TrashCanClickListener listener) {
        super(context, 0, new ArrayList<MoPubSampleAdUnit>());
        mListener = listener;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final View view;
        final ViewHolder viewHolder;

        if (convertView == null) {
            view = mLayoutInflater.inflate(R.layout.ad_configuration_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.separator = (TextView) view.findViewById(R.id.separator);
            viewHolder.description = (TextView) view.findViewById(R.id.banner_description);
            viewHolder.adUnitId = (TextView) view.findViewById(R.id.banner_ad_unit_id);
            viewHolder.trashCan = (ImageView) view.findViewById(R.id.banner_delete);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        view.setTag(viewHolder);
        final MoPubSampleAdUnit sampleAdUnit = getItem(position);
        viewHolder.description.setText(sampleAdUnit.getDescription());
        viewHolder.adUnitId.setText(sampleAdUnit.getAdUnitId());

        if (isFirstInSection(position)) {
            viewHolder.separator.setVisibility(View.VISIBLE);
            viewHolder.separator.setText(sampleAdUnit.getHeaderName());
        } else {
            viewHolder.separator.setVisibility(View.GONE);
        }

        if (sampleAdUnit.isUserDefined()) {
            viewHolder.trashCan.setVisibility(View.VISIBLE);
            viewHolder.trashCan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    mListener.onTrashCanClicked(sampleAdUnit);
                }
            });
        } else {
            viewHolder.trashCan.setVisibility(View.INVISIBLE);
            viewHolder.trashCan.setOnClickListener(null);
        }

        return view;
    }

    private boolean isFirstInSection(int position) {
        if (position <= 0) {
            return true;
        }

        final MoPubSampleAdUnit previous = getItem(position - 1);
        final MoPubSampleAdUnit current = getItem(position);
        return !previous.getHeaderName().equals(current.getHeaderName());
    }
}
