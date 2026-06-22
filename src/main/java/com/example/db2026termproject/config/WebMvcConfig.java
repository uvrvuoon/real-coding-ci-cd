package com.example.db2026termproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 브라우저에서 /uploads/** 로 사진을 달라고 요청하면
        // 컨트롤러가 실제로 파일을 저장하는 src/main/resources/static/uploads/ 폴더를 실시간으로 직접 쳐다보도록 교정합니다.
        String uploadDir = "file:///" + System.getProperty("user.dir") + "/src/main/resources/static/uploads/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDir);
    }
}