package com.TypeApi.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import org.springframework.web.multipart.MultipartFile;
public class ImageProcessor {
    private ExecutorService executor = Executors.newFixedThreadPool(5); // 创建一个拥有5个线程的线程池

    public void compressAndSaveImage(MultipartFile file, String decodeClassespath, String newfile, int year, int month, int day) {
        executor.submit(() -> {
            try {
                // 将 MultipartFile 写入到一个具体的文件
                File tempFile = File.createTempFile("temp", null); // 创建临时文件
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(file.getBytes()); // 写入 MultipartFile 的字节数据
                }

                // 然后进行压缩处理
                byte[] compressedImageData = ImageUtils.compressImage(file.getBytes(), 0.8f);
                File outputFile = new File(decodeClassespath + "/static/upload/" + "/" + year + "/" + month + "/" + day + "/" + newfile + "_compress.webp");
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(compressedImageData);
                }
                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }
                // 删除临时文件
                tempFile.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}