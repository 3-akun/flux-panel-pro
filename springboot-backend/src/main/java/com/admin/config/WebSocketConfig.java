package com.admin.config;

import com.admin.common.utils.WebSocketServer;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private WebSocketInterceptor webSocketInterceptor;

    @Value("${app.cors-origins:http://localhost:6366}")
    private String corsOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry
                .addHandler(myHandler(), "/system-info")
                .setAllowedOriginPatterns(parseOrigins(corsOrigins).toArray(String[]::new))
                .addInterceptors(webSocketInterceptor);
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
    public WebSocketHandler myHandler() {
        return new WebSocketServer();
    }

}
