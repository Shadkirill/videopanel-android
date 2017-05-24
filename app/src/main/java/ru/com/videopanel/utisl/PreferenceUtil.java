package ru.com.videopanel.utisl;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import static android.content.Context.MODE_PRIVATE;

public class PreferenceUtil {
    private static final String PREFERENCE_NAME = "ru.com.videopanel";
    private static final String LOGIN = "login";
    private static final String PASSWORD = "password";
    private static final String IS_DATA_LOADED = "is_data_loaded";


    private SharedPreferences prefs = null;

    public PreferenceUtil(Context context) {
        prefs = context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
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
