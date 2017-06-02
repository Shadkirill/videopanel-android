package ru.com.videopanel.db.dao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.RealmObject;
import ru.com.videopanel.models.AllowedDate;

public class AllowedDateDAO extends RealmObject {
    public static final String COL_START = "start";
    public static final String COL_END = "end";
    private Date start;
    private Date end;

    public AllowedDateDAO() {

    }

    public AllowedDateDAO(AllowedDate source) {
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);//2017-01-01T13:00:00+03:00
        try {
            this.start = format.parse(source.getStart());
            this.end = format.parse(source.getEnd());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

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
