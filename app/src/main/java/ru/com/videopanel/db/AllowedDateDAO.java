package ru.com.videopanel.db;

import io.realm.RealmObject;

public class AllowedDateDAO extends RealmObject {
    public static final String COL_START = "start";
    public static final String COL_END = "end";

    private String start;
    private String end;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

}
