package ru.com.videopanel.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.com.videopanel.R;
import ru.com.videopanel.api.NetworkUtil;
import ru.com.videopanel.api.ServiceGenerator;
import ru.com.videopanel.api.VideoService;
import ru.com.videopanel.update.UpdateService;
import ru.com.videopanel.utils.PreferenceUtil;

public class StartActivity extends AppCompatActivity {
    int startDelay;
    Observable<Long> longObservable = Observable.timer(startDelay, TimeUnit.SECONDS).doOnNext((next) -> {
        Intent intent = new Intent(this, ShowActivity.class);
        finish();
        startActivity(intent);
    });
    private String token;
    private View login_layout;
    private EditText loginEdit;
    private EditText passwordEdit;
    private EditText urlEdit;
    private PreferenceUtil preferenceUtil;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_start);
        getSupportActionBar().hide();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Идет проверка данных");
        dialog.setCancelable(false);

        TextView versionText = (TextView) findViewById(R.id.version_text);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;

            int verCode = pInfo.versionCode;
            versionText.setText("Клиент VideoPanel, версия " + version + " (#" + String.valueOf(verCode) + ")");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        login_layout = findViewById(R.id.login_layout);
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

        if (NetworkUtil.isOnline(this)) {
            dialog.show();
            VideoService service = ServiceGenerator.createService(VideoService.class);
            service.login(login, password)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            (receivedToken) -> {
                                token = receivedToken.getToken();
                                preferenceUtil.setLoginAndPassword(login, password);
                                service.logout(token).subscribeOn(Schedulers.io()).subscribe(
                                        (playlist) -> {
                                            Log.d("LOGOUT", String.valueOf(playlist.code()));
                                        },
                                        error -> Log.d("LOGOUT", "ERROR", error),
                                        () -> {
                                        }
                                );
                                dialog.dismiss();
                                showStart();
                            },
                            error -> {
                                //TODO Log error
                                dialog.dismiss();
                                Log.d("LOG", "ERROR", error);
                                showAlternativeErrorAlert(R.string.incorrect_data);
                            },
                            () -> {
                            }
                    );
        } else {
            showAlternativeErrorAlert(R.string.no_internet_connection);
            Log.e("STARTACTIVITY", "No internet connection");
        }
    }

    private void showErrorAlert(int errorMessageStringResource) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(errorMessageStringResource)
                .setTitle(R.string.error)
                .setPositiveButton(R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showAlternativeErrorAlert(int errorMessageStringResource) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(errorMessageStringResource)
                .setTitle(R.string.error)
                .setPositiveButton("Изменить данные", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("Сохранить все равно", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferenceUtil.setLoginAndPassword(loginEdit.getText().toString(), passwordEdit.getText().toString());
                        showStart();

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showStart() {
        startUpdateService();
        onStartPanel(null);
    }

    private void showLogin() {
        login_layout.setVisibility(View.VISIBLE);
    }

    private void startUpdateService() {
        startService(new Intent(this, UpdateService.class));
    }

    public void onStartPanel(View view) {
        Intent intent = new Intent(this, ShowActivity.class);
        finish();
        startActivity(intent);
    }
}
