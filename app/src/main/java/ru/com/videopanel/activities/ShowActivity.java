package ru.com.videopanel.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.VideoView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.com.videopanel.MessageEvent;
import ru.com.videopanel.R;
import ru.com.videopanel.db.DBHelper;
import ru.com.videopanel.db.dao.ItemDAO;
import ru.com.videopanel.db.dao.PlaylistDAO;
import ru.com.videopanel.files.FileSystem;
import ru.com.videopanel.update.UpdateService;
import ru.com.videopanel.utils.PreferenceUtil;

public class ShowActivity extends AppCompatActivity {
    Handler playlistHandler;
    Runnable r;
    private PlaylistDAO currentPlaylist;
    private VideoView videoView;
    private ImageView imageView;
    private int currentPlayItem = -1;

    public static void ImageViewAnimatedChange(Context c, final ImageView v, final String new_image) {
        v.setImageURI(Uri.parse(new_image));
//        final Animation anim_out = AnimationUtils.loadAnimation(c, android.R.anim.fade_out);
//        final Animation anim_in = AnimationUtils.loadAnimation(c, android.R.anim.fade_in);
//        anim_out.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
//
////                anim_in.setAnimationListener(new Animation.AnimationListener() {
////                    @Override
////                    public void onAnimationStart(Animation animation) {
////                    }
////
////                    @Override
////                    public void onAnimationRepeat(Animation animation) {
////                    }
////
////                    @Override
////                    public void onAnimationEnd(Animation animation) {
////                    }
////                });
////                v.startAnimation(anim_in);
//            }
//        });
//        v.startAnimation(anim_out);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_show);

        getSupportActionBar().hide();
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        videoView = (VideoView) findViewById(R.id.videoView);
        imageView = (ImageView) findViewById(R.id.imageView);

        getPlaylist();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopService(new Intent(this, UpdateService.class));
        EventBus.getDefault().unregister(this);
    }

    private void getPlaylist() {
        DBHelper.getCurrentPlaylist()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .firstElement()
                .subscribe(
                        playlist -> {
                            currentPlaylist = playlist;
                            if (currentPlaylist.getId() == null) {
                                showNothing();
                                Handler handler = new Handler();
                                handler.postDelayed(() -> {
                                    getPlaylist();
                                }, 5 * 1000);
                            } else {
                                new PreferenceUtil(this).setCurrentPlaylistId(playlist.getId());
                                showNextContent();
                            }
                        },
                        error -> Log.d("LOG", "ERROR", error),
                        () -> {

                        }
                );
    }

    private void showNextContent() {
        currentPlayItem++;
        if (currentPlayItem != -1 && currentPlayItem < currentPlaylist.getItems().size()) {
            ItemDAO nextContent = currentPlaylist.getItems().get(currentPlayItem);
            if (nextContent.getItemType().equals(ItemDAO.TYPE_VIDEO)) {
                goneView(imageView);
                visibleView(videoView);
                try {
                    Uri video = Uri.parse(FileSystem.getFilePath(getFilesDir(), currentPlaylist.getId(), nextContent.getUrl()));
                    videoView.setVideoURI(video);
                } catch (Exception e) {
                    Log.e("Error", e.getMessage());
                    e.printStackTrace();
                    showNextContent();
                }

                videoView.requestFocus();
                videoView.setOnPreparedListener(mp -> {
                    videoView.start();
                });

                videoView.setOnCompletionListener(mp -> {
                    videoView.suspend();
                    showNextContent();
                });
            } else if (nextContent.getItemType().equals(ItemDAO.TYPE_IMAGE)) {

                goneView(videoView);
                visibleView(imageView);
                ImageViewAnimatedChange(this, imageView, FileSystem.getFilePath(getFilesDir(), currentPlaylist.getId(), nextContent.getUrl()));

                playlistHandler = new Handler();
                r = () -> showNextContent();
                playlistHandler.postDelayed(r, nextContent.getDuration() * 1000);
            }

        } else {
            currentPlayItem = -1;
            //TODO send logs
            //TODO if we have time shownext if not get play
            getPlaylist();
        }
    }

    private void showNothing() {
        goneView(imageView);
        goneView(videoView);
    }

    public void goneView(View view) {
        view.setVisibility(View.GONE);
//        view.animate()
//                .alpha(0.0f)
//                .setDuration(500)
//                .setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//                        view.setVisibility(View.GONE);
//                    }
//                });
    }

    public void visibleView(View view) {
        view.setVisibility(View.VISIBLE);
//        view.animate()
//                .alpha(1.0f)
//                .setDuration(500)
//                .setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//
//                    }
//                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoView.suspend();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        if (event.getCommand().equals("stop")) {
            stop();
        } else if (event.getCommand().equals("start")) {
            start();
        }
    }

    private void start() {
        getPlaylist();
    }

    private void stop() {
        videoView.suspend();
        if (playlistHandler != null && r != null)
            playlistHandler.removeCallbacks(r);
        showNothing();
        currentPlaylist = null;
        currentPlayItem = -1;
    }
}
