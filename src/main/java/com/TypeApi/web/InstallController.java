package com.TypeApi.web;

import com.TypeApi.common.JWT;
import com.TypeApi.common.PHPass;
import com.TypeApi.common.RedisHelp;
import com.TypeApi.common.ResultAll;
import com.TypeApi.entity.Apiconfig;
import com.TypeApi.entity.App;
import com.TypeApi.entity.Category;
import com.TypeApi.entity.Users;
import com.TypeApi.service.ApiconfigService;
import com.TypeApi.service.AppService;
import com.TypeApi.service.CategoryService;
import com.TypeApi.service.UsersService;
import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 初次安装控制器
 * <p>
 * 用户检测数据表和字段是否存在，不存在则添加实现安装
 */
@Controller
@RequestMapping(value = "/install")
public class InstallController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UsersService usersService;

    @Autowired
    private ApiconfigService apiconfigService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AppService appService;

    @Value("${mybatis.configuration.variables.prefix}")
    private String prefix;

    @Value("${web.prefix}")
    private String dataprefix;

    @Value("${webinfo.key}")
    private String key;


    RedisHelp redisHelp = new RedisHelp();
    ResultAll Result = new ResultAll();
    PHPass phpass = new PHPass(8);


    /***
     * 检测数据库是否安装
     * @return
     */
    @RequestMapping(value = "/check")
    @ResponseBody
    public String check() {
        try {
            Integer i = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '" + prefix + "_users';", Integer.class);
            if (i.equals(0)) {
                return Result.getResultJson(201, "数据库未安装", null);
            }
            return Result.getResultJson(200, "数据库已安装", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    @RequestMapping(value = "/install")
    @ResponseBody
    public String install(@RequestParam(value = "username", required = false) String username,
                          @RequestParam(value = "password", required = false) String password,
                          @RequestParam(value = "email", required = false) String email) {

        try {
            // 检测是否有安装数据库
            Boolean isInstall = false;
            isInstall = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = '" + prefix + "_users';", Boolean.class);
            if (isInstall) return Result.getResultJson(201, "数据库已安装，无需重复执行", null);

            // 开始执行安装数据库
            String users = prefix + "_users"; // 用户
            String contents = prefix + "_contents"; // 内容
            String comments = prefix + "_comments"; //评论
            String metas = prefix + "_metas"; //分类
            String relationships = prefix + "_relationships"; //关联表
            String userlog = prefix + "_userlog"; //日志表
            String userapi = prefix + "_userapi"; //社会化
            String shop = prefix + "_shop"; //商城
            String shopType = prefix + "_shopType"; //商城
            String order = prefix + "_order"; //订单
            String commentLike = prefix + "_commentLike"; //评论点赞
            String appHomepage = prefix + "_appHomepage"; // 应用首页
            String paylog = prefix + "_paylog"; //支付记录
            String paykey = prefix + "_paykey"; // 卡密
            String apiconfig = prefix + "_apiconfig"; //api配置
            String invite = prefix + "_invitation"; // 邀请码


            // 安装用户表
            String createTableSQL = "CREATE TABLE IF NOT EXISTS`" + users + "` ("
                    + "`uid` int(10) unsigned NOT NULL AUTO_INCREMENT,"
                    + "`name` varchar(32) DEFAULT NULL COMMENT '用户名',"
                    + "`screenName` varchar(32) DEFAULT NULL COMMENT '昵称',"
                    + "`avatar` text NULL COMMENT '头像',"
                    + "`password` varchar(64) DEFAULT NULL COMMENT '密码',"
                    + "`mail` varchar(200) DEFAULT NULL COMMENT '邮箱',"
                    + "`introduce` varchar(255) DEFAULT '系统默认签名' COMMENT '简介',"
                    + "`sex` varchar(10) DEFAULT '未知' COMMENT '性别',"
                    + "`experience` int(11) DEFAULT 0 COMMENT '经验',"
                    + "`userBg` longtext COMMENT '背景图',"
                    + "`address` longtext COMMENT '地址',"
                    + "`pay` longtext COMMENT '支付',"
                    + "`vip` int(10) DEFAULT 0 COMMENT '会员级别',"
                    + "`assets` int(11) DEFAULT 0 COMMENT '用户积分',"
                    + "`medal` longtext COMMENT '勋章',"
                    + "`head_picture` longtext COMMENT '头像框',"
                    + "`url` varchar(200) DEFAULT NULL COMMENT '链接',"
                    + "`clientId` varchar(255) DEFAULT NULL COMMENT '客户端ID',"
                    + "`status` INT NOT NULL DEFAULT 1 COMMENT '状态',"
                    + "`group` varchar(16) DEFAULT 'visitor' COMMENT '用户组',"
                    + "`authCode` varchar(64) DEFAULT NULL COMMENT '授权码',"
                    + "`rank` longtext NULL COMMENT '头衔',"
                    + "`opt` longtext COMMENT '用户配置',"
                    + "`posttime` int(10) DEFAULT 0 COMMENT '发布时间',"
                    + "`bantime` int(10) DEFAULT 0 COMMENT '封禁时间',"
                    + "`logged` int(10) DEFAULT 0 COMMENT '登录时间',"
                    + "`activated` int(10) DEFAULT 0 COMMENT '活动时间',"
                    + "`created` int(10) DEFAULT 0 COMMENT '创建时间',"
                    + "PRIMARY KEY (`uid`),"
                    + "UNIQUE KEY `name` (`name`),"
                    + "UNIQUE KEY `mail` (`mail`)"
                    + ") ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;";
            jdbcTemplate.execute(createTableSQL);
            //安装内容表
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + contents + "` (" +
                    "`cid` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '内容ID'," +
                    "`mid` int(10) unsigned NOT NULL COMMENT '模块ID'," +
                    "`title` varchar(200) DEFAULT NULL COMMENT '标题'," +
                    "`slug` varchar(200) DEFAULT NULL COMMENT '标识符'," +
                    "`text` longtext COMMENT '内容文本'," +
                    "`order` int(10) unsigned DEFAULT '0' COMMENT '排序'," +
                    "`authorId` int(10) unsigned DEFAULT '0' COMMENT '作者ID'," +
                    "`template` varchar(32) DEFAULT NULL COMMENT '模板'," +
                    "`type` varchar(16) DEFAULT 'post' COMMENT '类型'," +
                    "`status` varchar(16) DEFAULT 'publish' COMMENT '状态'," +
                    "`password` varchar(32) DEFAULT NULL COMMENT '密码'," +
                    "`commentsNum` int(10) unsigned DEFAULT '0' COMMENT '评论数量'," +
                    "`allowComment` char(1) DEFAULT '0' COMMENT '允许评论'," +
                    "`price` int(10) DEFAULT '0' COMMENT '价格'," +
                    "`discount` float(2) DEFAULT 1.0 COMMENT '折扣'," +
                    "`allowPing` INT DEFAULT 0 COMMENT '允许Ping'," +
                    "`likes` INT DEFAULT 0 COMMENT '点赞数量'," +
                    "`marks` INT DEFAULT 0 COMMENT '标记数量'," +
                    "`images` longtext COMMENT '图片'," +
                    "`videos` longtext COMMENT '视频'," +
                    "`opt` longtext COMMENT '选项'," +
                    "`allowFeed` char(1) DEFAULT '0' COMMENT '允许Feed'," +
                    "`parent` int(10) unsigned DEFAULT '0' COMMENT '父ID'," +
                    "`created` int(10) unsigned DEFAULT '0' COMMENT '创建时间'," +
                    "`modified` int(10) unsigned DEFAULT '0' COMMENT '修改时间'," +
                    "`isCircleTop` INT DEFAULT 0 COMMENT '圈子置顶'," +
                    "`views` INT DEFAULT 0 COMMENT '浏览次数'," +
                    "`isrecommend` INT DEFAULT 0 COMMENT '是否推荐'," +
                    "`istop` INT DEFAULT 0 COMMENT '是否置顶'," +
                    "`replyTime` INT DEFAULT 0 COMMENT '回复时间'," +
                    "`isswiper` INT DEFAULT 0 COMMENT '是否轮播'," +
                    "PRIMARY KEY (`cid`)," +
                    "UNIQUE KEY `slug` (`slug`)," +
                    "KEY `created` (`created`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='内容表';";
            jdbcTemplate.execute(createTableSQL);

            // 安装评论表
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + comments + "` (" +
                    "`id` INT NOT NULL AUTO_INCREMENT COMMENT '评论ID'," +
                    "`cid` INT NOT NULL COMMENT '文章ID'," +
                    "`uid` INT NOT NULL COMMENT '用户ID'," +
                    "`text` TEXT COMMENT '内容'," +
                    "`images` TEXT COMMENT '图片列表'," +
                    "`ip` TEXT COMMENT 'IP地址'," +
                    "`parent` INT DEFAULT 0 COMMENT '父评论ID'," +
                    "`likes` INT DEFAULT 0 COMMENT '点赞数量'," +
                    "`all` INT DEFAULT 0 COMMENT '所有评论的父ID'," +
                    "`type` INT NOT NULL DEFAULT 0 COMMENT '评论类型'," +
                    "`created` INT COMMENT '创建时间'," +
                    "`modified` INT COMMENT '修改时间'," +
                    "PRIMARY KEY (`id`)," +
                    "KEY `cid` (`cid`)," +
                    "KEY `created` (`created`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='文章评论表';";
            jdbcTemplate.execute(createTableSQL);

            // 安装分类表
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + metas + "` (" +
                    "`mid` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '分类ID'," +
                    "`name` varchar(200) DEFAULT NULL COMMENT '名称'," +
                    "`avatar` varchar(255) COMMENT '头像'," +
                    "`slug` varchar(200) DEFAULT NULL COMMENT '别名'," +
                    "`type` varchar(32) NOT NULL COMMENT '类型'," +
                    "`description` varchar(200) DEFAULT NULL COMMENT '描述'," +
                    "`count` int(10) unsigned DEFAULT 0 COMMENT '计数'," +
                    "`order` int(10) unsigned DEFAULT 0 COMMENT '排序'," +
                    "`iswaterfall` int DEFAULT 0 COMMENT '是否瀑布流'," +
                    "`isvip` int DEFAULT 0 COMMENT '是否VIP'," +
                    "`follows` int DEFAULT 0 COMMENT '关注数量'," +
                    "`isrecommend` int DEFAULT 0 COMMENT '是否推荐'," +
                    "`parent` int(10) unsigned DEFAULT 0 COMMENT '父分类ID'," +
                    "`permission` INT NOT NULL DEFAULT 0 COMMENT '权限设置'," +
                    "`imgurl` varchar(500) DEFAULT NULL COMMENT '图片URL'," +
                    "`opt` longtext NULL COMMENT '自定义字段'," +
                    "PRIMARY KEY (`mid`)," +
                    "KEY `slug` (`slug`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='分类表';";

            jdbcTemplate.execute(createTableSQL);
            //安装关联表
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + relationships + "` (" +
                    "`cid` int(10) unsigned NOT NULL COMMENT '文章ID'," +
                    "`mid` int(10) unsigned NOT NULL COMMENT '分类ID'," +
                    "PRIMARY KEY (`cid`,`mid`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='关联表';";
            jdbcTemplate.execute(createTableSQL);

            // 安装用户日志表
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + userlog + "` (" +
                    "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT '日志ID'," +
                    "`uid` int(11) NOT NULL DEFAULT '-1' COMMENT '用户ID'," +
                    "`cid` int(11) NOT NULL DEFAULT '0' COMMENT '文章ID'," +
                    "`type` varchar(255) DEFAULT NULL COMMENT '类型'," +
                    "`num` int(11) DEFAULT '0' COMMENT '数值，用于后期扩展'," +
                    "`created` int(10) NOT NULL DEFAULT '0' COMMENT '时间'," +
                    "`toid` int(11) DEFAULT 0 COMMENT '目标ID'," + // 新增字段
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='用户日志（收藏，扩展等）';";
            jdbcTemplate.execute(createTableSQL);

            // 安装社会化表
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + userapi + "` (" +
                    "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
                    "`headImgUrl` varchar(255) DEFAULT NULL COMMENT '头像，可能用不上'," +
                    "`openId` varchar(255) DEFAULT NULL COMMENT '开放平台ID'," +
                    "`access_token` varchar(255) DEFAULT NULL COMMENT '唯一值'," +
                    "`appLoginType` varchar(255) DEFAULT NULL COMMENT '渠道类型'," +
                    "`uid` int(11) DEFAULT '0' COMMENT '用户ID'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='社会化登陆';";
            jdbcTemplate.execute(createTableSQL);

            //安装商城
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + shop + "` (" +
                    "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
                    "`title` varchar(300) DEFAULT NULL COMMENT '商品标题'," +
                    "`imgurl` varchar(500) DEFAULT NULL COMMENT '商品图片'," +
                    "`text` text COMMENT '商品内容'," +
                    "`price` int(11) DEFAULT '0' COMMENT '商品价格'," +
                    "`num` int(11) DEFAULT '0' COMMENT '商品数量'," +
                    "`type` int(11) DEFAULT '1' COMMENT '商品类型（实体，源码，工具，教程）'," +
                    "`value` text COMMENT '收费显示（除实体外，这个字段购买后显示）'," +
                    "`cid` int(11) DEFAULT '-1' COMMENT '所属文章'," +
                    "`uid` int(11) DEFAULT '0' COMMENT '发布人'," +
                    "`specs` longtext DEFAULT NULL COMMENT '规格'," +
                    "`freight` int DEFAULT 0 COMMENT '运费'," +
                    "`vipDiscount` varchar(255) NOT NULL DEFAULT '0.1' COMMENT 'VIP折扣，权高于系统设置折扣'," +
                    "`created` int(10) DEFAULT 0 COMMENT '创建时间'," +
                    "`status` int(10) DEFAULT 0 COMMENT '状态'," +
                    "`sellNum` int(11) DEFAULT 0 COMMENT '销售数量'," +
                    "`isMd` int(2) unsigned DEFAULT '1' COMMENT '是否为Markdown编辑器发布'," +
                    "`sort` int(11) unsigned DEFAULT '0' COMMENT '商品大类'," +
                    "`subtype` int(11) unsigned DEFAULT '0' COMMENT '子类型'," +
                    "`isView` int(2) unsigned DEFAULT '1' COMMENT '是否可见'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='商品表';";
            jdbcTemplate.execute(createTableSQL);

            // 安装商城分类
            createTableSQL = "CREATE TABLE IF NOT EXISTS `" + shopType + "` (" +
                    "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
                    "`parent` int(11) DEFAULT '0' COMMENT '上级分类'," +
                    "`name` varchar(255) DEFAULT NULL COMMENT '分类名称'," +
                    "`pic` varchar(400) DEFAULT NULL COMMENT '分类缩略图'," +
                    "`intro` varchar(400) DEFAULT NULL COMMENT '分类简介'," +
                    "`created` INT DEFAULT 0 COMMENT '创建时间'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';";
            jdbcTemplate.execute(createTableSQL);

            // 安装订单表
            createTableSQL = "CREATE TABLE IF NOT EXISTS `" + order + "` (" +
                    "`id` int NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
                    "`orders` text DEFAULT NULL COMMENT '商品订单'," +
                    "`price` int DEFAULT 0 COMMENT '商品价格'," +
                    "`paid` int DEFAULT 0 COMMENT '支付状态'," +
                    "`user_id` int NOT NULL COMMENT '购买用户'," +
                    "`boss_id` int NOT NULL COMMENT '老板ID'," +
                    "`product` int NOT NULL COMMENT '商品ID'," +
                    "`product_name` text COMMENT '商品标题'," +
                    "`specs` text COMMENT '商品规格'," +
                    "`tracking_number` text NULL COMMENT '快递单'," +
                    "`address` text NOT NULL COMMENT '地址'," +
                    "`freight` int DEFAULT 0 COMMENT '运费'," +
                    "`created` bigint DEFAULT NULL COMMENT '创建时间'," +
                    "`isTracking` int DEFAULT 0 COMMENT '是否追踪'," +
                    "`isPaid` int DEFAULT 0 COMMENT '是否支付'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='订单号表';";
            jdbcTemplate.execute(createTableSQL);

            // 安装点赞记录表
            createTableSQL = "CREATE TABLE IF NOT EXISTS `" + commentLike + "` (" +
                    "`id` INT NOT NULL AUTO_INCREMENT," +
                    "`cid` INT UNSIGNED NOT NULL COMMENT '评论id'," +
                    "`uid` INT NOT NULL COMMENT '用户id'," +
                    "`created` INT COMMENT '创建时间'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录表';";
            jdbcTemplate.execute(createTableSQL);

            // 安装应用首页;
            createTableSQL = "CREATE TABLE IF NOT EXISTS `" + appHomepage + "` (" +
                    "`id` INT NOT NULL AUTO_INCREMENT," +
                    "`page` TEXT NOT NULL COMMENT '路径'," +
                    "`name` VARCHAR(16) NOT NULL COMMENT '名称'," +
                    "`type` INT NOT NULL DEFAULT 0 COMMENT '类型'," +
                    "`image` TEXT COMMENT '图片'," +
                    "`enable` INT NOT NULL DEFAULT 1 COMMENT '启动'," +
                    "`created` INT COMMENT '创建时间'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='应用首页配置';";
            jdbcTemplate.execute(createTableSQL);

            // 支付记录
            createTableSQL = "CREATE TABLE IF NOT EXISTS `" + paylog + "` (" +
                    "`pid` INT NOT NULL AUTO_INCREMENT," +
                    "`subject` VARCHAR(255) DEFAULT NULL," +
                    "`total_amount` VARCHAR(255) DEFAULT NULL," +
                    "`out_trade_no` VARCHAR(255) DEFAULT NULL," +
                    "`trade_no` VARCHAR(255) DEFAULT NULL," +
                    "`paytype` VARCHAR(255) DEFAULT '' COMMENT '支付类型'," +
                    "`uid` INT DEFAULT '-1' COMMENT '充值人ID'," +
                    "`created` INT DEFAULT NULL," +
                    "`cid` INT DEFAULT NULL COMMENT '文章id'," +
                    "`status` INT DEFAULT '0' COMMENT '支付状态（0未支付，1已支付）'," +
                    "PRIMARY KEY (`pid`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='支付渠道充值记录';";
            jdbcTemplate.execute(createTableSQL);

            // 安装卡密表
            createTableSQL = "CREATE TABLE IF NOT EXISTS `" + paykey + "` (" +
                    "`id` INT NOT NULL AUTO_INCREMENT," +
                    "`value` VARCHAR(255) DEFAULT '' COMMENT '密钥'," +
                    "`price` INT DEFAULT '0' COMMENT '数值'," +
                    "`type` VARCHAR(255) DEFAULT 'point' COMMENT '类型'," +
                    "`status` INT DEFAULT '0' COMMENT '0未使用，1已使用'," +
                    "`created` INT DEFAULT '0' COMMENT '创建时间'," +
                    "`uid` INT DEFAULT '-1' COMMENT '使用用户'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='卡密充值相关';";
            jdbcTemplate.execute(createTableSQL);

            // 安装api配置
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + apiconfig + "` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `webinfoTitle` varchar(500) NOT NULL DEFAULT '' COMMENT '网站名称'," +
                    "  `webinfoUrl` varchar(500) NOT NULL DEFAULT '' COMMENT '网站URL'," +
                    "  `webinfoUploadUrl` varchar(255) NOT NULL DEFAULT 'http://127.0.0.1:8081/' COMMENT '本地图片访问路径'," +
                    "  `webinfoAvatar` varchar(500) NOT NULL DEFAULT 'https://cdn.helingqi.com/wavatar/' COMMENT '头像源'," +
                    "  `pexelsKey` varchar(255) NOT NULL DEFAULT '' COMMENT '图库key'," +
                    "  `scale` int(11) NOT NULL DEFAULT '100' COMMENT '一元能买多少积分'," +
                    "  `clock` int(11) NOT NULL DEFAULT '5' COMMENT '签到最多多少积分'," +
                    "  `vipPrice` int(11) NOT NULL DEFAULT '200' COMMENT 'VIP一天价格'," +
                    "  `vipDay` int(11) NOT NULL DEFAULT '300' COMMENT '多少天VIP等于永久'," +
                    "  `vipDiscount` varchar(11) NOT NULL DEFAULT '0.1' COMMENT 'VIP折扣'," +
                    "  `isEmail` int(2) NOT NULL DEFAULT '1' COMMENT '邮箱开关（0完全关闭邮箱，1只开启邮箱注册，2邮箱注册和操作通知）'," +
                    "  `isInvite` int(11) NOT NULL DEFAULT '0' COMMENT '注册是否验证邀请码（默认关闭）'," +
                    "  `cosAccessKey` varchar(300) NOT NULL DEFAULT ''," +
                    "  `cosSecretKey` varchar(300) NOT NULL DEFAULT ''," +
                    "  `cosBucket` varchar(255) NOT NULL DEFAULT ''," +
                    "  `cosBucketName` varchar(255) NOT NULL DEFAULT ''," +
                    "  `cosPath` varchar(255) DEFAULT ''," +
                    "  `cosPrefix` varchar(255) NOT NULL DEFAULT ''," +
                    "  `aliyunEndpoint` varchar(500) NOT NULL DEFAULT ''," +
                    "  `aliyunAccessKeyId` varchar(255) NOT NULL DEFAULT ''," +
                    "  `aliyunAccessKeySecret` varchar(255) NOT NULL DEFAULT ''," +
                    "  `aliyunAucketName` varchar(255) NOT NULL DEFAULT ''," +
                    "  `aliyunUrlPrefix` varchar(255) NOT NULL DEFAULT ''," +
                    "  `aliyunFilePrefix` varchar(255) NOT NULL DEFAULT ''," +
                    "  `ftpHost` varchar(255) NOT NULL DEFAULT ''," +
                    "  `ftpPort` int(11) NOT NULL DEFAULT '21'," +
                    "  `ftpUsername` varchar(255) NOT NULL DEFAULT ''," +
                    "  `ftpPassword` varchar(255) NOT NULL DEFAULT ''," +
                    "  `ftpBasePath` varchar(255) NOT NULL DEFAULT ''," +
                    "  `alipayAppId` varchar(255) NOT NULL DEFAULT ''," +
                    "  `alipayPrivateKey` text," +
                    "  `alipayPublicKey` text," +
                    "  `alipayNotifyUrl` varchar(500) NOT NULL DEFAULT ''," +
                    "  `appletsAppid` varchar(255) NOT NULL DEFAULT ''," +
                    "  `appletsSecret` text," +
                    "  `wxpayAppId` varchar(255) NOT NULL DEFAULT ''," +
                    "  `wxpayMchId` varchar(255) NOT NULL DEFAULT ''," +
                    "  `wxpayKey` text," +
                    "  `wxpayNotifyUrl` varchar(500) DEFAULT ''," +
                    "  `compress` int(11) DEFAULT '0'," +
                    "  `quality` float DEFAULT '0.8'," +
                    "  `uploadLevel` int(11) DEFAULT '0'," +
                    "  `levelExp` varchar(500) DEFAULT '[100,200,400,600,1000,1400,1800,2200,2400,2600,2800,3000,4000,5000,6000,7000]' COMMENT '等级经验'," +
                    "  `auditlevel` int(2) DEFAULT '1'," +
                    "  `forbidden` text," +
                    "  `qqAppletsAppid` varchar(500) DEFAULT NULL," +
                    "  `qqAppletsSecret` varchar(500) DEFAULT NULL," +
                    "  `wxAppId` varchar(500) DEFAULT NULL," +
                    "  `wxAppSecret` varchar(500) DEFAULT NULL," +
                    "  `pushAdsPrice` int(11) NOT NULL DEFAULT '100' COMMENT '推流广告价格(积分/天)'," +
                    "  `pushAdsNum` int(11) NOT NULL DEFAULT '10' COMMENT '推流广告数量'," +
                    "  `bannerAdsPrice` int(11) NOT NULL DEFAULT '100' COMMENT '横幅广告价格(积分/天)'," +
                    "  `bannerAdsNum` int(11) NOT NULL DEFAULT '5' COMMENT '横幅广告数量'," +
                    "  `startAdsPrice` int(11) NOT NULL DEFAULT '100' COMMENT '启动图广告价格(积分/天)'," +
                    "  `startAdsNum` int(11) NOT NULL DEFAULT '1' COMMENT '启动图广告数量'," +
                    "  `epayUrl` varchar(500) DEFAULT '' COMMENT '易支付接口地址'," +
                    "  `epayPid` int(11) DEFAULT NULL COMMENT '易支付商户ID'," +
                    "  `epayKey` varchar(300) DEFAULT '' COMMENT '易支付商户密钥'," +
                    "  `epayNotifyUrl` varchar(500) DEFAULT '' COMMENT '易支付回调地址'," +
                    "  `mchSerialNo` text COMMENT '微信支付商户证书序列号'," +
                    "  `mchApiV3Key` text COMMENT '微信支付API3私钥'," +
                    "  `cloudUid` varchar(255) DEFAULT '' COMMENT '云控UID'," +
                    "  `cloudUrl` varchar(255) DEFAULT '' COMMENT '云控URL'," +
                    "  `pushAppId` varchar(255) DEFAULT '' COMMENT 'pushAppId'," +
                    "  `pushAppKey` varchar(255) DEFAULT '' COMMENT 'pushAppKey'," +
                    "  `pushMasterSecret` varchar(255) DEFAULT '' COMMENT 'pushMasterSecret'," +
                    "  `isPush` int(2) DEFAULT '0' COMMENT '是否开启消息通知'," +
                    "  `disableCode` int(2) DEFAULT '0' COMMENT '是否禁用代码'," +
                    "  `allowDelete` int(2) DEFAULT '0' COMMENT '是否允许用户删除文章或评论'," +
                    "  `contentAuditlevel` int(2) DEFAULT '0' COMMENT '内容审核模式'," +
                    "  `clockExp` int(11) DEFAULT '5' COMMENT '签到经验'," +
                    "  `reviewExp` int(11) DEFAULT '1' COMMENT '每日前三次评论经验'," +
                    "  `postExp` int(11) DEFAULT '10' COMMENT '每日前三次发布内容经验（文章，动态，帖子）'," +
                    "  `violationExp` int(11) DEFAULT '50' COMMENT '违规扣除经验'," +
                    "  `deleteExp` int(11) DEFAULT '20' COMMENT '删除扣除经验（文章，评论，动态，帖子）'," +
                    "  `spaceMinExp` int(11) DEFAULT '20' COMMENT '发布动态要求最低经验值'," +
                    "  `chatMinExp` int(11) DEFAULT '20' COMMENT '聊天要求最低经验值'," +
                    "  `qiniuDomain` varchar(400) DEFAULT '' COMMENT '七牛云访问域名'," +
                    "  `qiniuAccessKey` varchar(400) DEFAULT '' COMMENT '七牛云公钥'," +
                    "  `qiniuSecretKey` varchar(400) DEFAULT '' COMMENT '七牛云私钥'," +
                    "  `qiniuBucketName` varchar(255) DEFAULT '' COMMENT '七牛云存储桶名称'," +
                    "  `silenceTime` int(11) DEFAULT '600' COMMENT '疑似攻击自动封禁时间(s)'," +
                    "  `interceptTime` int(11) DEFAULT '3600' COMMENT '多次触发违规自动封禁时间(s)'," +
                    "  `isLogin` int(2) DEFAULT '0' COMMENT '开启全局登录'," +
                    "  `postMax` int(11) DEFAULT '5' COMMENT '每日最大发布'," +
                    "  `forumAudit` int(11) DEFAULT '1' COMMENT '帖子及帖子评论是否需要审核'," +
                    "  `spaceAudit` int(11) DEFAULT '0' COMMENT '动态是否需要审核'," +
                    "  `uploadType` varchar(100) DEFAULT 'local' COMMENT '上传类型'," +
                    "  `banRobots` int(2) DEFAULT '0' COMMENT '是否开启机器人严格限制模式'," +
                    "  `adsGiftNum` int(11) DEFAULT '10' COMMENT '每日广告奖励次数'," +
                    "  `adsGiftAward` int(11) DEFAULT '5' COMMENT '每日广告奖励额'," +
                    "  `uploadPicMax` int(11) DEFAULT '5' COMMENT '图片最大上传大小'," +
                    "  `uploadMediaMax` int(11) DEFAULT '50' COMMENT '媒体最大上传大小'," +
                    "  `uploadFilesMax` int(11) DEFAULT '20' COMMENT '其他文件最大上传大小'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='api配置信息表';";
            jdbcTemplate.execute(createTableSQL);

            // 安装邀请码
            createTableSQL = "CREATE TABLE IF NOT EXISTS `" + invite + "` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `code` varchar(255) DEFAULT NULL COMMENT '邀请码'," +
                    "  `created` int(10) DEFAULT '0' COMMENT '创建时间'," +
                    "  `uid` int(11) DEFAULT '0' COMMENT '创建者'," +
                    "  `status` int(2) DEFAULT '0' COMMENT '0未使用，1已使用'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='邀请码';";
            jdbcTemplate.execute(createTableSQL);

            // 安装广告
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + prefix + "_ads` (" +
                    "  `aid` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `name` varchar(255) DEFAULT '' COMMENT '广告名称'," +
                    "  `type` int(11) DEFAULT '0' COMMENT '广告类型（0推流，1横幅，2启动图，3轮播图）'," +
                    "  `img` varchar(500) DEFAULT NULL COMMENT '广告缩略图'," +
                    "  `close` int(10) DEFAULT '0' COMMENT '0代表永久，其它代表结束时间'," +
                    "  `created` int(10) unsigned DEFAULT '0' COMMENT '创建时间'," +
                    "  `price` int(11) unsigned DEFAULT '0' COMMENT '购买价格'," +
                    "  `intro` varchar(500) DEFAULT '' COMMENT '广告简介'," +
                    "  `urltype` int(11) DEFAULT '0' COMMENT '0为APP内部打开，1为跳出APP'," +
                    "  `url` text COMMENT '跳转Url'," +
                    "  `uid` int(11) DEFAULT '-1' COMMENT '发布人'," +
                    "  `status` int(2) DEFAULT '0' COMMENT '0审核中，1已公开，2已到期'," +
                    "  PRIMARY KEY (`aid`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='广告表';";
            jdbcTemplate.execute(createTableSQL);

            // 安装通知
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + prefix + "_inbox` (" +
                    "`id` int(11) NOT NULL AUTO_INCREMENT," +
                    "`type` varchar(255) DEFAULT NULL COMMENT '消息类型：system(系统消息)，comment(评论消息)，finance(财务消息)'," +
                    "`uid` int(11) DEFAULT '0' COMMENT '消息发送人，0是平台'," +
                    "`text` text COMMENT '消息内容（只有简略信息）'," +
                    "`touid` int(11) NOT NULL DEFAULT '0' COMMENT '消息接收人uid'," +
                    "`isread` int(2) DEFAULT '0' COMMENT '是否已读，0已读，1未读'," +
                    "`value` int(11) DEFAULT '0' COMMENT '消息指向内容的id，根据类型跳转'," +
                    "`created` int(10) unsigned DEFAULT '0' COMMENT '创建时间'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='消息表';";
            jdbcTemplate.execute(createTableSQL);

            // 安装粉丝表
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + prefix + "_fan` (" +
                    "`id` int(11) NOT NULL AUTO_INCREMENT," +
                    "`uid` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '关注人'," +
                    "`touid` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '被关注人'," +
                    "`created` int(10) unsigned DEFAULT '0' COMMENT '关注时间'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='关注表（全局内容）';";
            jdbcTemplate.execute(createTableSQL);

            // 违规记录表
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + prefix + "_violation` (" +
                    "`id` int(11) NOT NULL AUTO_INCREMENT," +
                    "`uid` int(11) NOT NULL DEFAULT '0' COMMENT '违规者uid'," +
                    "`type` varchar(255) DEFAULT NULL COMMENT '处理类型（manager管理员操作，system系统自动）'," +
                    "`text` text COMMENT '具体原因'," +
                    "`created` int(10) unsigned DEFAULT '0' COMMENT '违规时间'," +
                    "`handler` int(11) unsigned DEFAULT '0' COMMENT '处理人，0为系统自动，其它为真实用户'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='违规记录表';";

            jdbcTemplate.execute(createTableSQL);

            // 聊天室
            createTableSQL = "CREATE TABLE IF NOT EXISTS `" + prefix + "_chat` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `sender_id` int(11) NOT NULL COMMENT '发送人'," +
                    "  `receiver_id` int(11) NOT NULL COMMENT '接收人'," +
                    "  `type` int(10) UNSIGNED DEFAULT '0' COMMENT '0是私聊，1是群聊'," +
                    "  `name` varchar(255) DEFAULT NULL COMMENT '名称，用户名或者群聊名'," +
                    "  `avatar` text COMMENT '头像'," +
                    "  `created` int(10) UNSIGNED DEFAULT '0' COMMENT '创建时间'," +
                    "  `lastTime` int(10) UNSIGNED DEFAULT '0' COMMENT '最后聊天时间'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='聊天室表';";
            jdbcTemplate.execute(createTableSQL);

            // 聊天记录
            createTableSQL = "CREATE TABLE IF NOT EXISTS `" + prefix + "_chat_msg` (" +
                    "  `id` int NOT NULL AUTO_INCREMENT," +
                    "  `sender_id` int NOT NULL DEFAULT 0 COMMENT '发送人'," +
                    "  `text` text CHARACTER SET utf8mb4 COMMENT '消息内容'," +
                    "  `type` int COMMENT '类型0私聊1群聊'," +
                    "  `chat_id` int NOT NULL COMMENT '聊天室id'," +
                    "  `created` int unsigned DEFAULT '0' COMMENT '发送时间'," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息';";
            jdbcTemplate.execute(createTableSQL);

            // 安装头像框
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + prefix + "_headpicture` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `name` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT '名称'," +
                    "  `link` text COMMENT '链接'," +
                    "  `type` int(11) DEFAULT '0' COMMENT '类型'," +
                    "  `status` int(11) DEFAULT '1' COMMENT '状态'," +
                    "  `permission` int(11) DEFAULT '0' COMMENT '权限'," +
                    "  `creator` int(11) DEFAULT '0' COMMENT '创建人ID'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='头像框表';";
            jdbcTemplate.execute(createTableSQL);

            // 安装头衔表
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + prefix + "_rank` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `name` text NOT NULL COMMENT '兑换名称'," +
                    "  `type` int(11) NOT NULL DEFAULT '0' COMMENT '类型'," +
                    "  `image` text COMMENT '图片'," +
                    "  `color` varchar(255) DEFAULT NULL COMMENT '文字颜色'," +
                    "  `background` varchar(255) DEFAULT NULL COMMENT '背景颜色'," +
                    "  `permission` int(11) DEFAULT NULL COMMENT '权限'," +
                    "  `created` int(11) DEFAULT NULL COMMENT '创建时间'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='头衔表';";

            jdbcTemplate.execute(createTableSQL);

            // 安装应用表
            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + prefix + "_app` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `name` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT '应用名称'," +
                    "  `logo` varchar(500) CHARACTER SET utf8 DEFAULT NULL COMMENT 'logo图标地址'," +
                    "  `keywords` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT 'web专属，SEO关键词'," +
                    "  `description` varchar(255) DEFAULT NULL COMMENT '应用简介'," +
                    "  `announcement` varchar(400) DEFAULT NULL COMMENT '弹窗公告（支持html）'," +
                    "  `mail` varchar(400) CHARACTER SET utf8 DEFAULT NULL COMMENT '邮箱地址（用于通知和显示）'," +
                    "  `website` varchar(400) CHARACTER SET utf8 DEFAULT NULL COMMENT '网址（非Api地址）'," +
                    "  `currencyName` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT '货币名称'," +
                    "  `version` varchar(255) CHARACTER SET utf8 DEFAULT 'v1.0.0 beta' COMMENT 'app专属，版本号'," +
                    "  `versionCode` int(11) DEFAULT '10' COMMENT 'app专属，版本码'," +
                    "  `versionIntro` varchar(400) DEFAULT NULL COMMENT '版本简介'," +
                    "  `androidUrl` varchar(400) CHARACTER SET utf8 DEFAULT NULL COMMENT '安卓下载地址'," +
                    "  `iosUrl` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT 'ios下载地址'," +
                    "  `silence` int(11) DEFAULT '0' COMMENT '静默更新'," +
                    "  `forceUpdate` int(11) DEFAULT '0' COMMENT '强制更新'," +
                    "  `issue` int(11) DEFAULT '1' COMMENT '发布/发行'," +
                    "  `updateType` int(11) DEFAULT '0' COMMENT '更新方式'," +
                    "  `adpid` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT '广告联盟ID'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='应用表（web应用和APP应用）';";
            jdbcTemplate.execute(createTableSQL);

            createTableSQL = "CREATE TABLE IF NOT EXISTS`" + prefix + "_exchange` (" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                    "  `name` text NOT NULL COMMENT '兑换名称'," +
                    "  `type` varchar(255) NOT NULL DEFAULT 'rank' COMMENT '类型'," +
                    "  `exchange_id` int(11) DEFAULT NULL COMMENT '兑换物品id'," +
                    "  `price` int(11) DEFAULT NULL COMMENT '价格'," +
                    "  `created` int(11) DEFAULT NULL COMMENT '创建时间'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='兑换表';";
            jdbcTemplate.execute(createTableSQL);

            // 数据表安装ok 开始初始化
            App app = new App();
            app.setName("应用名称");
            app.setCurrencyName("积分");
            appService.insert(app);

            // 初始化 apiconfig
            Apiconfig apiconfig1 = new Apiconfig();
            apiconfig1.setWebinfoUrl("http://127.0.0.1/");
            apiconfig1.setWebinfoTitle("CHIKATA");
            apiconfigService.insert(apiconfig1);

            // 插入一个默认用户
            Users user = new Users();
            user.setGroup("administrator");
            user.setName("admin");
            user.setScreenName("管理员");
            user.setPassword(phpass.HashPassword("123456"));
            usersService.insert(user);

            // 插入一个默认分类
            Category category = new Category();
            category.setPermission(0);
            category.setIsrecommend(1);
            category.setName("默认分类");
            category.setType("category");
            category.setDescription("初始默认分类");
            categoryService.insert(category);


            // 将apiconfig缓存到redis中
            try {
                apiconfig1 = apiconfigService.selectByKey(1);
                Map configJson = JSONObject.parseObject(JSONObject.toJSONString(apiconfig1), Map.class);
                redisHelp.setKey(dataprefix + "_" + "config", configJson, 6000, redisTemplate);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Result.getResultJson(200, "数据库创建完成，默认账号密码admin 123456", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }
    }

    /***
     * 更新
     */
    @RequestMapping(value = "/update")
    @ResponseBody
    public String update(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            Users user = new Users();
            Boolean permission = false;
            if (token != null && !token.isEmpty()) {
                DecodedJWT verify = JWT.verify(token);
                user = usersService.selectByKey(Integer.parseInt(verify.getClaim("aud").asString()));
                permission = permission(user);
                if (!permission) return Result.getResultJson(201, "无权限", null);
            }
            return Result.getResultJson(200, "更新完成", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(400, "接口异常", null);
        }

    }

    /***
     * 权限判断
     * @param user
     * @return
     */
    private Boolean permission(Users user) {
        if (user.getUid() == null) return false;
        if (user.getGroup().equals("administrator")) return true;
        return false;
    }

    /***
     * 让内容字段变为utf8mb4，以支持emoji标签
     */
    @RequestMapping(value = "/toUtf8mb4")
    @ResponseBody
    public String toUtf8mb4(@RequestParam(value = "webkey", required = false, defaultValue = "") String webkey) {
        if (!webkey.equals(this.key)) {
            return Result.getResultJson(0, "请输入正确的访问KEY。如果忘记，可在服务器/opt/application.properties中查看", null);
        }
        try {
            String isRepeated = redisHelp.getRedis("isTypechoInstall", redisTemplate);
            if (isRepeated == null) {
                redisHelp.setRedis("isTypechoInstall", "1", 15, redisTemplate);
            } else {
                return Result.getResultJson(0, "你的操作太频繁了", null);
            }
            jdbcTemplate.execute("alter table `" + prefix + "_contents`  MODIFY COLUMN `text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("alter table `" + prefix + "_contents`  MODIFY COLUMN `title` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("alter table `" + prefix + "_shop`  MODIFY COLUMN `text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("alter table `" + prefix + "_shop`  MODIFY COLUMN `value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("alter table `" + prefix + "_inbox`  MODIFY COLUMN `text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            jdbcTemplate.execute("alter table `" + prefix + "_comments`  MODIFY COLUMN `text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            return Result.getResultJson(1, "操作成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.getResultJson(1, "操作失败", null);
        }

    }
}
