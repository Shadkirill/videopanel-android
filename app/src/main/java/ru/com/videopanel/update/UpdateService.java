package ru.com.videopanel.update;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.RealmList;
import ru.com.videopanel.MessageEvent;
import ru.com.videopanel.api.NetworkUtil;
import ru.com.videopanel.api.ServiceGenerator;
import ru.com.videopanel.api.VideoService;
import ru.com.videopanel.db.DBHelper;
import ru.com.videopanel.db.dao.ItemDAO;
import ru.com.videopanel.db.dao.PlaylistDAO;
import ru.com.videopanel.files.FileSystem;
import ru.com.videopanel.models.PlaylistInfo;
import ru.com.videopanel.utils.PreferenceUtil;

public class UpdateService extends Service {
    Disposable timerSubscribe;
    FileSystem fileSystem;
    PreferenceUtil preferenceUtil;
    String tempToken;

    public UpdateService() {
    }

    @Override
    public void onCreate() {
        fileSystem = new FileSystem();
        preferenceUtil = new PreferenceUtil(getApplicationContext());

        timerSubscribe = Observable.interval(0, 30, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> Log.d("TICK", "ERROR", error))
                .doOnNext(aLong ->
                        {
                            if (NetworkUtil.isOnline(getApplicationContext())) {
                                updateIfNeeded();
                            } else {
                                Log.e("SERVICETICK", "No internet connection");
                            }
                        }
                )
                .subscribe();
    }

    private void updateIfNeeded() {
        VideoService service = ServiceGenerator.createService(VideoService.class);
        service.login(preferenceUtil.getLogin(), preferenceUtil.getPassword())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(Observable.empty())
                .flatMap(token -> {
                    tempToken = token.getToken();
                    return service.playlists(tempToken)
                            .subscribeOn(Schedulers.io());
                })
                .map(playlistInfos -> {
                    String currentPlaylistId = preferenceUtil.getCurrentPlaylistId();
                    for (PlaylistInfo remotePlaylist : playlistInfos) {
                        if (!currentPlaylistId.equals(String.valueOf(remotePlaylist.getId()))) {
                            continue;
                        }
                        Log.d("DEBUG", "Current playlist: " + currentPlaylistId + " | Remote ID: " + remotePlaylist.getId());

                        if (!DBHelper.isUpdateNeed(remotePlaylist)) {
                            continue;
                        }
                        Log.d("DEBUG", "Is update needed: " + DBHelper.isUpdateNeed(remotePlaylist));


                        EventBus.getDefault().post(new MessageEvent("stop"));
                        DBHelper.deleteOther(playlistInfos);
                        EventBus.getDefault().post(new MessageEvent("start"));
                        return playlistInfos;
                    }
                    DBHelper.deleteOther(playlistInfos);
                    //TODO delete files
                    return playlistInfos;
                })
                .flatMap(Observable::fromIterable)
                .filter(DBHelper::isUpdateNeed)
                .flatMap(playlistInfo ->
                        service.playlistData(playlistInfo.getId(), tempToken)
                                .doOnError(error -> {
                                    Log.d("LOG", "ERROR", error);
                                })
                                .subscribeOn(Schedulers.io()))
                .map(playlist -> {
                    Log.d("LOADING", "start ID:" + playlist.getId());
                    PlaylistDAO playlistDAO = DBHelper.addPlaylist(playlist);
                    PlaylistDAO playlistafterload = loadFiles(playlistDAO);

                    if (preferenceUtil.getCurrentPlaylistId().equals(String.valueOf(playlist.getId())))
                        EventBus.getDefault().post(new MessageEvent("stop"));

                    fileSystem.replasePlaylistFilesToProduction(getFilesDir(), playlistDAO.getId());
                    Log.d("DEBUG", "Remote playlist last updated: " + playlistafterload.getLastUpdated());
                    DBHelper.replacePlaylistAfterLoad(playlistafterload);

                    if (preferenceUtil.getCurrentPlaylistId().equals(String.valueOf(playlist.getId())))
                        EventBus.getDefault().post(new MessageEvent("start"));
                    Log.d("LOADING", "finish ID:" + playlist.getId());
                    return playlist;
                })
                .doOnNext(playlist -> Log.d("SERVICETICK", "updated ID:" + playlist.getId()))
                .doOnComplete(() ->
                        {
                            service.logout(tempToken).subscribeOn(Schedulers.io()).subscribe(
                                    (playlist) -> {
                                        Log.d("LOGOUT", String.valueOf(playlist.code()));
                                    },
                                    error -> Log.d("LOGOUT", "ERROR", error),
                                    () -> {
                                    }
                            );
                            tempToken = null;
                            Log.d("SERVICETICK", "complete");
                        }
                )
                .subscribe();
    }

    private PlaylistDAO loadFiles(PlaylistDAO playlist) {
        RealmList<ItemDAO> items = playlist.getItems();
        for (ItemDAO item : items) {
            try {
                String fileName = fileSystem.saveFile(getFilesDir(), playlist.getId(), item.getUrl(), item.getCrc32());
                item.setUrl(fileName);
            } catch (IOException e) {
                Log.e("FILE LOAD ERROR", item.getUrl(), e);
            }
        }
        return playlist;
    }

//    private void tryToLoadFiles() {
//        DBHelper.getNotCachedPlaylist()
//                .map(playlistDAO -> {
//                    RealmList<ItemDAO> items = playlistDAO.getItems();
//                    boolean packFlagError = false;
//                    for (ItemDAO item : items) {
//                        if (!item.isCached()) {
//                            try {
//                                String localUrl = fileSystem.saveFile(getFilesDir(), item.getUrl(), item.getCrc32());
//                                item.setUrl(localUrl);
//                                item.setCacheStatus(ItemDAO.STATUS_READY);
//                            } catch (IOException e) {
//                                Log.e("FILE LOAD ERROR", item.getUrl(), e);
//                                //TODO Log error
//                                packFlagError = true;
//                            }
//                        }
//                    }
//
//                    playlistDAO.setItems(items);
//
//                    if (!packFlagError) {
//                        playlistDAO.setCacheStatus(PlaylistDAO.STATUS_READY);
//                    }
//                    DBHelper.updatePlaylist(playlistDAO);
//                    return playlistDAO;
//                })
//                .subscribe(playlistDAO -> Log.d("CACHED", playlistDAO.getId()),
//                        e -> Log.e("CACHED", "ERROR", e),
//                        () -> {
//                        }
//                );
//    }

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
