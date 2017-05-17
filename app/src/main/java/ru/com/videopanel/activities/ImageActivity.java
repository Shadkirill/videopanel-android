package ru.com.videopanel.activities;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import ru.com.videopanel.R;

public class ImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_picture);

        getSupportActionBar().hide();
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        ImageView imageView = (ImageView) findViewById(R.id.imageView);

        Bundle b = getIntent().getExtras();
        String url = b.getString("url");
        Long duration = b.getLong("duration");
        Picasso.with(this).load(url).into(imageView);

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            setResult(RESULT_OK);
            finish();
        }, duration * 1000);

    }
}
