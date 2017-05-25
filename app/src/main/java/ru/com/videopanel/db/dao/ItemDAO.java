package ru.com.videopanel.db.dao;

import io.realm.RealmObject;

public class ItemDAO extends RealmObject {
    public static final String COL_TYPE = "itemType";
    public static final String COL_URL = "url";
    public static final String COL_DURATION = "duration";
    public static final String COL_CRC32 = "crc32";

    public static final String TYPE_IMAGE = "Image";
    public static final String TYPE_VIDEO = "Video";

    public static final int STATUS_READY = 1;
    public static final int STATUS_NEED_TO_CACHE = 0;

    private String itemType;
    private String url;
    private Long duration;
    private String crc32;

    private int cacheStatus = STATUS_NEED_TO_CACHE;

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

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getCrc32() {
        return crc32;
    }

    public void setCrc32(String crc32) {
        this.crc32 = crc32;
    }

    public int getCacheStatus() {
        return cacheStatus;
    }

    public void setCacheStatus(int cacheStatus) {
        this.cacheStatus = cacheStatus;
    }

    public boolean isCached() {
        return cacheStatus == STATUS_READY;
    }
}
