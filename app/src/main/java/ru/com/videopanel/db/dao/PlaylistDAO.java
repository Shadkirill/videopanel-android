package ru.com.videopanel.db.dao;

import io.realm.RealmList;
import io.realm.RealmObject;

public class PlaylistDAO extends RealmObject {
    public static final String COL_ID = "id";
    public static final String COL_LAST_UPDATED = "lastUpdated";
    public static final String COL_ITEMS = "items";
    public static final String COL_DATES = "dates";
    public static final String COL_DOWNLOADING = "downloading";

    public static final int STATUS_READY = 1;
    public static final int STATUS_NEED_TO_CACHE_ITEMS = 0;

    public String id;
    private String lastUpdated;
    private RealmList<ItemDAO> items;
    private RealmList<AllowedDateDAO> dates;
    private boolean downloading = true;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public RealmList<ItemDAO> getItems() {
        return items;
    }

    public void setItems(RealmList<ItemDAO> items) {
        this.items = items;
    }

    public RealmList<AllowedDateDAO> getDates() {
        return dates;
    }

    public void setDates(RealmList<AllowedDateDAO> dates) {
        this.dates = dates;
    }

    public boolean isDownloading() {
        return downloading;
    }

    public void setDownloading(boolean downloading) {
        this.downloading = downloading;
    }
}
