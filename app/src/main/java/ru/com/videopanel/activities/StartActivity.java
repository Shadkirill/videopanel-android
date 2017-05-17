package ru.com.videopanel.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.com.videopanel.R;
import ru.com.videopanel.api.ServiceGenerator;
import ru.com.videopanel.api.VideoService;
import ru.com.videopanel.db.DBHelper;
import ru.com.videopanel.db.dao.ItemDAO;
import ru.com.videopanel.db.dao.PlaylistDAO;
import ru.com.videopanel.models.PlaylistInfo;

public class StartActivity extends AppCompatActivity {

    private PlaylistDAO currentPlaylist;
    private int currentPlayItem = -1;

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
                .firstElement()
                .subscribe(
                        playlist -> {
                            Log.d("LOG", playlist.getLastUpdated());
                            currentPlaylist = playlist;
                            showNextContent();

                        },
                        error -> Log.d("LOG", "ERROR", error),
                        () -> {
                        }
                );
    }

    private void showNextContent() {
        currentPlayItem++;
        if (currentPlayItem < currentPlaylist.getItems().size()) {
            ItemDAO nextContent = currentPlaylist.getItems().get(currentPlayItem);
            if (nextContent.getItemType().equals(ItemDAO.TYPE_VIDEO)) {
                startVideoActivity(nextContent.getUrl());
            } else if (nextContent.getItemType().equals(ItemDAO.TYPE_IMAGE)) {
                startImageActivity(nextContent.getUrl(), nextContent.getDuration());
            }

        } else {
            currentPlayItem = -1;
            //TODO send logs
        }
    }

    public void onYellowClick(View view) {
//        startImageActivity();
    }

    private void startVideoActivity(String url) {
        Intent intent = new Intent(this, VideoActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        intent.putExtras(bundle);
        startActivityForResult(intent, 11);
    }

    private void startImageActivity(String url, Long duration) {
        Intent intent = new Intent(this, ImageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        bundle.putLong("duration", duration);
        intent.putExtras(bundle);
        startActivityForResult(intent, 11);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 11) {
            if (resultCode == RESULT_OK) {
                showNextContent();
            }
        }

    }
}
