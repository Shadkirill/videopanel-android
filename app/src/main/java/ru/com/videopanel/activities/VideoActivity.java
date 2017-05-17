package ru.com.videopanel.activities;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.MediaController;
import android.widget.VideoView;

import ru.com.videopanel.R;

public class VideoActivity extends AppCompatActivity {
    VideoView videoView;
    MediaController mediacontroller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_video);

        getSupportActionBar().hide();
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        videoView = (VideoView) findViewById(R.id.videoView);

        try {
            Bundle b = getIntent().getExtras();
            String url = b.getString("url");
            Uri video = Uri.parse(url);
            videoView.setVideoURI(video);

        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        videoView.requestFocus();
        videoView.setOnPreparedListener(mp -> {
            videoView.start();
//            mp.setLooping(true);

        });

        videoView.setOnCompletionListener(mp -> {
            setResult(RESULT_OK);
            finish();
        });

    }

    public void videoClick(View view) {

    }
}
