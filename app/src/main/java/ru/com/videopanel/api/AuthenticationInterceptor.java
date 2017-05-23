package ru.com.videopanel.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Basic http authentication interceptor
 */
class AuthenticationInterceptor implements Interceptor {

    private String authToken;

    /**
     * Initialization interceptor
     *
     * @param token Http authentication string token
     */
    AuthenticationInterceptor(String token) {
        this.authToken = token;
    }
    /**
     * Authorization Add Authorization header on the request
     */
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        Request.Builder builder = original.newBuilder()
                .header("Authorization", authToken);

        Request request = builder.build();
        return chain.proceed(request);
    }
}