package com.admin.config;

import com.admin.common.interceptor.JwtInterceptor;
import com.admin.common.interceptor.LoginRateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.cors-origins:http://localhost:6366}")
    private String corsOrigins;

    @Resource
    private LoginRateLimitInterceptor loginRateLimitInterceptor;

    @Resource
    private JwtInterceptor jwtInterceptor;

    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        List<String> origins = parseOrigins(corsOrigins);
        if (origins.contains("*")) {
            corsConfiguration.addAllowedOriginPattern("*");
        } else {
            origins.forEach(corsConfiguration::addAllowedOrigin);
        }
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addExposedHeader("Authorization");
        corsConfiguration.setAllowCredentials(!origins.contains("*"));
        return corsConfiguration;
    }

    private List<String> parseOrigins(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of("http://localhost:6366");
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildConfig());
        return new CorsFilter(source);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = parseOrigins(corsOrigins);
        if (origins.contains("*")) {
            registry.addMapping("/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "DELETE", "PUT")
                    .maxAge(3600);
        } else {
            registry.addMapping("/**")
                    .allowedOrigins(origins.toArray(String[]::new))
                    .allowCredentials(true)
                    .allowedMethods("GET", "POST", "DELETE", "PUT")
                    .maxAge(3600);
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginRateLimitInterceptor)
                .addPathPatterns("/api/v1/user/login");

        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/flow/**")
                .excludePathPatterns("/api/v1/open_api/**")
                .excludePathPatterns("/api/v1/config/get")
                .excludePathPatterns("/api/v1/user/login")
                .excludePathPatterns("/api/v1/captcha/**");
    }
}
