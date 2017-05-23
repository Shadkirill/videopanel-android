package ru.com.videopanel.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import io.reactivex.Observable;
import io.realm.Realm;
import ru.com.videopanel.db.dao.AllowedDateDAO;
import ru.com.videopanel.db.dao.ItemDAO;
import ru.com.videopanel.db.dao.PlaylistDAO;
import ru.com.videopanel.db.dbutil.RealmResultsObservable;
import ru.com.videopanel.models.AllowedDate;
import ru.com.videopanel.models.Item;
import ru.com.videopanel.models.Playlist;

public class DBHelper {

    public static void addPlaylist(Playlist playlist) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();
        PlaylistDAO playlistDAO = realm.createObject(PlaylistDAO.class, String.valueOf(playlist.getId()));
        playlistDAO.setLastUpdated(playlist.getLastUpdated());

        for (AllowedDate allowedDate : playlist.getAllowedDates()) {
            AllowedDateDAO allowedDateDAO = realm.createObject(AllowedDateDAO.class);
            SimpleDateFormat format = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);//2017-01-01T13:00:00+03:00
            try {
                allowedDateDAO.setStart(format.parse(allowedDate.getStart()));
                allowedDateDAO.setEnd(format.parse(allowedDate.getStart()));
            } catch (ParseException e) {
                e.printStackTrace();
                //TODO send error to server
            }

            playlistDAO.getDates().add(allowedDateDAO);
        }

        for (Item item : playlist.getItems()) {
            ItemDAO itemDAO = realm.createObject(ItemDAO.class);
            itemDAO.setItemType(item.getItemType());
            itemDAO.setCrc32(item.getCrc32());
            itemDAO.setDuration(Math.round(item.getDuration() != null ? item.getDuration() : 0));
            itemDAO.setUrl(item.getUrl());
            playlistDAO.getItems().add(itemDAO);
        }

        realm.commitTransaction();
    }

    public static Observable<PlaylistDAO> getAllPlaylist() {
        Realm realm = getRealm();
        return RealmResultsObservable
                .from(realm.where(PlaylistDAO.class).findAll());
    }

    /**
     * Get
     *
     * @return
     */
    private static Realm getRealm() {
        return Realm.getDefaultInstance();
    }

    public static void clearDB() {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.deleteAll();
        realm.commitTransaction();
    }
}
