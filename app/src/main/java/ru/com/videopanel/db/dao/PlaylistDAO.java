package ru.com.videopanel.db.dao;

import io.realm.RealmList;
import io.realm.RealmObject;
import ru.com.videopanel.models.AllowedDate;
import ru.com.videopanel.models.Item;
import ru.com.videopanel.models.Playlist;

public class PlaylistDAO extends RealmObject {
    public static final String COL_ID = "id";
    public static final String COL_LAST_UPDATED = "lastUpdated";
    public static final String COL_ITEMS = "items";
    public static final String COL_DATES = "dates";
    public static final String COL_DOWNLOADING = "downloading";

    public String id;
    private String lastUpdated;
    private RealmList<ItemDAO> items;
    private RealmList<AllowedDateDAO> dates;
    private boolean downloading = true;

    public PlaylistDAO() {

    }

    public PlaylistDAO(Playlist source) {
        this.id = String.valueOf(source.getId());
        this.lastUpdated = source.getLastUpdated();
        downloading = true;

        RealmList<ItemDAO> sourceItems = new RealmList<>();
        for (Item sourceItem : source.getItems()) {
            sourceItems.add(new ItemDAO(sourceItem));
        }
        this.items = sourceItems;

        RealmList<AllowedDateDAO> sourceDates = new RealmList<>();
        for (AllowedDate sourceDate : source.getAllowedDates()) {
            sourceDates.add(new AllowedDateDAO(sourceDate));
        }
        this.dates = sourceDates;
    }

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
