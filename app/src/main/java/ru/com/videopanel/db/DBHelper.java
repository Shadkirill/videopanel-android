package ru.com.videopanel.db;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import ru.com.videopanel.db.dao.AllowedDateDAO;
import ru.com.videopanel.db.dao.PlaylistDAO;
import ru.com.videopanel.db.dbutil.RealmResultsObservable;
import ru.com.videopanel.models.PlaylistInfo;

public class DBHelper {

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

    public static void savePlaylist(PlaylistDAO playlistDAO) {
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
        RealmResults<PlaylistDAO> all = getOther(playlistInfos);
        realm.beginTransaction();
        deletePlaylitsRecursive(all);
        realm.commitTransaction();
    }

    public static RealmResults<PlaylistDAO> getOther(List<PlaylistInfo> playlistInfos) {
        Realm realm = getRealm();
        if (playlistInfos.isEmpty()) {
            return realm.where(PlaylistDAO.class).findAll();
        } else {
            ArrayList<String> ids = new ArrayList<>();
            for (PlaylistInfo playlist : playlistInfos) {
                ids.add(String.valueOf(playlist.getId()));
            }

            RealmQuery<PlaylistDAO> data = realm.where(PlaylistDAO.class)
                    .not()
                    .in(PlaylistDAO.COL_ID, ids.toArray(new String[ids.size()]));

            return data.findAll();
        }
    }

    public static void replacePlaylistAfterLoad(PlaylistDAO playlistafterload) {
        Realm realm = getRealm();
        RealmResults<PlaylistDAO> allOld = realm.where(PlaylistDAO.class)
                .equalTo(PlaylistDAO.COL_ID, String.valueOf(playlistafterload.getId()))
                .equalTo(PlaylistDAO.COL_DOWNLOADING, false).findAll();
        realm.beginTransaction();
        if (!allOld.isEmpty())
            deletePlaylitsRecursive(allOld);

        playlistafterload.setDownloading(false);
        savePlaylist(playlistafterload);
        realm.commitTransaction();
        Log.d("Check", "replace");
    }

    private static void deletePlaylitsRecursive(RealmResults<PlaylistDAO> delete) {
        for (PlaylistDAO playlistDAO : delete) {
            playlistDAO.getItems().deleteAllFromRealm();
            playlistDAO.getDates().deleteAllFromRealm();
        }
        delete.deleteAllFromRealm();
    }
}
