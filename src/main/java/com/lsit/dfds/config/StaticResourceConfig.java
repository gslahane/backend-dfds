// src/main/java/com/lsit/dfds/config/StaticResourceConfig.java
package com.lsit.dfds.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Base path where your uploads are stored
        Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

        // Map URL "/files/**" -> local "uploads" folder
        registry.addResourceHandler("/files/**").addResourceLocations("file:" + uploadDir.toString() + "/")
                .setCachePeriod(3600); // cache 1 hour (optional)
    }
}