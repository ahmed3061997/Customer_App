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

public class Case implements Serializable {
    private String mNgoId;
    private String mCaseId;
    private String mTitle;
    private String mBody;
    private String mThumbImg;
    private long mTimestamp;
    private String mOrgName;
    private String mOrgThumb;
    private int mNeeded;
    private int mDonated;
    private ArrayList<String> mImages;

    public ArrayList<String> getImages() {
        return mImages;
    }

    public void setImages(ArrayList<String> mImages) {
        this.mImages = mImages;
    }

    public String getCaseId() {
        return mCaseId;
    }

    public void setCaseId(String caseId) {
        this.mCaseId = caseId;
    }

    public String getNgoId() {
        return mNgoId;
    }

    public void setNgoId(String ngoId) {
        this.mNgoId = ngoId;
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

    public int getNeeded() {
        return mNeeded;
    }

    public void setNeeded(int needed) {
        this.mNeeded = needed;
    }

    public int getDonated() {
        return mDonated;
    }

    public void setDonated(int donated) {
        this.mDonated = donated;
    }
}
