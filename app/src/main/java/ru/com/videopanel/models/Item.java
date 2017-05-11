package ru.com.videopanel.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Item {

    @SerializedName("itemType")
    @Expose
    private String itemType;
    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("duration")
    @Expose
    private Double duration;
    @SerializedName("crc32")
    @Expose
    private String crc32;

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public String getCrc32() {
        return crc32;
    }

    public void setCrc32(String crc32) {
        this.crc32 = crc32;
    }

}