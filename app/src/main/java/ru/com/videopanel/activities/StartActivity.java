package ru.com.videopanel.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.apache.commons.lang.RandomStringUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.List;
import java.util.zip.CRC32;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.com.videopanel.R;
import ru.com.videopanel.api.ServiceGenerator;
import ru.com.videopanel.api.VideoService;
import ru.com.videopanel.db.DBHelper;
import ru.com.videopanel.models.Item;

public class StartActivity extends AppCompatActivity {
    private String token;
    private View login_layout;
    private View start_layout;

    private SharedPreferences prefs = null;

    public static long checksumMappedFile(String filepath) throws IOException {
        FileInputStream inputStream = new FileInputStream(filepath);
        FileChannel fileChannel = inputStream.getChannel();
        int len = (int) fileChannel.size();
        MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, len);
        CRC32 crc = new CRC32();
        for (int cnt = 0; cnt < len; cnt++) {
            int i = buffer.get(cnt);
            crc.update(i);
        }
        return crc.getValue();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        login_layout = findViewById(R.id.login_layout);
        start_layout = findViewById(R.id.start_layout);
        prefs = getSharedPreferences("ru.com.videopanel", MODE_PRIVATE);

        if (TextUtils.isEmpty(prefs.getString("login", null))) {
            showLogin();
        } else {
            showStart();
        }
    }

    private void showStart() {
        login_layout.setVisibility(View.GONE);
        start_layout.setVisibility(View.VISIBLE);
    }

    private void showLogin() {
        login_layout.setVisibility(View.VISIBLE);
        start_layout.setVisibility(View.GONE);
    }

    public void onGreenClick(View view) {
        Toast.makeText(this, "Загрузка плейлистов началась. Это может занять время", Toast.LENGTH_LONG).show();
        VideoService service = ServiceGenerator.createService(VideoService.class);

        service.login("AA", "A")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(receivedToken -> {
                    token = receivedToken.getToken();
                    return service.playlists(token).subscribeOn(Schedulers.io());
                })
                .flatMap(Observable::fromIterable)
                .flatMap(playlistInfo ->
                        service.playlistData(playlistInfo.getId(), token).subscribeOn(Schedulers.io())
                )
                .map(playlist -> {
                    List<Item> items = playlist.getItems();
                    for (Item item : items) {
                        item.setUrl(saveFile(item.getUrl(), item.getCrc32() != null ? Long.parseLong(item.getCrc32(), 32) : 0));
                    }
                    playlist.setItems(items);
                    return playlist;
                })
                .map(playlist -> {
                    DBHelper.addPlaylist(playlist);
                    return playlist;
                })
                .doOnComplete(() -> {
                    service.logout(token).subscribeOn(Schedulers.io()).subscribe(
                            (playlist) -> {
                                Log.d("LOG", String.valueOf(playlist.code()));
                            },
                            error -> Log.d("LOG", "ERROR", error),
                            () -> {
                            }
                    );
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        (playlist) -> {
                        },
                        error -> Log.d("LOG", "ERROR", error),
                        () -> Toast.makeText(this, "Загрузка плейлистов окончена", Toast.LENGTH_LONG).show()
                );
    }

    public String saveFile(String url, long crc32) throws IOException {
        URL u = new URL(url);
        InputStream is = u.openStream();

        DataInputStream dis = new DataInputStream(is);

        byte[] buffer = new byte[1024];
        int length;
        String path = getFilesDir() + "/" + generateUniqueFileName();
        FileOutputStream fos = new FileOutputStream(new File(path));
        while ((length = dis.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }
        if (crc32 == 0 || crc32 == checksumMappedFile(path)) {
            return path;
        } else {
//            throw new IOException("AAAA");
            return path;//TODO return real crchex
        }
    }

    String generateUniqueFileName() {
        String filename;
        long millis = System.currentTimeMillis();
        String datetime = new Date().toGMTString();
        datetime = datetime.replace(" ", "");
        datetime = datetime.replace(":", "");
        String rndChars = RandomStringUtils.randomAlphanumeric(16);
        filename = rndChars + "_" + datetime + "_" + millis;
        return filename;
    }

    public void onRedClick(View view) {
        Intent intent = new Intent(this, ShowActivity.class);
        startActivity(intent);
    }


    public void onYellowClick(View view) {
    }
}
