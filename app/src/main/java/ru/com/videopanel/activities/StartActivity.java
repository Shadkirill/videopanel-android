package ru.com.videopanel.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

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
import ru.com.videopanel.db.dao.ItemDAO;
import ru.com.videopanel.db.dao.PlaylistDAO;
import ru.com.videopanel.models.Item;
import ru.com.videopanel.models.PlaylistInfo;

public class StartActivity extends AppCompatActivity {

    private PlaylistDAO currentPlaylist;
    private int currentPlayItem = -1;

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
    }

    public void onGreenClick(View view) {
        VideoService service = ServiceGenerator.createService(VideoService.class, "username", "password");

        Observable<List<PlaylistInfo>> playlistInfoObservable = service.playlists();
        playlistInfoObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(Observable::fromIterable)
                .flatMap(playlistInfo ->
                        service.playlistData(playlistInfo.getId()).subscribeOn(Schedulers.io())
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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((playlist) -> {
                        },
                        error -> Log.d("LOG", "ERROR", error),
                        () -> {
                        }
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
        DBHelper.getAllPlaylist()
                .firstElement()
                .subscribe(
                        playlist -> {
                            Log.d("LOG", playlist.getLastUpdated());
                            currentPlaylist = playlist;
                            showNextContent();

                        },
                        error -> Log.d("LOG", "ERROR", error),
                        () -> {
                        }
                );
    }

    private void showNextContent() {
        currentPlayItem++;
        if (currentPlayItem < currentPlaylist.getItems().size()) {
            ItemDAO nextContent = currentPlaylist.getItems().get(currentPlayItem);
            if (nextContent.getItemType().equals(ItemDAO.TYPE_VIDEO)) {
                startVideoActivity(nextContent.getUrl());
            } else if (nextContent.getItemType().equals(ItemDAO.TYPE_IMAGE)) {
                startImageActivity(nextContent.getUrl(), nextContent.getDuration());
            }

        } else {
            currentPlayItem = -1;
            //TODO send logs
        }
    }

    public void onYellowClick(View view) {
//        startImageActivity();
    }

    private void startVideoActivity(String url) {
        Intent intent = new Intent(this, VideoActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        intent.putExtras(bundle);
        startActivityForResult(intent, 11);
    }

    private void startImageActivity(String url, Long duration) {
        Intent intent = new Intent(this, ImageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        bundle.putLong("duration", duration);
        intent.putExtras(bundle);
        startActivityForResult(intent, 11);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 11) {
            if (resultCode == RESULT_OK) {
                showNextContent();
            }
        }

    }
}
