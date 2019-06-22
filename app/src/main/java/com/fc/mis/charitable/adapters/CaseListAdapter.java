package com.fc.mis.charitable.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.fc.mis.charitable.R;
import com.fc.mis.charitable.activities.CaseActivity;
import com.fc.mis.charitable.models.Case;
import com.fc.mis.charitable.models.GetTimeAgo;
import com.fc.mis.charitable.models.LanguageDetection;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CaseListAdapter extends RecyclerView.Adapter<CaseListAdapter.CaseViewHolder> {

    private Context mContext;
    private List<Case> mCases;
    private boolean mDisplayOrg = false;

    public CaseListAdapter(Context context, List<Case> cases, boolean displayOrg) {
        this.mContext = context;
        this.mCases = cases;
        this.mDisplayOrg = displayOrg;
    }

    public boolean isDisplayOrg() {
        return mDisplayOrg;
    }

    @NonNull
    @Override
    public CaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View viewItem = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.case_single_layout, parent, false);

        return new CaseViewHolder(viewItem);
    }

    @Override
    public void onBindViewHolder(@NonNull CaseViewHolder holder, int position) {
        final Case caseItem = mCases.get(position);
        holder.bindCase(caseItem);
    }

    @Override
    public int getItemCount() {
        return mCases.size();
    }

    public class CaseViewHolder extends RecyclerView.ViewHolder {
        private AppCompatImageView mCoverImg;
        private AppCompatImageView mOrgImg;
        private AppCompatTextView mOrgName;
        private AppCompatTextView mTitle;
        private AppCompatTextView mBody;
        private AppCompatTextView mTime;
        private AppCompatTextView mDonation;
        private LinearLayoutCompat mContentLayout;
        private Case mCaseRef;

        public CaseViewHolder(@NonNull View view) {
            super(view);
            mCoverImg = (AppCompatImageView) view.findViewById(R.id.case_single_cover_img);
            mOrgImg = (AppCompatImageView) view.findViewById(R.id.case_single_org_thumb_img);
            mOrgName = (AppCompatTextView) view.findViewById(R.id.case_single_org_name_txt);
            mTitle = (AppCompatTextView) view.findViewById(R.id.case_single_title_txt);
            mBody = (AppCompatTextView) view.findViewById(R.id.case_single_body_txt);
            mTime = (AppCompatTextView) view.findViewById(R.id.case_single_time_stamp_txt);
            mDonation = (AppCompatTextView) view.findViewById(R.id.case_single_donation_txt);
            mContentLayout = (LinearLayoutCompat) view.findViewById(R.id.case_single_content_layout);

            mContentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCase();
                }
            });
        }

        public void bindCase(Case caseRef) {
            this.mCaseRef = caseRef;

            loadImage(mCoverImg, mCaseRef.getThumbImg(), true);

            if (isDisplayOrg()) {
                loadImage(mOrgImg, mCaseRef.getOrgThumb(), false);
                mOrgName.setText(mCaseRef.getOrgName());
            } else {
                mOrgImg.setVisibility(View.GONE);
                mOrgName.setVisibility(View.GONE);
                itemView.findViewById(R.id.case_single_org_name_sep).setVisibility(View.GONE);
            }

            mTitle.setText(mCaseRef.getTitle());
            mBody.setText(mCaseRef.getBody());
            mTime.setText(GetTimeAgo.getTimeAgo(mCaseRef.getTimestamp(), itemView.getContext()));

            String needed = String.valueOf(mCaseRef.getNeeded());
            String donated = String.valueOf(mCaseRef.getDonated());

            if (TextUtils.isEmpty(donated)) {
                mDonation.setText("Needs " + needed + " L.E");
            } else {
                mDonation.setText(donated + " L.E / " + needed + " L.E");
            }

            LanguageDetection.checkLanguageLayoutDirectionForAr(mTitle);
            LanguageDetection.checkLanguageLayoutDirectionForAr(mBody);
        }


        private void loadImage(final AppCompatImageView imageView, final String url, final boolean placeHolder) {
            if (url == null || TextUtils.isEmpty(url) || url.equals("default")) {
                imageView.setVisibility(View.GONE); // ensure image view is invisible
                return;
            }

            Log.d("CaseListAdapter", "loading image for: " + mCaseRef.getTitle() + ", " + url);

            imageView.setVisibility(View.VISIBLE);

            if (placeHolder)
                Picasso.get().load(url).networkPolicy(NetworkPolicy.OFFLINE)
                        .placeholder(R.drawable.image_place_holder).into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError(Exception e) {
                        Picasso.get().load(url).placeholder(R.drawable.image_place_holder).into(imageView);
                    }
                });
            else
                Picasso.get().load(url).networkPolicy(NetworkPolicy.OFFLINE)
                        .noPlaceholder().into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError(Exception e) {
                        Picasso.get().load(url).noPlaceholder().into(imageView);
                    }
                });
        }

        private void showCase() {
            Intent intent = new Intent(mContext, CaseActivity.class);
            intent.putExtra("Case", mCaseRef);

            Activity activity = ((Activity) mContext);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    mCoverImg,
                    "CoverImage");

            if (mCaseRef.getThumbImg().equals("default"))
                mContext.startActivity(intent);
            else
                mContext.startActivity(intent, options.toBundle());
        }
    }
}
