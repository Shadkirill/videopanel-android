package ru.com.videopanel.api;


import java.util.List;

import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
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

    @FormUrlEncoded
    @POST("report/error")
    Observable<Response<Void>> reportError(@Field("terminal_id") String terminalId, @Field("playlist_id") String plaulistId, @Field("date") String date, @Field("error") String error);

    @FormUrlEncoded
    @POST("report")
    Observable<Response<Void>> reportPlaylist(@Query("token") String token, @Field("playlist_id") String playlistId, @Field("date") String date);

}
