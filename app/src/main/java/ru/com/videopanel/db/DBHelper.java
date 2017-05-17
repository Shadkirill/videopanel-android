package ru.com.videopanel.db;

import io.reactivex.Observable;
import io.realm.Realm;
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
            allowedDateDAO.setStart(allowedDate.getStart());
            allowedDateDAO.setEnd(allowedDate.getEnd());
            playlistDAO.getDates().add(allowedDateDAO);
        }

        for (Item item : playlist.getItems()) {
            ItemDAO itemDAO = realm.createObject(ItemDAO.class);
            itemDAO.setItemType(item.getItemType());
            itemDAO.setCrc32(item.getCrc32());
            itemDAO.setDuration(item.getDuration());
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
