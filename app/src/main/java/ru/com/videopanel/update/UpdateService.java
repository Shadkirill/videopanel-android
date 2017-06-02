package ru.com.videopanel.update;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.RealmList;
import io.realm.RealmResults;
import ru.com.videopanel.MessageEvent;
import ru.com.videopanel.api.NetworkUtil;
import ru.com.videopanel.api.ServiceGenerator;
import ru.com.videopanel.api.VideoService;
import ru.com.videopanel.db.DBHelper;
import ru.com.videopanel.db.dao.ItemDAO;
import ru.com.videopanel.db.dao.PlaylistDAO;
import ru.com.videopanel.files.FileSystem;
import ru.com.videopanel.models.PlaylistInfo;
import ru.com.videopanel.models.Token;
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
                        doOnUpdaterTick()
                )
                .subscribe();
    }

    private void doOnUpdaterTick() {
        if (NetworkUtil.isOnline(getApplicationContext())) {
            doOnelineTick();
        } else {
            Log.e("SERVICETICK", "No internet connection");
        }
    }

    private void doOnelineTick() {
        VideoService service = ServiceGenerator.createService(VideoService.class);
        service.login(preferenceUtil.getLogin(), preferenceUtil.getPassword())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(Observable.empty())
                .flatMap(token -> getPlaylists(service, token))
                .map(playlistInfos -> {
                    String currentPlaylistId = preferenceUtil.getCurrentPlaylistId();
                    RealmResults<PlaylistDAO> other = DBHelper.getOther(playlistInfos);
                    for (PlaylistDAO pl : other) {
                        if (pl.getId().equals(currentPlaylistId)) {
                            EventBus.getDefault().post(new MessageEvent("stop"));
                            break;
                        }
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
                    PlaylistDAO playlistDAO = new PlaylistDAO(playlist);
                    PlaylistDAO playlistafterload = loadFiles(playlistDAO);

                    if (preferenceUtil.getCurrentPlaylistId().equals(String.valueOf(playlist.getId())))
                        EventBus.getDefault().post(new MessageEvent("stop"));

                    fileSystem.replasePlaylistFilesToProduction(getFilesDir(), playlistDAO.getId());
                    DBHelper.replacePlaylistAfterLoad(playlistafterload);

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

    private ObservableSource<List<PlaylistInfo>> getPlaylists(VideoService service, Token token) {
        tempToken = token.getToken();
        return service.playlists(tempToken)
                .subscribeOn(Schedulers.io());
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
