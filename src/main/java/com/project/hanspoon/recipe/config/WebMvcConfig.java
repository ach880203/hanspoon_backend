package com.project.hanspoon.recipe.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Log4j2
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("c:/hanspoon/img/")
    String uploadPath;

    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String resourceLocation = "file:///" + uploadPath;
        log.info("매핑된 로컬 경로 : "+ resourceLocation);

        registry.addResourceHandler("/images/recipe/**")

                .addResourceLocations(resourceLocation);
    }
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
