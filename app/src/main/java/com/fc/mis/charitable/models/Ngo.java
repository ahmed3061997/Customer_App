package com.fc.mis.charitable.models;

import java.io.Serializable;

public class Ngo implements Serializable {

    private String mId;
    private String mAdminName;
    private String mOrgName;
    private String mOrgAddress;
    private String mThumbImage;
    private int mCasesCount;
    private int mEventsCount;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getOrgName() {
        return mOrgName;
    }

    public void setOrgName(String orgName) {
        this.mOrgName = orgName;
    }

    public String getOrgAddress() {
        return mOrgAddress;
    }

    public void setOrgAddress(String orgAddress) {
        this.mOrgAddress = orgAddress;
    }

    public String getThumbImage() {
        return mThumbImage;
    }

    public void setThumbImage(String thumbImage) {
        this.mThumbImage = thumbImage;
    }

    public int getCasesCount() {
        return mCasesCount;
    }

    public void setCasesCount(int casesCount) {
        this.mCasesCount = casesCount;
    }

    public int getEventsCount() {
        return mEventsCount;
    }

    public void setEventsCount(int eventsCount) {
        this.mEventsCount = eventsCount;
    }

    public String getAdminName() {
        return mAdminName;
    }

    public void setAdminName(String adminName) {
        this.mAdminName = adminName;
    }
}
