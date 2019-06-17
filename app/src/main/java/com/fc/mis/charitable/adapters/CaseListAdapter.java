package com.fc.mis.charitable.adapters;

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
    private boolean mViewOnly = false;

    public CaseListAdapter(Context context, List<Case> cases, boolean displayOrg, boolean viewOnly) {
        this.mContext = context;
        this.mCases = cases;
        this.mDisplayOrg = displayOrg;
        this.mViewOnly = viewOnly;
    }

    public boolean isDisplayOrg() {
        return mDisplayOrg;
    }

    public boolean isViewOnly() {
        return mViewOnly;
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
                    showCase(false);
                }
            });

            mContentLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showMenu();
                    return false;
                }
            });
        }

        public void bindCase(Case caseRef) {
            this.mCaseRef = caseRef;

            loadImage(mCoverImg, mCaseRef.getThumbImg());

            if (isDisplayOrg()) {
                loadImage(mOrgImg, mCaseRef.getOrgThumb());
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

        private void loadImage(final AppCompatImageView imageView, final String url) {
            if (url == null || TextUtils.isEmpty(url) || url.equals("default")) {
                imageView.setVisibility(View.GONE); // ensure image view is invisible
                return;
            }

            Log.d("CaseListAdapter", "loading image for: " + mCaseRef.getTitle() + ", " + url);

            imageView.setVisibility(View.VISIBLE);

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
        }


        public void showMenu() {
            if (isViewOnly())
                return;

            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());

            builder.setItems(new String[]{"View", "Edit", "Delete"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        showCase(false);
                    } else if (which == 1) {
                        showCase(true);
                    } else if (which == 2) {
                        removeCase();
                    }
                }
            });

            builder.create().show();
        }

        private void showCase(boolean edit) {
            if (isViewOnly())
                return;

            Intent intent = new Intent(mContext, CaseActivity.class);
            intent.putExtra("EditMode", edit);
            intent.putExtra("Case", mCaseRef);
            mContext.startActivity(intent);
        }

        private void removeCase() {
            mCaseRef.remove();

            notifyItemRemoved(mCases.indexOf(mCaseRef));

            mCases.remove(mCaseRef);
        }
    }
}
