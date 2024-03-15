package com.TypeApi.service;

import com.TypeApi.entity.Apiconfig;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UploadService {


    String cosUpload(MultipartFile file, String  dataprefix, Apiconfig apiconfig, Integer uid);
    String localUpload(MultipartFile file, String  dataprefix, Apiconfig apiconfig, Integer uid) throws IOException;
    String ossUpload(MultipartFile file, String  dataprefix, Apiconfig apiconfig, Integer uid);
    String qiniuUpload(MultipartFile file, String  dataprefix, Apiconfig apiconfig, Integer uid);
    String ftpUpload(MultipartFile file, String  dataprefix, Apiconfig apiconfig, Integer uid);
}
