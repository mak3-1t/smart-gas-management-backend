package com.gasmanagement.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bind cấu hình JWT từ application.yml:
 * <pre>
 * jwt:
 *   secret: ...
 *   expiration: 86400000
 *   refresh-expiration: 604800000
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** Secret key dùng để sign JWT (HS256) */
    private String secret;

    /** Thời gian hết hạn access token (ms), default 24h */
    private long expiration = 86400000L;

    /** Thời gian hết hạn refresh token (ms), default 7 ngày */
    private long refreshExpiration = 604800000L;
}
