package com.admin.config;


import com.admin.common.utils.IpUtils;
import com.admin.common.utils.JwtUtil;
import com.admin.entity.Node;
import com.admin.entity.User;
import com.admin.service.NodeService;
import com.admin.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;


@Configuration
@Slf4j
public class WebSocketInterceptor extends HttpSessionHandshakeInterceptor {

    @Resource
    NodeService nodeService;

    @Resource
    UserService userService;

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception ex) {

    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        ServletServerHttpRequest serverHttpRequest = (ServletServerHttpRequest) request;
        HttpServletRequest servletRequest = serverHttpRequest.getServletRequest();
        String type = serverHttpRequest.getServletRequest().getParameter("type");
        String version = serverHttpRequest.getServletRequest().getParameter("version");
        String http = serverHttpRequest.getServletRequest().getParameter("http");
        String tls = serverHttpRequest.getServletRequest().getParameter("tls");
        String socks = serverHttpRequest.getServletRequest().getParameter("socks");
        if (Objects.equals(type, "1")) {
            String secret = resolveCredential(servletRequest, "node.");
            log.info("节点连接请求 - version: {} - IP: {}", version, getClientIp(request));
            Node node = nodeService.getOne(new QueryWrapper<Node>().eq("secret", secret));
            if (node == null) {
                log.info("节点验证失败：未找到匹配的secret");
                return false;
            }
            attributes.put("id", node.getId());
            attributes.put("nodeSecret", secret);
            attributes.put("nodeVersion", version);
            attributes.put("http",http);
            attributes.put("tls",tls);
            attributes.put("socks",socks);
            log.info("节点 {} 通过验证，版本: {}", node.getId(), version);
            // 不在这里更新状态，等到连接建立后再统一更新
        }else {
            String secret = resolveCredential(servletRequest, "auth.");
            boolean b = JwtUtil.validateToken(secret);
            if (!b) return false;
            Long userId = JwtUtil.getUserIdFromToken(secret);
            User user = userService.getById(userId);
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                return false;
            }
            attributes.put("id", userId);
        }
        attributes.put("type", type);
        return true;
    }

    private String resolveCredential(HttpServletRequest request, String protocolPrefix) {
        String headerSecret = request.getHeader("X-Node-Secret");
        if (headerSecret != null && !headerSecret.isBlank()) {
            return headerSecret;
        }

        String protocol = request.getHeader("Sec-WebSocket-Protocol");
        String decoded = decodeProtocolCredential(protocol, protocolPrefix);
        if (decoded != null && !decoded.isBlank()) {
            return decoded;
        }

        // 兼容旧节点和旧前端，后续版本可移除 query secret。
        return request.getParameter("secret");
    }

    private String decodeProtocolCredential(String protocol, String prefix) {
        if (protocol == null || protocol.isBlank()) {
            return null;
        }
        for (String item : protocol.split(",")) {
            String value = item.trim();
            if (!value.startsWith(prefix)) {
                continue;
            }
            String encoded = value.substring(prefix.length());
            try {
                int padding = (4 - encoded.length() % 4) % 4;
                encoded = encoded + "=".repeat(padding);
                byte[] decoded = Base64.getUrlDecoder().decode(encoded);
                return new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                log.info("WebSocket 鉴权协议解析失败");
                return null;
            }
        }
        return null;
    }

    public String getClientIp(ServerHttpRequest request) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return null;
    }


}
