package ru.com.videopanel.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Playlist {

    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("last_updated")
    @Expose
    private String lastUpdated;
    @SerializedName("allowed_dates")
    @Expose
    private List<AllowedDate> allowedDates = null;
    @SerializedName("items")
    @Expose
    private List<Item> items = null;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<AllowedDate> getAllowedDates() {
        return allowedDates;
    }

    public void setAllowedDates(List<AllowedDate> allowedDates) {
        this.allowedDates = allowedDates;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}