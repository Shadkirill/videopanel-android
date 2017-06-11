package ru.com.videopanel.update;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.HashMap;
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
import ru.com.videopanel.models.Playlist;
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
                .doOnError(error -> {
                    Log.d("TICK", "ERROR", error);
                    DBHelper.addErrorReport("UpdateService: update tick error", (Throwable) error);
                })
                .doOnNext(aLong ->
                        doOnUpdaterTick()
                )
                .subscribe();
    }

    private void doOnUpdaterTick() {
        if (NetworkUtil.isOnline(getApplicationContext())) {
            doOnlineTick();
        } else {
            Log.e("SERVICETICK", "No internet connection");
        }
    }

    private void doOnlineTick() {
        VideoService service = ServiceGenerator.createService(VideoService.class);
        service.login(preferenceUtil.getLogin(), preferenceUtil.getPassword())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(Observable.empty())
                .map((token) -> {
                    sendPlayedPlaylists(service, token);
                    return token;
                })
                .map((token) -> {
                    sendErrors(service);
                    return token;
                })
                .flatMap(token -> getPlaylists(service, token))
                .map(playlistInfos -> {
                    deleteOutdatedPlaylists(playlistInfos);
                    return playlistInfos;
                })
                .flatMap(Observable::fromIterable)
                .filter(DBHelper::isUpdateNeed)
                .flatMap(playlistInfo ->
                        getPlaylistData(service, playlistInfo))
                .map(playlist -> {
                    Log.d("LOADING", "start ID:" + playlist.getId());
                    PlaylistDAO playlistDAO = new PlaylistDAO(playlist);
                    PlaylistDAO playlistafterload = loadFiles(playlistDAO);

                    if (preferenceUtil.getCurrentPlaylistId().equals(String.valueOf(playlist.getId())))
                        EventBus.getDefault().post(new MessageEvent("stop"));

                    fileSystem.replacePlaylistFilesToProduction(getFilesDir(), playlistDAO.getId());
                    DBHelper.replacePlaylistAfterLoad(playlistafterload);

                    Log.d("LOADING", "finish ID:" + playlist.getId());
                    return playlist;
                })
                .doOnNext(playlist -> Log.d("SERVICETICK", "updated ID:" + playlist.getId()))
                .doOnComplete(() ->
                        {
                            logout(service);
                            Log.d("SERVICETICK", "complete");
                        }
                )
                .subscribe();
    }

    private void sendErrors(VideoService service) {
        DBHelper.getErrors()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(Observable.empty())
                .map((errorReport) -> {
                    HashMap<String, Object> body = new HashMap<>();
                    body.put("date", errorReport.getDate());
                    body.put("short_text", errorReport.getShortText());
                    body.put("detailed_text", errorReport.getDetailedText());
                    service.reportError(body)
                            .subscribeOn(Schedulers.io()).subscribe(
                            (response) -> {
                                if (response.code() == 200) {
                                    DBHelper.removeErrorReport(errorReport);
                                }
                                Log.d("sendErrors", String.valueOf(response.code() + " date " + errorReport.getShortText() + " text" + errorReport.getShortText()));
                            },
                            error -> Log.d("sendErrors", "ERROR", error),
                            () -> {
                            });

                    return errorReport;
                }).subscribe();
    }

    private void sendPlayedPlaylists(VideoService service, Token token) {
        DBHelper.getPlayedPlaylistReports()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
//                .onErrorResumeNext(Observable.empty())
                .map((playlistReport) -> {
                    HashMap<String, Object> body = new HashMap<>();
                    body.put("date", playlistReport.date);
                    service.reportPlaylist(token.getToken(), playlistReport.playlistId, body)
                            .subscribeOn(Schedulers.io()).subscribe(
                            (response) -> {
                                if (response.code() == 200) {
                                    DBHelper.removePlaylistReport(playlistReport);
                                }
                                Log.d("sendPlayedPlaylists", String.valueOf(response.code() + " token" + token.getToken() + " playlist " + playlistReport.playlistId));
                            },
                            error -> Log.d("sendPlayedPlaylists", "ERROR", error),
                            () -> {
                            });

                    return playlistReport;
                }).subscribe();
    }

    private void logout(VideoService service) {
        service.logout(tempToken).subscribeOn(Schedulers.io()).subscribe(
                (playlist) -> {
                    Log.d("LOGOUT", String.valueOf(playlist.code()));
                },
                error -> {
                    Log.d("LOGOUT", "ERROR", error);
                    DBHelper.addErrorReport("UpdateService: logout error", error);
                },
                () -> {
                }
        );
        tempToken = null;
    }

    private Observable<Playlist> getPlaylistData(VideoService service, PlaylistInfo playlistInfo) {
        return service.playlistData(playlistInfo.getId(), tempToken)
                .doOnError(error -> {
                    Log.d("LOG", "ERROR", error);
                    DBHelper.addErrorReport("UpdateService: getPlaylistData error", error);
                })
                .subscribeOn(Schedulers.io());
    }

    private void deleteOutdatedPlaylists(List<PlaylistInfo> playlistInfos) {
        stopPlaylistIfWeNeedDeleteIt(playlistInfos);
        DBHelper.deleteOther(playlistInfos);
    }

    private void stopPlaylistIfWeNeedDeleteIt(List<PlaylistInfo> playlistInfos) {
        String currentPlaylistId = preferenceUtil.getCurrentPlaylistId();
        RealmResults<PlaylistDAO> other = DBHelper.getOther(playlistInfos);
        for (PlaylistDAO pl : other) {
            if (pl.getId().equals(currentPlaylistId)) {
                EventBus.getDefault().post(new MessageEvent("stop"));
                break;
            }
        }
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
                DBHelper.addErrorReport("UpdateService: loadFiles error", e);
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
