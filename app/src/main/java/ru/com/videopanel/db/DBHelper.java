package ru.com.videopanel.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import ru.com.videopanel.db.dao.AllowedDateDAO;
import ru.com.videopanel.db.dao.ItemDAO;
import ru.com.videopanel.db.dao.PlaylistDAO;
import ru.com.videopanel.db.dbutil.RealmResultsObservable;
import ru.com.videopanel.models.AllowedDate;
import ru.com.videopanel.models.Item;
import ru.com.videopanel.models.Playlist;
import ru.com.videopanel.models.PlaylistInfo;

public class DBHelper {

    public static PlaylistDAO addPlaylist(Playlist playlist) {
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();
        PlaylistDAO playlistDAO;


        playlistDAO = realm.createObject(PlaylistDAO.class);

        playlistDAO.setId(String.valueOf(playlist.getId()));
        playlistDAO.setLastUpdated(playlist.getLastUpdated());
        playlistDAO.setDownloading(true);

        playlistDAO.getDates().deleteAllFromRealm();

        for (AllowedDate allowedDate : playlist.getAllowedDates()) {
            AllowedDateDAO allowedDateDAO = realm.createObject(AllowedDateDAO.class);
            SimpleDateFormat format = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);//2017-01-01T13:00:00+03:00
            try {
                allowedDateDAO.setStart(format.parse(allowedDate.getStart()));
                allowedDateDAO.setEnd(format.parse(allowedDate.getEnd()));
            } catch (ParseException e) {
                e.printStackTrace();
                //TODO send error to server
            }

            playlistDAO.getDates().add(allowedDateDAO);
        }

        //DELETE OLD
        playlistDAO.getItems().deleteAllFromRealm();

        for (Item item : playlist.getItems()) {
            ItemDAO itemDAO = realm.createObject(ItemDAO.class);
            itemDAO.setItemType(item.getItemType());
            itemDAO.setCrc32(item.getCrc32());
            itemDAO.setDuration(Math.round(item.getDuration() != null ? item.getDuration() : 0));
            itemDAO.setUrl(item.getUrl());
            playlistDAO.getItems().add(itemDAO);
        }
        realm.copyToRealm(playlistDAO);
        realm.commitTransaction();
        return realm.copyFromRealm(playlistDAO);
    }

    public static Observable<PlaylistDAO> getAllPlaylist() {
        Realm realm = getRealm();
        return RealmResultsObservable
                .from(realm.where(PlaylistDAO.class).findAll());
    }

    public static Observable<PlaylistDAO> getCurrentPlaylist() {
        Date currentDate = new Date();

        PlaylistDAO playlistDAO = new PlaylistDAO();
        playlistDAO.setItems(null);

        Realm realm = getRealm();
        return RealmResultsObservable
                .from(realm.where(PlaylistDAO.class)
                        .lessThanOrEqualTo(PlaylistDAO.COL_DATES + "." + AllowedDateDAO.COL_START, currentDate)
                        .greaterThan(PlaylistDAO.COL_DATES + "." + AllowedDateDAO.COL_END, currentDate)
                        .equalTo(PlaylistDAO.COL_DOWNLOADING, false)
                        .findAll())
                .switchIfEmpty(Observable.just(playlistDAO));
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

    public static void updatePlaylist(PlaylistDAO playlistDAO) {
        Realm realm = getRealm();
        realm.copyToRealm(playlistDAO);
    }

    public static boolean isUpdateNeed(PlaylistInfo playlistInfo) {
        Realm realm = getRealm();
        PlaylistDAO first = realm.
                where(PlaylistDAO.class)
                .equalTo(PlaylistDAO.COL_ID, String.valueOf(playlistInfo.getId()))
                .findFirst();
        if (first == null)
            return true;
        if (!playlistInfo.getLastUpdated().equals(first.getLastUpdated()))
            return true;
        return false;
    }

    public static void deleteOther(List<PlaylistInfo> playlistInfos) {
        Realm realm = getRealm();
        if (playlistInfos.isEmpty()) {
            realm.beginTransaction();
            RealmResults<PlaylistDAO> all = realm.where(PlaylistDAO.class).findAll();
            all.deleteAllFromRealm();
            realm.commitTransaction();
        } else {
            ArrayList<String> ids = new ArrayList<>();
            for (PlaylistInfo playlist : playlistInfos) {
                ids.add(String.valueOf(playlist.getId()));
            }

            RealmQuery<PlaylistDAO> data = realm.where(PlaylistDAO.class)
                    .not()
                    .in(PlaylistDAO.COL_ID, ids.toArray(new String[ids.size()]));

            RealmResults<PlaylistDAO> all = data.findAll();
            realm.beginTransaction();
            all.deleteAllFromRealm();
            realm.commitTransaction();
        }
    }

    public static void replacePlaylistAfterLoad(PlaylistDAO playlistafterload) {
        Realm realm = getRealm();
        PlaylistDAO old = realm.where(PlaylistDAO.class)
                .equalTo(PlaylistDAO.COL_ID, String.valueOf(playlistafterload.getId()))
                .equalTo(PlaylistDAO.COL_DOWNLOADING, false).findFirst();
        realm.beginTransaction();
        if (old != null)
            old.deleteFromRealm();

        playlistafterload.setDownloading(false);
        updatePlaylist(playlistafterload);
        realm.commitTransaction();

    }
}
