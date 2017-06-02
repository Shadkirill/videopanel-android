package ru.com.videopanel.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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

import com.afollestad.easyvideoplayer.EasyVideoCallback;
import com.afollestad.easyvideoplayer.EasyVideoPlayer;

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
    private PlaylistDAO currentPlaylist;
    private EasyVideoPlayer videoView1;
    private EasyVideoPlayer videoView2;
    private EasyVideoPlayer currentVideoView;
    private EasyVideoPlayer nextVideoView;
    private ImageView imageView1;
    private ImageView imageView2;
    private ImageView currentImageView;
    private ImageView nextImageView;

    private View currentView;
    private int currentPlayItem = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBar();
        setContentView(R.layout.activity_show);
        hideNavigationButtons();
    }

    private void hideStatusBar() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void hideNavigationButtons() {
        getSupportActionBar().hide();
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void initViewsBeforeStart() {
        videoView1 = (EasyVideoPlayer) findViewById(R.id.videoView);
        videoView2 = (EasyVideoPlayer) findViewById(R.id.videoView2);
        currentVideoView = videoView1;
        nextVideoView = videoView2;
        imageView1 = (ImageView) findViewById(R.id.imageView);
        imageView2 = (ImageView) findViewById(R.id.imageView2);
        currentImageView = imageView1;
        nextImageView = imageView2;

        currentView = currentImageView;
        currentImageView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(this, UpdateService.class));
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopService(new Intent(this, UpdateService.class));
        EventBus.getDefault().unregister(this);
    }


    private void crossFadeViews(View from, View to) {
        int animationDuration = 2000;
        to.setAlpha(0f);
        to.setVisibility(View.VISIBLE);
        to.animate()
                .alpha(1f)
                .setDuration(animationDuration)
                .setListener(null);

        from.animate()
                .alpha(0f)
                .setDuration(animationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        from.setVisibility(View.GONE);
                    }
                });
        currentView = to;
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
        if (currentPlaylist != null && currentPlaylist.getId() != null) {
            currentPlayItem++;
            if (currentPlayItem != -1 && currentPlayItem < currentPlaylist.getItems().size()) {
                ItemDAO nextContent = currentPlaylist.getItems().get(currentPlayItem);
                if (nextContent.getItemType().equals(ItemDAO.TYPE_VIDEO)) {
                    showVideo(nextContent);
                } else if (nextContent.getItemType().equals(ItemDAO.TYPE_IMAGE)) {
                    showImage(nextContent);
                }

            } else {
                currentPlayItem = -1;
                //TODO send logs
                //TODO if we have time shownext if not get play
                getPlaylist();
            }
        }
    }

    private void showImage(ItemDAO nextContent) {
        nextImageView.setImageURI(Uri.parse(FileSystem.getFilePath(getFilesDir(), currentPlaylist.getId(), nextContent.getUrl())));
        crossFadeViews(currentView, nextImageView);

        swapImageViews();

        Handler playlistHandler = new Handler();
        playlistHandler.postDelayed(this::showNextContent, nextContent.getDuration() * 1000);
    }

    private void swapImageViews() {
        ImageView tmpImageView = currentImageView;
        currentImageView = nextImageView;
        nextImageView = tmpImageView;
    }

    private void showVideo(ItemDAO nextContent) {
        Uri video = Uri.parse(FileSystem.getFilePath(getFilesDir(), currentPlaylist.getId(), nextContent.getUrl()));

        nextVideoView.setSource(video);
        nextVideoView.setAutoPlay(true);
        try {
            nextVideoView.disableControls();
        } catch (NullPointerException e) {
            //Do nothing
        }
        nextVideoView.setAutoFullscreen(true);
        nextVideoView.setCallback(new EasyVideoCallback() {
            @Override
            public void onStarted(EasyVideoPlayer player) {

            }

            @Override
            public void onPaused(EasyVideoPlayer player) {

            }

            @Override
            public void onPreparing(EasyVideoPlayer player) {

            }

            @Override
            public void onPrepared(EasyVideoPlayer player) {
            }

            @Override
            public void onBuffering(int percent) {
            }

            @Override
            public void onError(EasyVideoPlayer player, Exception e) {
                showNextContent();
                //TODO check error
            }

            @Override
            public void onCompletion(EasyVideoPlayer player) {
                player.pause();
                showNextContent();
            }

            @Override
            public void onRetry(EasyVideoPlayer player, Uri source) {

            }

            @Override
            public void onSubmit(EasyVideoPlayer player, Uri source) {

            }
        });
        crossFadeViews(currentView, nextVideoView);
        swapVideoViews();
    }

    private void swapVideoViews() {
        EasyVideoPlayer tmpVideoView = currentVideoView;
        currentVideoView = nextVideoView;
        nextVideoView = tmpVideoView;
    }

    private void showNothing() {
        goneView(currentView);
    }

    public void goneView(View view) {
        view.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        start();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        if (event.getCommand().equals("stop"))
            rerun();
    }

    private void rerun() {
        stop();
        start();
    }

    private void start() {
        initViewsBeforeStart();
        Handler playlistHandler = new Handler();
        playlistHandler.postDelayed(this::getPlaylist, 5 * 1000);
    }

    private void stop() {
        currentVideoView.stop();
        currentVideoView.setVisibility(View.GONE);
        nextVideoView.stop();
        nextVideoView.setVisibility(View.GONE);
        currentImageView.setVisibility(View.GONE);
        nextImageView.setVisibility(View.GONE);

        showNothing();
        currentPlaylist = null;
        currentPlayItem = -1;
        new PreferenceUtil(this).setCurrentPlaylistId("-1");
    }
}
