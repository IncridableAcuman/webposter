package com.post.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));

        source.registerCorsConfiguration("/**",configuration);

        return source;
    }
}
