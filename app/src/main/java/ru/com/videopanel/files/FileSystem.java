package ru.com.videopanel.files;

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
import java.util.zip.CRC32;

public class FileSystem {

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

    public String saveFile(File filedir, String url, long crc32) throws IOException {
        URL u = new URL(url);
        InputStream is = u.openStream();

        DataInputStream dis = new DataInputStream(is);

        byte[] buffer = new byte[1024];
        int length;
        String path = filedir + "/" + generateUniqueFileName();
        FileOutputStream fos = new FileOutputStream(new File(path));
        while ((length = dis.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }
        if (crc32 == 0 || crc32 == checksumMappedFile(path)) {
            return path;
        } else {
//            throw new IOException("AAAA");
            return path;
            //TODO log error
        }
    }

    private String generateUniqueFileName() {
        String filename;
        long millis = System.currentTimeMillis();
        String datetime = new Date().toGMTString();
        datetime = datetime.replace(" ", "");
        datetime = datetime.replace(":", "");
        String rndChars = RandomStringUtils.randomAlphanumeric(16);
        filename = rndChars + "_" + datetime + "_" + millis;
        return filename;
    }
}
