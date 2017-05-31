package ru.com.videopanel.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;

import ru.com.videopanel.App;

import static android.content.Context.MODE_PRIVATE;

public class PreferenceUtil {
    private static final String PREFERENCE_NAME = "ru.com.videopanel";
    private static final String LOGIN = "login";
    private static final String PASSWORD = "password";
    private static final String IS_DATA_LOADED = "is_data_loaded";
    private static final String URL = "url";
    private static final String API_MOCK_URL = "http://videopanel.getsandbox.com/";
    private static final String API_DEFAULT_URL = "https://videopanel.herokuapp.com/api/";
    private static final String API_URL = API_DEFAULT_URL;//isDebug ? API_MOCK_URL : API_DEFAULT_URL;
    private static boolean isDebug = (0 != (App.getAppContext().getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
    private SharedPreferences prefs = null;

    public PreferenceUtil(Context context) {
        prefs = context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
    }

    public String getUrl() {
        return prefs.getString(URL, API_URL);
    }

    public void setURL(String url) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(URL, url);
        edit.apply();
    }

    public boolean isLogin() {
        return !TextUtils.isEmpty(getLogin()) && !TextUtils.isEmpty(getPassword());
    }

    public String getLogin() {
        return prefs.getString(LOGIN, null);
    }

    public String getPassword() {
        return prefs.getString(PASSWORD, null);
    }

    public void setLoginAndPassword(String login, String password) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(LOGIN, login);
        edit.putString(PASSWORD, password);
        edit.apply();
    }

    public void removeLoginAndPassword() {
        SharedPreferences.Editor edit = prefs.edit();
        edit.remove(LOGIN);
        edit.remove(PASSWORD);
        edit.apply();
    }

    public boolean isDataLoaded() {
        return prefs.getBoolean(IS_DATA_LOADED, false);
    }

    public void setDataLoaded() {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(IS_DATA_LOADED, true);
        edit.apply();
    }
}
