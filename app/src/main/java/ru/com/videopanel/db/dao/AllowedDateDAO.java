package ru.com.videopanel.db.dao;

import java.util.Date;

import io.realm.RealmObject;

public class AllowedDateDAO extends RealmObject {
    public static final String COL_START = "start";
    public static final String COL_END = "end";

    private Date start;
    private Date end;

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

}
