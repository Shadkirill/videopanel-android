package ru.com.videopanel.api;

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
     * Create typed retrofit service.
     *
     * @param serviceClass Retrofit interface .class
     * @return Service object typed of serviceClass parameter
     */
    public static <S> S createService(Class<S> serviceClass) {
        Retrofit.Builder builder =
                new Retrofit.Builder()
                        .baseUrl(new PreferenceUtil(App.getAppContext()).getUrl())
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())

                        .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();

        return retrofit.create(serviceClass);
    }
}