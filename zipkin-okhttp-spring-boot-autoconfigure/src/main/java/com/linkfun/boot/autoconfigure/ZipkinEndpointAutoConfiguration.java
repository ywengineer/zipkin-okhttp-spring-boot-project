package com.linkfun.boot.autoconfigure;

import com.linkfun.boot.endpoint.ZipkinMetricsEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.InMemoryReporterMetrics;

/**
 * Description:
 * <p>
 * User: Mark.Yang
 * Email: ywengineer@gmail.com
 * Date: 2020-03-20
 * Time: 10:22
 */
@Configuration
public class ZipkinEndpointAutoConfiguration {

    @Bean
    @ConditionalOnBean(InMemoryReporterMetrics.class)
    public ZipkinMetricsEndpoint zipkinMetricsEndpoint() {
        return new ZipkinMetricsEndpoint();
    }
}
