package ru.com.videopanel.files;

import android.webkit.URLUtil;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;

public class FileSystem {
    private static final String DIRECTORY_DOWNLOADING = "downloading";
    private static final String DIRECTORY_PRODUCTION = "production";

    public static String getFilePath(File filedir, String playlistId, String file) {
        return "file://" + filedir.getPath() + "/" + DIRECTORY_PRODUCTION + "/" + playlistId + "/" + file;
    }

    private static void copyFileOrDirectory(String srcDir, String dstDir) {
        try {
            File src = new File(srcDir);
            File dst = new File(dstDir);

            if (src.isDirectory()) {

                String files[] = src.list();
                int filesLength = files.length;
                for (int i = 0; i < filesLength; i++) {
                    String src1 = (new File(src, files[i]).getPath());
                    String dst1 = (new File(dst, files[i]).getPath());
                    copyFileOrDirectory(src1, dst1);

                }
            } else {
                copyFile(src, dst);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    //TODO add crc check
    private static long checksumMappedFile(String filepath) throws IOException {
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

    private void downloadFileFromInternet(String url, String direction, String crc32) throws IOException {
        URL u = new URL(url);
        InputStream is = u.openStream();
        DataInputStream dis = new DataInputStream(is);
        byte[] buffer = new byte[1024];
        int length;
        File newFile = new File(direction);
        if (!newFile.getParentFile().exists())
            newFile.getParentFile().mkdirs();

        if (!newFile.exists()) {
            newFile.createNewFile();
        }

        FileOutputStream fos = new FileOutputStream(newFile, true);
        while ((length = dis.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }
        //TODO add crc
//        if (crc32 != 0 && crc32 != checksumMappedFile(direction)) {
////            throw new IOException("Incorrect CRC check");
//            return;
//        }
    }

    public String saveFile(File filedir, String playlistId, String url, String crc32) throws IOException {
        String productionFileName = filedir.getPath() + "/" + DIRECTORY_PRODUCTION + "/" + playlistId + "/" + URLUtil.guessFileName(url, null, null);
        String downloadingFileName = filedir.getPath() + "/" + DIRECTORY_DOWNLOADING + "/" + playlistId + "/" + URLUtil.guessFileName(url, null, null);
        if (new File(productionFileName).exists()) {
            copyFile(new File(productionFileName), new File(downloadingFileName));
        } else {
            downloadFileFromInternet(url, downloadingFileName, crc32);
        }
        return URLUtil.guessFileName(url, null, null);
    }

    public void replacePlaylistFilesToProduction(File filedir, String playlistId) {
        String productionPlaylistDir = filedir.getPath() + "/" + DIRECTORY_PRODUCTION + "/" + playlistId;
        String downloadingPlaylistDir = filedir.getPath() + "/" + DIRECTORY_DOWNLOADING + "/" + playlistId;
        deleteRecursive(new File(productionPlaylistDir));
        copyFileOrDirectory(downloadingPlaylistDir, productionPlaylistDir);
        deleteRecursive(new File(downloadingPlaylistDir));
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }
}
