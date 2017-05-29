package ru.com.videopanel.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.com.videopanel.R;
import ru.com.videopanel.api.ServiceGenerator;
import ru.com.videopanel.api.VideoService;
import ru.com.videopanel.db.DBHelper;
import ru.com.videopanel.update.UpdateService;
import ru.com.videopanel.utisl.PreferenceUtil;

public class StartActivity extends AppCompatActivity {
    int startDelay;
    Observable<Long> longObservable = Observable.timer(startDelay, TimeUnit.SECONDS).doOnNext((next) -> {
        Intent intent = new Intent(this, ShowActivity.class);
        finish();
        startActivity(intent);
    });
    private String token;
    private View login_layout;
    private View start_layout;
    private EditText loginEdit;
    private EditText passwordEdit;
    private EditText urlEdit;
    private Button startPanel;
    private PreferenceUtil preferenceUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        login_layout = findViewById(R.id.login_layout);
        start_layout = findViewById(R.id.start_layout);
        preferenceUtil = new PreferenceUtil(this);

        loginEdit = (EditText) findViewById(R.id.login_edit);
        passwordEdit = (EditText) findViewById(R.id.password_edit);
        urlEdit = (EditText) findViewById(R.id.urlText);

        urlEdit.setText(preferenceUtil.getUrl());

        if (preferenceUtil.isLogin()) {
            startDelay = 0;
            showStart();
        } else {
            startDelay = 3;
            showLogin();
        }
    }

    public void onLoginClick(View view) {

        preferenceUtil.setURL(urlEdit.getText().toString());

        String login = loginEdit.getText().toString();
        if (TextUtils.isEmpty(login)) {
            showErrorAlert(R.string.login_missed);
            return;
        }

        String password = passwordEdit.getText().toString();
        if (TextUtils.isEmpty(password)) {
            showErrorAlert(R.string.password_missed);
            return;
        }

        VideoService service = ServiceGenerator.createService(VideoService.class);
        service.login(login, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        (receivedToken) -> {
                            token = receivedToken.getToken();
                            preferenceUtil.setLoginAndPassword(login, password);
                            showStart();
                        },
                        error -> {
                            //TODO Log error
                            Log.d("LOG", "ERROR", error);
                            showErrorAlert(R.string.incorrect_data);
                        },
                        () -> {
                        }
                );
    }

    private void showErrorAlert(int errorMessageStringResource) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(errorMessageStringResource)
                .setTitle(R.string.error)
                .setPositiveButton(R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showStart() {
//        login_layout.setVisibility(View.GONE);
//        start_layout.setVisibility(View.VISIBLE);
        startUpdateService();
        onStartPanel(null);
    }

    private void showLogin() {
        login_layout.setVisibility(View.VISIBLE);
        start_layout.setVisibility(View.GONE);
    }

    private void startUpdateService() {
//        Toast.makeText(this, "Загрузка плейлистов началась. Это может занять время", Toast.LENGTH_LONG).show();

        if (preferenceUtil.isDataLoaded()) {
            startService(new Intent(this, UpdateService.class));
        } else {
            VideoService service = ServiceGenerator.createService(VideoService.class);

            service.playlists(token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(Observable::fromIterable)
                    .flatMap(playlistInfo ->
                            service.playlistData(playlistInfo.getId(), token).subscribeOn(Schedulers.io())
                    )
                    .map(playlist -> {
                        DBHelper.addPlaylist(playlist);
                        return playlist;
                    })
                    .doOnComplete(() -> {
                        service.logout(token).subscribeOn(Schedulers.io()).subscribe(
                                (playlist) -> {
                                    Log.d("LOG", String.valueOf(playlist.code()));
                                },
                                error -> Log.d("LOG", "ERROR", error),
                                () -> {
                                }
                        );
                        preferenceUtil.setDataLoaded();
                        startUpdateService();
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            (playlist) -> {
                            },
                            error -> Log.d("LOG", "ERROR", error),
                            () -> {
                            }
                    );
        }
    }

    public void onLogoutClick(View view) {
        VideoService service = ServiceGenerator.createService(VideoService.class);
        service.logout(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        (response) -> {
                            Log.d("LOG", String.valueOf(response.code()));
                            preferenceUtil.removeLoginAndPassword();
                            showLogin();
                        },
                        error -> Log.d("LOG", "ERROR", error),
                        () -> {
                            preferenceUtil.removeLoginAndPassword();
                            showLogin();
                        }
                );
    }

    public void onStartPanel(View view) {
        Intent intent = new Intent(this, ShowActivity.class);
        finish();
        startActivity(intent);
    }
}
