package com.Fanbbs.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * TypechoApp
 * @author vips 2023-06-09
 */
@Data
public class App implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;

    /**
     * name  应用名称
     */
    private String name;

    /**
     * logo  logo图标地址
     */
    private String logo;

    /**
     * keywords  web专属，SEO关键词
     */
    private String keywords;

    /**
     * description  应用简介
     */
    private String description;

    /**
     * announcement  弹窗公告（支持html）
     */
    private String announcement;

    /**
     * mail  邮箱地址（用于通知和显示）
     */
    private String mail;

    /**
     * website  网址（非Api地址）
     */
    private String website;

    /**
     * currencyName  货币名称
     */
    private String currencyName;

    /**
     * version  app专属，版本号
     */
    private String version;

    /**
     * versionCode  app专属，版本码
     */
    private Integer versionCode;

    /**
     * versionIntro  版本简介
     */
    private String versionIntro;

    /**
     * androidUrl  安卓下载地址
     */
    private String androidUrl;

    /**
     * iosUrl  ios下载地址
     */
    private String iosUrl;

    /**
     * adpid  广告联盟ID
     */
    private String adpid;

    /**
     * silence  静默更新
     */

    private Integer silence;
    /**
     * forceUpdate 强制更新
     */
    private Integer forceUpdate;

    /**
     * issue  是否发行
     */
    private Integer issue;

    /**
     * updateType  更新方式
     */
    private Integer updateType;
}