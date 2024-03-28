package com.Fanbbs.config;

import com.Fanbbs.common.JWTInterceptors;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JWTInterceptors())
                .excludePathPatterns("/user/login")
                .excludePathPatterns("/user/OAuth")
                .excludePathPatterns("/user/userInfo")
                .excludePathPatterns("/user/userList")
                .excludePathPatterns("/user/register")
                .excludePathPatterns("/user/regCodeSend")
                .excludePathPatterns("/user/resetPassword")
                .excludePathPatterns("/user/regConfig")
                .excludePathPatterns("/article/articleList")
                .excludePathPatterns("/article/info")
                .excludePathPatterns("/category/list")
                .excludePathPatterns("/category/info")
                .excludePathPatterns("/comments/list")
                .excludePathPatterns("/install/check")
                .excludePathPatterns("/install/install")
                .excludePathPatterns("/raffle/list")
                .excludePathPatterns("/rank/list")
                .excludePathPatterns("/shop/list")
                .excludePathPatterns("/system/appHomepage")
                .excludePathPatterns("/system/app")
                .excludePathPatterns("/headpicture/list");
    }
}
