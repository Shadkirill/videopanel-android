package ru.com.videopanel.activities;

import android.app.ProgressDialog;
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

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.com.videopanel.R;
import ru.com.videopanel.api.NetworkUtil;
import ru.com.videopanel.api.ServiceGenerator;
import ru.com.videopanel.api.VideoService;
import ru.com.videopanel.db.DBHelper;
import ru.com.videopanel.models.Token;
import ru.com.videopanel.utils.PreferenceUtil;

public class StartActivity extends AppCompatActivity {
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

        preferenceUtil = new PreferenceUtil(this);
        if (preferenceUtil.isLogin()) {
            startShow();
        }

        dialog = new ProgressDialog(this);
        dialog.setMessage("Идет проверка данных");
        dialog.setCancelable(false);

        setApkVersion();


        loginEdit = (EditText) findViewById(R.id.login_edit);
        passwordEdit = (EditText) findViewById(R.id.password_edit);
        urlEdit = (EditText) findViewById(R.id.urlText);
        urlEdit.setText(preferenceUtil.getUrl());

//        loginEdit.setText("4");
//        passwordEdit.setText("lewnuBL4");
//        urlEdit.setText("https://videopanel.herokuapp.com/api/");
    }

    private void setApkVersion() {
        TextView versionText = (TextView) findViewById(R.id.version_text);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int verCode = pInfo.versionCode;
            versionText.setText("Клиент VideoPanel, версия " + version + " (#" + String.valueOf(verCode) + ")");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
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
                                saveCredentialsAndLogout(login, password, service, receivedToken);
                                startShow();
                            },
                            error -> {
                                dialog.dismiss();
                                Log.d("LOG", "ERROR", error);
                                DBHelper.addErrorReport("Login on start screen error", error);
                                showAlternativeErrorAlert(R.string.incorrect_data);
                            },
                            () -> {
                            }
                    );
        } else {
            showAlternativeErrorAlert(R.string.no_internet_connection);
            DBHelper.addErrorReport("No internet connection", null);
            Log.e("STARTACTIVITY", "No internet connection");
        }
    }

    private void saveCredentialsAndLogout(String login, String password, VideoService service, Token receivedToken) {
        preferenceUtil.setLoginAndPassword(login, password);
        service.logout(receivedToken.getToken()).subscribeOn(Schedulers.io()).subscribe(
                (playlist) -> {
                    Log.d("LOGOUT", String.valueOf(playlist.code()));
                },
                error -> {
                    DBHelper.addErrorReport("Logout error after first login", error);
                    Log.d("LOGOUT", "ERROR", error);
                },
                () -> {
                }
        );
        dialog.dismiss();
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
                .setPositiveButton(R.string.alert_positive_change_data, (dialog, which) -> {
                })
                .setNegativeButton(R.string.alert_negative_save_anyway, (dialog, which) -> {
                    preferenceUtil.setLoginAndPassword(
                            loginEdit.getText().toString(),
                            passwordEdit.getText().toString());
                    startShow();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void startShow() {
        Intent intent = new Intent(this, ShowActivity.class);
        finish();
        startActivity(intent);
    }
}
