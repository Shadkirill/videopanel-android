package ru.com.videopanel.api;


import java.util.HashMap;
import java.util.List;

import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.com.videopanel.models.Playlist;
import ru.com.videopanel.models.PlaylistInfo;
import ru.com.videopanel.models.Token;

/**
 * Retrofit api interface
 */
public interface VideoService {
    @GET("auth/login")
    Observable<Token> login(@Query("login") String login, @Query("password") String password);

    @GET("auth/logout")
    Observable<Response<Void>> logout(@Query("token") String token);

    @GET("playlists")
    Observable<List<PlaylistInfo>> playlists(@Query("token") String token);

    @GET("playlist/{id}")
    Observable<Playlist> playlistData(@Path("id") int groupId, @Query("token") String token);

    @POST("report/error")
    Observable<Response<Void>> reportError(@Body HashMap<String, Object> body);

    @POST("report/played/{id}")
    Observable<Response<Void>> reportPlaylist(@Header("token") String token, @Path("id") String playlistId, @Body HashMap<String, Object> body);

}
