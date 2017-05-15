package ru.com.videopanel;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;
import ru.com.videopanel.api.ServiceGenerator;
import ru.com.videopanel.api.VideoService;
import ru.com.videopanel.models.Playlist;
import ru.com.videopanel.models.PlaylistInfo;

public class StartActivity extends AppCompatActivity {
    VideoView videoView;
    MediaController mediacontroller;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        videoView = (VideoView) findViewById(R.id.videoView);

    }

    public void onStartClick(View view) {
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
                .subscribeWith(new DisposableObserver<Playlist>() {
                    @Override
                    public void onNext(@NonNull Playlist playlist) {
                        startPlayer(playlist.getItems().get(2).getUrl());
                        Log.d("LOG", String.valueOf(playlist.getItems().get(2).getUrl()) + "  " + String.valueOf(playlist.getAllowedDates().get(0).getStart()));
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e("LOG", "ERROR", e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });



    }

    public void onFinishClick(View view) {
//        VideoService service = ServiceGenerator.createService(VideoService.class, "username", "password");
//
//        Observable<Response<Void>> reportErrorObservable = service.reportError("1", "1", "1", "1");
//        reportErrorObservable
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(
//                        response -> {
//                            if (response.code() == 200) {
//                                Log.d("LOG", response.toString());
//                            } else {
//                                Log.e("LOG", response.message());
//                            }
//                        },
//                        error -> Log.d("LOG", "ERROR", error),
//                        () -> {
//                        });
        VideoService service = ServiceGenerator.createService(VideoService.class, "username", "password");

        Observable<Response<Void>> reportErrorObservable = service.reportPlaylist("1", "1");
        reportErrorObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.code() == 200) {
                                Log.d("LOG", response.toString());
                            } else {
                                Log.e("LOG", response.message());
                            }
                        },
                        error -> Log.d("LOG", "ERROR", error),
                        () -> {
                        });
        videoView.pause();
        videoView.stopPlayback();
    }

    private void startPlayer(String videoPath) {
        try {
            mediacontroller = new MediaController(
                    this);
            mediacontroller.setAnchorView(videoView);
            // Get the URL from String VideoURL
            Uri video = Uri.parse(videoPath);
//            videoView.setMediaController(mediacontroller);
            videoView.setVideoURI(video);

        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        videoView.requestFocus();
        videoView.setOnPreparedListener(mp -> {
            videoView.start();
            mp.setLooping(true);

        });
    }
}
