package com.fc.mis.charitable.models;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Event implements Serializable {
    private String mEventId;
    private String mTitle;
    private String mBody;
    private String mThumbImg;
    private long mTimestamp;
    private String mOrgName;
    private String mOrgThumb;


    private long mTime;
    private String mLocation;
    private ArrayList<String> mImages;

    public ArrayList<String> getImages() {
        return mImages;
    }

    public void setImages(ArrayList<String> mImages) {
        this.mImages = mImages;
    }

    public String getEventId() {
        return mEventId;
    }

    public void setEventId(String caseId) {
        this.mEventId = caseId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getBody() {
        return mBody;
    }

    public void setBody(String body) {
        this.mBody = body;
    }

    public String getThumbImg() {
        return mThumbImg;
    }

    public void setThumbImg(String thumbImg) {
        this.mThumbImg = thumbImg;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long mTimestamp) {
        this.mTimestamp = mTimestamp;
    }

    public String getOrgName() {
        return mOrgName;
    }

    public void setOrgName(String orgName) {
        this.mOrgName = orgName;
    }

    public String getOrgThumb() {
        return mOrgThumb;
    }

    public void setOrgThumb(String orgThumb) {
        this.mOrgThumb = orgThumb;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long mTime) {
        this.mTime = mTime;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String location) {
        this.mLocation = location;
    }

    public void remove() {
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        databaseReference
                .child("Events")
                .child(User.getCurrentUserId())
                .child(mEventId)
                .removeValue();

        User.getCurrentUser().modifyCounter("events", -1); // decrement
    }


    public static void saveEvent(final Event eventRef, final OnCompleteListener<Void> completeListener) {
        final String userId = User.getCurrentUserId(); // ngo id
        String eventId = eventRef.getEventId();

        final DatabaseReference databaseRef;

        if (eventId == null) {
            databaseRef = FirebaseDatabase.getInstance().getReference()
                    .child("Events")
                    .child(userId) // ngo id
                    .push(); // event id (generated)

            // new event
            User.getCurrentUser().modifyCounter("events", 1); // increment

        } else {
            databaseRef = FirebaseDatabase.getInstance().getReference()
                    .child("Events")
                    .child(userId) // ngo id
                    .child(eventId); // event id (given)
        }

        HashMap<String, Object> caseMap = new HashMap<>();
        caseMap.put("title", eventRef.getTitle());
        caseMap.put("body", eventRef.getBody());
        caseMap.put("timestamp", ServerValue.TIMESTAMP);
        caseMap.put("time", eventRef.getTime());
        caseMap.put("location", eventRef.getLocation());

        if (eventRef.getThumbImg() == null)
            caseMap.put("thumb_img", "default");

        eventRef.setEventId(databaseRef.getKey()); // forward case id again (generated or given)

        databaseRef.updateChildren(caseMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                completeListener.onComplete(task); // forward callback
            }
        });
    }
}
