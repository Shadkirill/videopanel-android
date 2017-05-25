package ru.com.videopanel.update;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.RealmList;
import ru.com.videopanel.api.ServiceGenerator;
import ru.com.videopanel.api.VideoService;
import ru.com.videopanel.db.DBHelper;
import ru.com.videopanel.db.dao.ItemDAO;
import ru.com.videopanel.db.dao.PlaylistDAO;
import ru.com.videopanel.files.FileSystem;
import ru.com.videopanel.utisl.PreferenceUtil;

public class UpdateService extends Service {
    Disposable timerSubscribe;
    FileSystem fileSystem;
    PreferenceUtil preferenceUtil = new PreferenceUtil(this);
    String tempToken;

    public UpdateService() {
    }

    @Override
    public void onCreate() {
        fileSystem = new FileSystem();
        VideoService service = ServiceGenerator.createService(VideoService.class);
//TODO NEW?
//TODO UPDATED?
//TODO Check WHAT ITEMS UPDATED?
        timerSubscribe = Observable.interval(1, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap((number) -> service.login(preferenceUtil.getLogin(), preferenceUtil.getPassword()).subscribeOn(Schedulers.io()))
                .flatMap(token -> {
                    tempToken = token.getToken();
                    return service.playlists(tempToken);
                })
                .flatMap(Observable::fromIterable)
                .filter(DBHelper::isUpdateNeed)
                .flatMap(playlistInfo -> service.playlistData(playlistInfo.getId(), tempToken))

                .doOnEach((playlist) -> Log.d("SERVICETICK", playlist.toString()))
                .doOnError(error -> Log.d("LOG", "ERROR", error))
                .doOnComplete(() -> {
                    tryToLoadFiles();
                    //TODO LOGOUT;
                    tempToken = null;
                })
                .subscribe();
    }

    private void tryToLoadFiles() {
        DBHelper.getNotCachedPlaylist()
                .map(playlistDAO -> {
                    RealmList<ItemDAO> items = playlistDAO.getItems();
                    boolean packFlagError = false;
                    for (ItemDAO item : items) {
                        if (!item.isCached()) {
                            try {
                                String localUrl = fileSystem.saveFile(getFilesDir(), item.getUrl(), item.getCrc32() != null ? Long.parseLong(item.getCrc32(), 32) : 0);
                                item.setUrl(localUrl);
                                item.setCacheStatus(ItemDAO.STATUS_READY);
                            } catch (IOException e) {
                                Log.e("FILE LOAD ERROR", item.getUrl(), e);
                                //TODO Log error
                                packFlagError = true;
                            }
                        }
                    }

                    playlistDAO.setItems(items);

                    if (!packFlagError) {
                        playlistDAO.setCacheStatus(PlaylistDAO.STATUS_READY);
                    }
                    DBHelper.updatePlaylist(playlistDAO);
                    return playlistDAO;
                })
                .subscribe(playlistDAO -> Log.d("CACHED", playlistDAO.getId()),
                        e -> Log.e("CACHED", "ERROR", e),
                        () -> {
                        }
                );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timerSubscribe.dispose();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
