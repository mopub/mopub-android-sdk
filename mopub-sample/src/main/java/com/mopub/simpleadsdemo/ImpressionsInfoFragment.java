// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.DividerItemDecoration.VERTICAL;
import static android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImpressionsInfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImpressionsInfoFragment extends Fragment {
    private static final String ARG_IMPRESSIONS_LIST = "list_of_impressions";

    @Nullable
    private ImpressionsInfoAdapter adapter;
    private View fragmentView;

    public ImpressionsInfoFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameter.
     *
     * @param impressions - list of impressions.
     * @return A new instance of fragment ImpressionsInfoFragment.
     */
    public static ImpressionsInfoFragment newInstance(@NonNull ArrayList<String> impressions) {
        final ImpressionsInfoFragment fragment = new ImpressionsInfoFragment();
        final Bundle args = new Bundle();
        args.putStringArrayList(ARG_IMPRESSIONS_LIST, impressions);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ArrayList<String> impressionsList = null;
        if (getArguments() != null) {
            impressionsList = getArguments().getStringArrayList(ARG_IMPRESSIONS_LIST);
        }

        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_impressions_info, container, false);

        final RecyclerView recyclerView = fragmentView.findViewById(R.id.impressions_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ImpressionsInfoAdapter(container.getContext(), impressionsList);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), VERTICAL));
        recyclerView.setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

        if (adapter.stringList.size() > 0) {
            fragmentView.findViewById(R.id.text_no_impressions).setVisibility(View.GONE);
        }

        final Button closeButton = fragmentView.findViewById(R.id.close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity activity = getActivity();
                if (activity != null) {
                    activity.onBackPressed();
                }
            }
        });

        return fragmentView;
    }

    void onClear() {
        fragmentView.findViewById(R.id.text_no_impressions).setVisibility(View.VISIBLE);
        if (adapter != null) {
            adapter.stringList.clear();
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * RecyclerView adapter for the impression data list
     */
    static class ImpressionsInfoAdapter extends RecyclerView.Adapter<ImpressionsInfoAdapter.ImpressionsViewHolder> {
        private final int mBackgroundLight;
        private final int mBackgroundDark;

        @NonNull
        final List<String> stringList;

        ImpressionsInfoAdapter(@NonNull final Context context, @Nullable final List<String> list) {
            mBackgroundDark = context.getResources().getColor(R.color.listDark);
            mBackgroundLight = context.getResources().getColor(R.color.listLight);

            if (list != null) {
                stringList = list;
            } else {
                stringList = new ArrayList<>();
            }
        }

        @NonNull
        @Override
        public ImpressionsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            final View itemView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(android.R.layout.simple_list_item_1, viewGroup, false);
            return new ImpressionsViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ImpressionsViewHolder viewHolder, int i) {
            final int color = (i & 1) == 0 ? mBackgroundDark : mBackgroundLight;
            viewHolder.bindViewHolder(stringList.get(i), color);
        }

        @Override
        public int getItemCount() {
            return stringList.size();
        }

        /**
         * ViewHolder
         */
        static class ImpressionsViewHolder extends RecyclerView.ViewHolder {
            @NonNull
            final TextView infoTextView;

            ImpressionsViewHolder(View itemView) {
                super(itemView);
                itemView.setTag(this);
                infoTextView = itemView.findViewById(android.R.id.text1);
                infoTextView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        shareImpressionData(infoTextView);
                        return true;
                    }
                });
            }

            void bindViewHolder(final String text, final int color) {
                itemView.setBackgroundColor(color);
                infoTextView.setText(text);
            }

            private void shareImpressionData(@NonNull final TextView textView) {
                final Intent impressionIntent = new Intent(android.content.Intent.ACTION_SEND);
                impressionIntent.setType("text/plain");
                impressionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                impressionIntent.putExtra(Intent.EXTRA_SUBJECT, "impression data");
                impressionIntent.putExtra(Intent.EXTRA_TEXT, textView.getText().toString());

                textView.getContext().startActivity(Intent.createChooser(impressionIntent,
                        textView.getResources().getString(R.string.share_impression)));
            }
        }
    }
}
