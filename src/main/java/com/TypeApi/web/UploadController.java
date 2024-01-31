package com.TypeApi.web;

import com.TypeApi.common.*;
import com.TypeApi.entity.Apiconfig;
import com.TypeApi.entity.Users;
import com.TypeApi.service.ApiconfigService;
import com.TypeApi.service.UploadService;
import com.TypeApi.service.UsersService;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.*;
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

    EditFile editFile = new EditFile();
    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    baseFull baseFull = new baseFull();
    UserStatus UStatus = new UserStatus();

    /**
     * 通用上传接口
     * 除这个接口外，其它接口都是为了兼容旧版
     */
    @RequestMapping(value = "/full", method = RequestMethod.POST)
    @ResponseBody
    public Object full(@RequestParam(value = "file") MultipartFile file,
                       HttpServletRequest request) throws IOException {
        String token = request.getHeader("Authorization");
        Users user = new Users();
        Integer uid = null;
        if (token != null && !token.isEmpty()) {
            DecodedJWT verify = JWT.verify(token);
            user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
            if (user == null || user.toString().isEmpty()) return Result.getResultJson(201, "用户不存在，请重新登录", null);
            uid = user.getUid();
        }
        if (file == null || file.isEmpty()) return Result.getResultJson(201, "请上传文件", null);

        String result = Result.getResultJson(201, "未开启任何上传通道，请检查配置", null);
        Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
        //验证上传大小

        Integer flieUploadType = 0;  //0为普通文件，1为图片，2为媒体
        String oldFileName = file.getOriginalFilename();
        String eName = "";
        try {
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        } catch (Exception e) {
            oldFileName = oldFileName + ".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bi != null || eName.equals(".WEBP") || eName.equals(".webp")) {
            flieUploadType = 1;
        }

        Integer isMedia = baseFull.isMedia(eName);
        if (isMedia.equals(1)) {
            flieUploadType = 2;
        }
        Integer uploadPicMax = apiconfig.getUploadPicMax();
        Integer uploadMediaMax = apiconfig.getUploadMediaMax();
        Integer uploadFilesMax = apiconfig.getUploadFilesMax();
        if (flieUploadType.equals(0)) {
            long filesMax = uploadFilesMax * 1024 * 1024;
            if (file.getSize() > filesMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(201, "文件大小不能超过" + filesMax + "M", null);
            }
        }
        if (flieUploadType.equals(1)) {
            long picMax = uploadPicMax * 1024 * 1024;
            if (file.getSize() > picMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(201, "图片大小不能超过" + picMax + "M", null);
            }
        }

        if (flieUploadType.equals(2)) {
            long mediaMax = uploadMediaMax * 1024 * 1024;
            if (file.getSize() > mediaMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(201, "媒体大小不能超过" + mediaMax + "M", null);
            }
        }

        //验证上传大小结束

        if (apiconfig.getUploadType().equals("cos")) {
            result = uploadService.cosUpload(file, this.dataprefix, apiconfig, uid);
        }
        if (apiconfig.getUploadType().equals("local")) {
            result = uploadService.localUpload(file, this.dataprefix, apiconfig, uid);
        }
        if (apiconfig.getUploadType().equals("oss")) {
            result = uploadService.ossUpload(file, this.dataprefix, apiconfig, uid);
        }
        if (apiconfig.getUploadType().equals("ftp")) {
            result = uploadService.ftpUpload(file, this.dataprefix, apiconfig, uid);
        }
        if (apiconfig.getUploadType().equals("qiniu")) {
            result = uploadService.qiniuUpload(file, this.dataprefix, apiconfig, uid);
        }
        return result;
    }

    /**
     * 上传cos
     *
     * @return
     */
    @RequestMapping(value = "/cosUpload", method = RequestMethod.POST)
    @ResponseBody
    public Object cosUpload(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "token", required = false) String token) throws IOException {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }

        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid = Integer.parseInt(map.get("uid").toString());
        if (file == null) {
            return new UploadMsg(0, "文件为空", null);
        }
        Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
        //验证上传大小

        Integer flieUploadType = 0;  //0为普通文件，1为图片，2为媒体
        String oldFileName = file.getOriginalFilename();
        String eName = "";
        try {
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        } catch (Exception e) {
            oldFileName = oldFileName + ".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bi != null || eName.equals(".WEBP") || eName.equals(".webp")) {
            flieUploadType = 1;
        }

        Integer isMedia = baseFull.isMedia(eName);
        if (isMedia.equals(1)) {
            flieUploadType = 2;
        }
        Integer uploadPicMax = apiconfig.getUploadPicMax();
        Integer uploadMediaMax = apiconfig.getUploadMediaMax();
        Integer uploadFilesMax = apiconfig.getUploadFilesMax();
        if (flieUploadType.equals(0)) {
            long filesMax = uploadFilesMax * 1024 * 1024;
            if (file.getSize() > filesMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "文件大小不能超过" + filesMax + "M", null);
            }
        }
        if (flieUploadType.equals(1)) {
            long picMax = uploadPicMax * 1024 * 1024;
            if (file.getSize() > picMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "图片大小不能超过" + picMax + "M", null);
            }
        }

        if (flieUploadType.equals(2)) {
            long mediaMax = uploadMediaMax * 1024 * 1024;
            if (file.getSize() > mediaMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "媒体大小不能超过" + mediaMax + "M", null);
            }
        }

        //验证上传大小结束
        if (!apiconfig.getUploadType().equals("cos")) {
            return Result.getResultJson(0, "该上传通道已关闭", null);
        }
        String result = uploadService.cosUpload(file, this.dataprefix, apiconfig, uid);
        return result;
    }

    private class UploadMsg {
        public int status;
        public String msg;
        public String path;

        public UploadMsg() {
            super();
        }

        public UploadMsg(int status, String msg, String path) {
            this.status = status;
            this.msg = msg;
            this.path = path;
        }
    }

    /**
     * 上传到本地
     */
    @RequestMapping(value = "/localUpload", method = RequestMethod.POST)
    @ResponseBody
    public String localUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "token", required = false) String token) throws IOException {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid = Integer.parseInt(map.get("uid").toString());
        Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
        //验证上传大小

        Integer flieUploadType = 0;  //0为普通文件，1为图片，2为媒体
        String oldFileName = file.getOriginalFilename();
        String eName = "";
        try {
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        } catch (Exception e) {
            oldFileName = oldFileName + ".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bi != null || eName.equals(".WEBP") || eName.equals(".webp")) {
            flieUploadType = 1;
        }

        Integer isMedia = baseFull.isMedia(eName);
        if (isMedia.equals(1)) {
            flieUploadType = 2;
        }
        Integer uploadPicMax = apiconfig.getUploadPicMax();
        Integer uploadMediaMax = apiconfig.getUploadMediaMax();
        Integer uploadFilesMax = apiconfig.getUploadFilesMax();
        if (flieUploadType.equals(0)) {
            long filesMax = uploadFilesMax * 1024 * 1024;
            if (file.getSize() > filesMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "文件大小不能超过" + filesMax + "M", null);
            }
        }
        if (flieUploadType.equals(1)) {
            long picMax = uploadPicMax * 1024 * 1024;
            if (file.getSize() > picMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "图片大小不能超过" + picMax + "M", null);
            }
        }

        if (flieUploadType.equals(2)) {
            long mediaMax = uploadMediaMax * 1024 * 1024;
            if (file.getSize() > mediaMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "媒体大小不能超过" + mediaMax + "M", null);
            }
        }

        //验证上传大小结束
        if (!apiconfig.getUploadType().equals("local")) {
            return Result.getResultJson(0, "该上传通道已关闭", null);
        }
        String result = uploadService.localUpload(file, this.dataprefix, apiconfig, uid);
        return result;

    }

    /**
     * 上传到oss
     */
    @RequestMapping(value = "/ossUpload", method = RequestMethod.POST)
    @ResponseBody
    public String ossUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "token", required = false) String token) throws IOException {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid = Integer.parseInt(map.get("uid").toString());
        Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
        //验证上传大小

        Integer flieUploadType = 0;  //0为普通文件，1为图片，2为媒体
        String oldFileName = file.getOriginalFilename();
        String eName = "";
        try {
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        } catch (Exception e) {
            oldFileName = oldFileName + ".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bi != null || eName.equals(".WEBP") || eName.equals(".webp")) {
            flieUploadType = 1;
        }

        Integer isMedia = baseFull.isMedia(eName);
        if (isMedia.equals(1)) {
            flieUploadType = 2;
        }
        Integer uploadPicMax = apiconfig.getUploadPicMax();
        Integer uploadMediaMax = apiconfig.getUploadMediaMax();
        Integer uploadFilesMax = apiconfig.getUploadFilesMax();
        if (flieUploadType.equals(0)) {
            long filesMax = uploadFilesMax * 1024 * 1024;
            if (file.getSize() > filesMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "文件大小不能超过" + filesMax + "M", null);
            }
        }
        if (flieUploadType.equals(1)) {
            long picMax = uploadPicMax * 1024 * 1024;
            if (file.getSize() > picMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "图片大小不能超过" + picMax + "M", null);
            }
        }

        if (flieUploadType.equals(2)) {
            long mediaMax = uploadMediaMax * 1024 * 1024;
            if (file.getSize() > mediaMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "媒体大小不能超过" + mediaMax + "M", null);
            }
        }

        //验证上传大小结束
        if (!apiconfig.getUploadType().equals("oss")) {
            return Result.getResultJson(0, "该上传通道已关闭", null);
        }
        String result = uploadService.ossUpload(file, this.dataprefix, apiconfig, uid);
        return result;

    }

    /**
     * 上传到七牛云
     */
    @RequestMapping(value = "/qiniuUpload", method = RequestMethod.POST)
    @ResponseBody
    public String qiniuUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "token", required = false) String token) throws IOException {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid = Integer.parseInt(map.get("uid").toString());
        Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
        //验证上传大小

        Integer flieUploadType = 0;  //0为普通文件，1为图片，2为媒体
        String oldFileName = file.getOriginalFilename();
        String eName = "";
        try {
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        } catch (Exception e) {
            oldFileName = oldFileName + ".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bi != null || eName.equals(".WEBP") || eName.equals(".webp")) {
            flieUploadType = 1;
        }

        Integer isMedia = baseFull.isMedia(eName);
        if (isMedia.equals(1)) {
            flieUploadType = 2;
        }
        Integer uploadPicMax = apiconfig.getUploadPicMax();
        Integer uploadMediaMax = apiconfig.getUploadMediaMax();
        Integer uploadFilesMax = apiconfig.getUploadFilesMax();
        if (flieUploadType.equals(0)) {
            long filesMax = uploadFilesMax * 1024 * 1024;
            if (file.getSize() > filesMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "文件大小不能超过" + filesMax + "M", null);
            }
        }
        if (flieUploadType.equals(1)) {
            long picMax = uploadPicMax * 1024 * 1024;
            if (file.getSize() > picMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "图片大小不能超过" + picMax + "M", null);
            }
        }

        if (flieUploadType.equals(2)) {
            long mediaMax = uploadMediaMax * 1024 * 1024;
            if (file.getSize() > mediaMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "媒体大小不能超过" + mediaMax + "M", null);
            }
        }

        //验证上传大小结束
        if (!apiconfig.getUploadType().equals("qiniu")) {
            return Result.getResultJson(0, "该上传通道已关闭", null);
        }
        String result = uploadService.qiniuUpload(file, this.dataprefix, apiconfig, uid);
        return result;
    }

    /**
     * 上传到远程ftp
     */
    @RequestMapping(value = "ftpUpload", method = RequestMethod.POST)
    @ResponseBody
    public String ftpUpload(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "token", required = false) String token) {
        Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
        if (uStatus == 0) {
            return Result.getResultJson(0, "用户未登录或Token验证失败", null);
        }
        Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
        Integer uid = Integer.parseInt(map.get("uid").toString());
        String oldFileName = file.getOriginalFilename();
        Apiconfig apiconfig = UStatus.getConfig(this.dataprefix, apiconfigService, redisTemplate);
        //验证上传大小

        Integer flieUploadType = 0;  //0为普通文件，1为图片，2为媒体
        String eName = "";
        try {
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        } catch (Exception e) {
            oldFileName = oldFileName + ".png";
            eName = oldFileName.substring(oldFileName.lastIndexOf("."));
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bi != null || eName.equals(".WEBP") || eName.equals(".webp")) {
            flieUploadType = 1;
        }

        Integer isMedia = baseFull.isMedia(eName);
        if (isMedia.equals(1)) {
            flieUploadType = 2;
        }
        Integer uploadPicMax = apiconfig.getUploadPicMax();
        Integer uploadMediaMax = apiconfig.getUploadMediaMax();
        Integer uploadFilesMax = apiconfig.getUploadFilesMax();
        if (flieUploadType.equals(0)) {
            long filesMax = uploadFilesMax * 1024 * 1024;
            if (file.getSize() > filesMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "文件大小不能超过" + filesMax + "M", null);
            }
        }
        if (flieUploadType.equals(1)) {
            long picMax = uploadPicMax * 1024 * 1024;
            if (file.getSize() > picMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "图片大小不能超过" + picMax + "M", null);
            }
        }

        if (flieUploadType.equals(2)) {
            long mediaMax = uploadMediaMax * 1024 * 1024;
            if (file.getSize() > mediaMax) {
                // 文件大小超过限制，返回错误消息或进行其他处理
                return Result.getResultJson(0, "媒体大小不能超过" + mediaMax + "M", null);
            }
        }

        //验证上传大小结束
        if (!apiconfig.getUploadType().equals("ftp")) {
            return Result.getResultJson(0, "该上传通道已关闭", null);
        }
        String result = uploadService.ftpUpload(file, this.dataprefix, apiconfig, uid);
        return result;


    }
}
