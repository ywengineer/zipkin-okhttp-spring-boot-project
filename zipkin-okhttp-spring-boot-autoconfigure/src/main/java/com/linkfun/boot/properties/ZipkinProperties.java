package com.linkfun.boot.properties;

import com.linkfun.boot.ZipkinConfigConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import zipkin2.codec.Encoding;

/**
 * Description:
 * <p>
 * User: Mark.Yang
 * Email: ywengineer@gmail.com
 * Date: 2020-03-20
 * Time: 10:07
 */
@Getter
@Setter
@ConfigurationProperties(ZipkinConfigConstants.PREFIX)
public class ZipkinProperties {
    private boolean enabled;
    private SenderProperties sender;
    private ReporterProperties reporter;

    @Getter
    @Setter
    public static class SenderProperties {
        private String endpoint;
        private Encoding encoding;
        private Integer maxRequests;
        private Integer connectTimeout, readTimeout, writeTimeout;
        private Boolean compressionEnabled;
        private Integer messageMaxBytes;
    }


    @Getter
    @Setter
    public static class ReporterProperties {
        private Integer closeTimeoutMills;
    }
}
