package ru.com.videopanel.db.dao;

import io.realm.RealmObject;

public class ErrorReportDAO extends RealmObject {
    public static final String COL_DATE = "date";
    public static final String COL_SHORT_TEXT = "shortText";
    public static final String COL_DETAILED_TEXT = "detailedText";

    public String date;
    public String shortText;
    public String detailedText;

    public ErrorReportDAO() {

    }

    public String getShortText() {
        return shortText;
    }

    public void setShortText(String shortText) {
        this.shortText = shortText;
    }

    public String getDetailedText() {
        return detailedText;
    }

    public void setDetailedText(String detailedText) {
        this.detailedText = detailedText;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

}
