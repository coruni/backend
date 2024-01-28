package com.TypeApi.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.TypeApi.entity.Apiconfig;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
public class ImageProcessor {
    private ExecutorService executor = Executors.newFixedThreadPool(5); // 创建一个拥有5个线程的线程池

    public void compressAndSaveImage(MultipartFile file, String decodeClassespath, String filetype, String newfile, int year, int month, int day) {
        executor.submit(() -> {
            try {
                byte[] compressedImageData = ImageUtils.compressImage(file.getBytes(), "jpg", 0.8f);
                File outputFile = new File(decodeClassespath + "/static/upload/" + "/" + year + "/" + month + "/" + day + "/" + newfile + "_compress" + filetype);
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(compressedImageData);
                }
                if (!outputFile.exists()) {
                    outputFile.mkdirs();
                }
                // 在压缩完成后，可以在这里添加压缩成功后的操作，比如保存到数据库或者返回信息给前端
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}