package com.hgc.campusechocommon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源映射
 */
@Configuration
public class MVCConfig implements WebMvcConfigurer {
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/css/");
//        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/js/");
//        registry.addResourceHandler("/get/**").addResourceLocations("file:///root/services/photos/");
//    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/js/");
        registry.addResourceHandler("/get/**").addResourceLocations("file:///D:/Users/zed/Pictures/Camera Roll/");
    }
}
