package com.Fanbbs.web;

import com.Fanbbs.common.*;
import com.Fanbbs.entity.Apiconfig;
import com.Fanbbs.entity.Users;
import com.Fanbbs.service.ApiconfigService;
import com.Fanbbs.service.UploadService;
import com.Fanbbs.service.UsersService;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 * <p>
 * 提供本地和cos上传，之后的接口支持都加在这里
 */

@Controller
@RequestMapping(value = "/upload")
public class UploadController {

    @Value("${web.prefix}")
    private String dataprefix;


    @Autowired
    private ApiconfigService apiconfigService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private RedisTemplate redisTemplate;


    ResultAll Result = new ResultAll();

    UserStatus UStatus = new UserStatus();


    /***
     * 上传接口
     * @param files
     * @param request
     * @return
     * @throws IOException
     */

    @PostMapping(value = "/full")
    @ResponseBody
    public Object full(@RequestParam Map<String, MultipartFile> files,
                       HttpServletRequest request) throws IOException {
        String token = request.getHeader("Authorization");
        Users user = getUserFromToken(token);
        if (user == null || user.getUid() == null) {
            return Result.getResultJson(201, "用户不存在，请重新登录", null);
        }

        Map<String, Object> data = new HashMap<>();
        String image = null;
        List<Object> imageList = new ArrayList<>();

        // 遍历Map中的所有文件
        for (MultipartFile file : files.values()) {
            if (file != null && !file.isEmpty()) {
                Object result = handleSingleFile(file, user, this.dataprefix, apiconfigService, redisTemplate);
                if (result != null) {
                    if (image == null) {
                        image = (String) result;
                    } else {
                        imageList.add(result);
                    }
                }
            }
        }

        if (image == null && imageList.isEmpty()) {
            return Result.getResultJson(201, "请上传文件", null);
        }

        if (image != null) {
            data.put("url", image);
        }
        if (!imageList.isEmpty()) {
            data.put("urls", imageList);
        }

        return Result.getResultJson(200, "上传成功", data);
    }
    private Object handleSingleFile(MultipartFile file, Users user, String dataprefix, ApiconfigService apiconfigService, RedisTemplate<String, String> redisTemplate) throws IOException {
        Apiconfig apiconfig = UStatus.getConfig(dataprefix, apiconfigService, redisTemplate);
        Integer fileUploadLevel = apiconfig.getUploadLevel();
        String filename = file.getOriginalFilename();
        String extension = getExtensionWithoutDot(filename);

        if (!isAllowedFileType(extension, fileUploadLevel))
            return Result.getResultJson(201, fileUploadLevel.equals(0) ? "已关闭上传" : "文件类型不被允许", null);

        long maxSize = getMaxSizeForFileType(extension, apiconfig);
        if (file.getSize() > maxSize) {
            return Result.getResultJson(201, "文件大小超过限制", null);
        }

        String uploadType = apiconfig.getUploadType();
        if ("cos".equals(uploadType)) {
            return uploadService.cosUpload(file, dataprefix, apiconfig, user.getUid());
        } else if ("local".equals(uploadType)) {
            return uploadService.localUpload(file, dataprefix, apiconfig, user.getUid());
        } else if ("oss".equals(uploadType)) {
            return uploadService.ossUpload(file, dataprefix, apiconfig, user.getUid());
        } else if ("ftp".equals(uploadType)) {
            return uploadService.ftpUpload(file, dataprefix, apiconfig, user.getUid());
        } else if ("qiniu".equals(uploadType)) {
            return uploadService.qiniuUpload(file, dataprefix, apiconfig, user.getUid());
        } else {
            return Result.getResultJson(201, "未开启任何上传通道，请检查配置", null);
        }
    }

    private Users getUserFromToken(String token) {
        if (token != null && !token.isEmpty()) {
            try {
                DecodedJWT verify = JWT.verify(token);
                int userId = Integer.parseInt(verify.getClaim("aud").asString());
                return usersService.selectByKey(userId);
            } catch (Exception e) {
                // 处理异常
            }
        }
        return null;
    }

    private String getExtensionWithoutDot(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "png";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    private boolean isAllowedFileType(String extension, Integer fileUploadLevel) {
        if (fileUploadLevel == 0) {
            // 关闭上传功能
            return false;
        } else if (fileUploadLevel == 1) {
            // 只允许上传图片
            return isImageFile(extension);
        } else if (fileUploadLevel == 2) {
            // 允许上传图片和媒体文件
            return isImageFile(extension) || isMediaFile(extension);
        } else {
            // 允许上传所有类型文件
            return true;
        }
    }

    private long getMaxSizeForFileType(String extension, Apiconfig apiconfig) {
        Integer uploadPicMax = apiconfig.getUploadPicMax();
        Integer uploadMediaMax = apiconfig.getUploadMediaMax();
        Integer uploadFilesMax = apiconfig.getUploadFilesMax();
        if (isImageFile(extension)) {
            return uploadPicMax * 1024L * 1024L;
        } else if (isMediaFile(extension)) {
            return uploadMediaMax * 1024L * 1024L;
        } else {
            return uploadFilesMax * 1024L * 1024L;
        }
    }

    private boolean isImageFile(String extension) {
        return extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("gif") || extension.equalsIgnoreCase("bmp") || extension.equalsIgnoreCase("webp");
    }

    private boolean isMediaFile(String extension) {
        return extension.equalsIgnoreCase("mp4") || extension.equalsIgnoreCase("mov") || extension.equalsIgnoreCase("avi") || extension.equalsIgnoreCase("mp3");
    }
}
