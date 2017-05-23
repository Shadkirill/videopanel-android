package ru.com.videopanel.api;


import java.util.List;

import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import ru.com.videopanel.models.Playlist;
import ru.com.videopanel.models.PlaylistInfo;

/**
 * Retrofit api interface
 */
public interface VideoService {
    @GET("playlists")
    Observable<List<PlaylistInfo>> playlists();

    @GET("playlist/{id}")
    Observable<Playlist> playlistData(@Path("id") int groupId);

    @FormUrlEncoded
    @POST("report/error")
    Observable<Response<Void>> reportError(@Field("terminal_id") String terminalId, @Field("playlist_id") String plaulistId, @Field("date") String date, @Field("error") String error);

    @FormUrlEncoded
    @POST("report")
    Observable<Response<Void>> reportPlaylist(@Field("playlist_id") String playlistId, @Field("date") String date);

}
