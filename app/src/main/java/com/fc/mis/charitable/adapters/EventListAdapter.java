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
import com.fc.mis.charitable.activities.EventActivity;
import com.fc.mis.charitable.models.Event;
import com.fc.mis.charitable.models.GetTimeAgo;
import com.fc.mis.charitable.models.LanguageDetection;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.EventViewHolder> {

    private Context mContext;
    private List<Event> mEvents;
    private boolean mDisplayOrg = false;

    public EventListAdapter(Context context, List<Event> events, boolean displayOrg) {
        this.mContext = context;
        this.mEvents = events;
        this.mDisplayOrg = displayOrg;
    }

    public boolean isDisplayOrg() {
        return mDisplayOrg;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View viewItem = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_single_layout, parent, false);

        return new EventViewHolder(viewItem);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        final Event eventItem = mEvents.get(position);
        holder.bindEvent(eventItem);
    }

    @Override
    public int getItemCount() {
        return mEvents.size();
    }

    public class EventViewHolder extends RecyclerView.ViewHolder {
        private AppCompatImageView mCoverImg;
        private AppCompatImageView mOrgImg;
        private AppCompatTextView mOrgName;
        private AppCompatTextView mTitle;
        private AppCompatTextView mBody;
        private AppCompatTextView mTime;
        private AppCompatTextView mLocation;
        private AppCompatTextView mEventTime;
        private LinearLayoutCompat mContentLayout;
        private Event mEventRef;

        public EventViewHolder(@NonNull View view) {
            super(view);
            mCoverImg = (AppCompatImageView) view.findViewById(R.id.event_single_cover_img);
            mOrgImg = (AppCompatImageView) view.findViewById(R.id.event_single_org_thumb_img);
            mOrgName = (AppCompatTextView) view.findViewById(R.id.event_single_org_name_txt);
            mTitle = (AppCompatTextView) view.findViewById(R.id.event_single_title_txt);
            mBody = (AppCompatTextView) view.findViewById(R.id.event_single_body_txt);
            mTime = (AppCompatTextView) view.findViewById(R.id.event_single_time_stamp_txt);
            mLocation = (AppCompatTextView) view.findViewById(R.id.event_single_location_txt);
            mEventTime = (AppCompatTextView) view.findViewById(R.id.event_single_time_txt);
            mContentLayout = (LinearLayoutCompat) view.findViewById(R.id.event_single_content_layout);

            mContentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEvent();
                }
            });
        }

        public void bindEvent(Event eventRef) {
            this.mEventRef = eventRef;

            loadImage(mCoverImg, mEventRef.getThumbImg(), true);

            if (isDisplayOrg()) {
                loadImage(mOrgImg, mEventRef.getOrgThumb(), false);
                mOrgName.setText(mEventRef.getOrgName());
            } else {
                mOrgImg.setVisibility(View.GONE);
                mOrgName.setVisibility(View.GONE);
                itemView.findViewById(R.id.event_single_org_name_sep).setVisibility(View.GONE);
            }

            mTitle.setText(mEventRef.getTitle());
            mBody.setText(mEventRef.getBody());
            mLocation.setText(mEventRef.getLocation());

            mTime.setText(GetTimeAgo.getTimeAgo(mEventRef.getTimestamp(), itemView.getContext()));

            displayDateTime();
        }

        private void displayDateTime() {
            Calendar calendar = Calendar.getInstance();

            calendar.setTimeInMillis(mEventRef.getTime());

            int dayNum = calendar.get(Calendar.DAY_OF_MONTH);
            String day = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.ENGLISH);
            String month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH);

            int hour = calendar.get(Calendar.HOUR);
            String min = "" + calendar.get(Calendar.MINUTE);
            String dayNight = (calendar.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM");

            if (dayNight.equals("AM")) // 0
                hour = 12; // 12 instead

            if (min.equals("0")) // :0
                min += "0"; // :00 instead

            mEventTime.setText(String.format("%s, %s %d %d:%s %s", day, month, dayNum, hour, min, dayNight));


            LanguageDetection.checkLanguageLayoutDirectionForAr(mTitle);
            LanguageDetection.checkLanguageLayoutDirectionForAr(mBody);
        }

        private void loadImage(final AppCompatImageView imageView, final String url, final boolean placeHolder) {
            if (url == null || TextUtils.isEmpty(url) || url.equals("default")) {
                imageView.setVisibility(View.GONE); // ensure image view is invisible
                return;
            }

            Log.d("EventListAdapter", "loading image for: " + mEventRef.getTitle() + ", " + url);

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

        private void showEvent() {
            Intent intent = new Intent(mContext, EventActivity.class);
            intent.putExtra("Event", mEventRef);
            mContext.startActivity(intent);
        }
    }
}
