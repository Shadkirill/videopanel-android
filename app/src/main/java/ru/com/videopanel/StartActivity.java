package ru.com.videopanel;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.com.videopanel.api.ServiceGenerator;
import ru.com.videopanel.api.VideoService;
import ru.com.videopanel.db.DBHelper;
import ru.com.videopanel.db.PlaylistDAO;
import ru.com.videopanel.models.PlaylistInfo;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }

    public void onGreenClick(View view) {
        VideoService service = ServiceGenerator.createService(VideoService.class, "username", "password");

        Observable<List<PlaylistInfo>> playlistInfoObservable = service.playlists();
        playlistInfoObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(Observable::fromIterable)
                .flatMap(playlistInfo ->
                        service.playlistData(playlistInfo.getId()).subscribeOn(Schedulers.io())
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        DBHelper::addPlaylist,
                        error -> Log.d("LOG", "ERROR", error),
                        () -> {
                        }
                );
    }

    public void onRedClick(View view) {
        DBHelper.getAllPlaylist()
                .subscribe(
                        playlist -> Log.d("LOG", ((PlaylistDAO) playlist).getItems().get(0).getUrl()),
                        error -> Log.d("LOG", "ERROR", error),
                        () -> {
                        }
                );
    }

    public void onYellowClick(View view) {

    }
}
