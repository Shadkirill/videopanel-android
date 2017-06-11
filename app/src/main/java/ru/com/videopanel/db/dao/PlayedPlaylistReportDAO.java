package ru.com.videopanel.db.dao;


import io.realm.RealmObject;

public class PlayedPlaylistReportDAO extends RealmObject {
    public static final String COL_DATE = "date";
    public static final String COL_PLAYLIST_ID = "playlistId";

    public String date;
    public String playlistId;

    public PlayedPlaylistReportDAO() {

    }

    public String getPlaylistIdText() {
        return playlistId;
    }

    public void setPlaylistId(String shortText) {
        this.playlistId = shortText;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}