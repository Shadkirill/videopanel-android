package ru.com.videopanel.api;

import android.text.TextUtils;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.com.videopanel.App;
import ru.com.videopanel.utils.PreferenceUtil;

/**
 * Retrofit service generator
 */
public class ServiceGenerator {
    /**
     * Backend server base URL
     */
    private static final String API_BASE_URL = "http://videopanel.getsandbox.com/";

    /**
     * Custom http client for retrofit
     */
    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
//
//    /**
//     * Retrofit service builder
//     */
//    private static Retrofit.Builder builder =
//            new Retrofit.Builder()
//                    .baseUrl(new PreferenceUtil(App.getAppContext()).getUrl())
//                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//                    .addConverterFactory(GsonConverterFactory.create());
//
//    /**
//     * Retrofit service
//     */
//    private static Retrofit retrofit = builder.build();

    /**
     * Create typed retrofit service
     *
     * @param serviceClass Retrofit interface .class
     * @return Service object typed of serviceClass parameter
     */
    public static <S> S createService(Class<S> serviceClass) {
        return createService(serviceClass, null, null);
    }

    /**
     * Create typed retrofit service with basic authentication
     * @param serviceClass Retrofit interface .class
     * @param username HTTP basic username
     * @param password HTTP basic password
     * @return Service object typed of serviceClass parameter
     */
    public static <S> S createService(
            Class<S> serviceClass, String username, String password) {
        if (!TextUtils.isEmpty(username)
                && !TextUtils.isEmpty(password)) {
            String authToken = Credentials.basic(username, password);
            return createService(serviceClass, authToken);
        }

        return createService(serviceClass, null);
    }

//    public static void chengeURL(String s){
//        builder = new Retrofit.Builder()
//                .baseUrl(new PreferenceUtil(App.getAppContext()).getUrl())
//                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//                .addConverterFactory(GsonConverterFactory.create());
//    }

    /**
     * Create typed retrofit service with basic authentication
     * @param serviceClass Retrofit interface .class
     * @param authToken HTTP basic auth token
     * @return Service object typed of serviceClass parameter
     */
    private static <S> S createService(
            Class<S> serviceClass, final String authToken) {

//        if (!TextUtils.isEmpty(authToken)) {
//            AuthenticationInterceptor interceptor =
//                    new AuthenticationInterceptor(authToken);
//
//            if (!httpClient.interceptors().contains(interceptor)) {
//                httpClient.addInterceptor(interceptor);
//
//
//                builder.client(httpClient.build());
//
//                retrofit = builder.build();
//            }
//        }

        Retrofit.Builder builder =
                new Retrofit.Builder()
                        .baseUrl(new PreferenceUtil(App.getAppContext()).getUrl())
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())

                        .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();

        return retrofit.create(serviceClass);
    }
}