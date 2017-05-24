package ru.com.videopanel.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.VideoView;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import ru.com.videopanel.R;
import ru.com.videopanel.db.DBHelper;
import ru.com.videopanel.db.dao.ItemDAO;
import ru.com.videopanel.db.dao.PlaylistDAO;

public class ShowActivity extends AppCompatActivity {
    private PlaylistDAO currentPlaylist;
    private VideoView videoView;
    private ImageView imageView;
    private int currentPlayItem = -1;
    private Disposable subscribe;

    public static void ImageViewAnimatedChange(Context c, final ImageView v, final String new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, android.R.anim.fade_out);
        final Animation anim_in = AnimationUtils.loadAnimation(c, android.R.anim.fade_in);
        anim_out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setImageURI(Uri.parse(new_image));
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);

        getSupportActionBar().hide();
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        videoView = (VideoView) findViewById(R.id.videoView);
        imageView = (ImageView) findViewById(R.id.imageView);


        subscribe = Observable.interval(1, TimeUnit.SECONDS)
                .subscribe((playlist) -> {
                            Log.d("AAA", playlist.toString());
                        },
                        error -> Log.d("LOG", "ERROR", error)
                );

//        getPlaylist();

        planPlaylists();
    }

    @Override
    protected void onStop() {
        super.onStop();
        subscribe.dispose();
    }

    private void planPlaylists() {


    }

    private void getPlaylist() {
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
                goneView(imageView);
                visibleView(videoView);
                try {
                    Uri video = Uri.parse(nextContent.getUrl());
                    videoView.setVideoURI(video);

                } catch (Exception e) {
                    Log.e("Error", e.getMessage());
                    e.printStackTrace();
                }

                videoView.requestFocus();
                videoView.setOnPreparedListener(mp -> {
                    videoView.start();
                });

                videoView.setOnCompletionListener(mp -> {
                    showNextContent();
                });
            } else if (nextContent.getItemType().equals(ItemDAO.TYPE_IMAGE)) {

                videoView.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
                ImageViewAnimatedChange(this, imageView, nextContent.getUrl());
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    setResult(RESULT_OK);
                    showNextContent();
                    //TODO nextContent.getDuration()
                }, 3 * 1000);
            }

        } else {
            currentPlayItem = -1;
            //TODO send logs
            finish();
        }
    }

    public void goneView(View view) {
        view.animate()
                .alpha(0.0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        view.setVisibility(View.GONE);
                    }
                });
    }

    public void visibleView(View view) {
        view.animate()
                .alpha(1.0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        view.setVisibility(View.VISIBLE);
                    }
                });
    }
}
