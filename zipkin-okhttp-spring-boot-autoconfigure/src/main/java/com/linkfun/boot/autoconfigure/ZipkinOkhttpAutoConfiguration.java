package com.linkfun.boot.autoconfigure;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import brave.context.slf4j.MDCScopeDecorator;
import brave.spring.beans.CurrentTraceContextFactoryBean;
import brave.spring.beans.TracingFactoryBean;
import com.linkfun.boot.properties.ZipkinProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import zipkin2.codec.BytesEncoder;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.InMemoryReporterMetrics;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

/**
 * Description:
 * <p>
 * User: Mark.Yang
 * Email: ywengineer@gmail.com
 * Date: 2020-03-20
 * Time: 10:22
 */
@Configuration
@ConditionalOnClass(ZipkinProperties.class)
@EnableConfigurationProperties({ZipkinProperties.class})
public class ZipkinOkhttpAutoConfiguration {

    @Bean
    public InMemoryReporterMetrics inMemoryReporterMetrics() {
        return new InMemoryReporterMetrics();
    }

    @Bean
    public Sender sender(ZipkinProperties properties) {
        Assert.notNull(properties, "zipkin's endpoint not assigned");
        final OkHttpSender.Builder builder = OkHttpSender.newBuilder();
        if (properties.getEndpoint() != null) builder.endpoint(properties.getEndpoint());
        if (properties.getEncoding() != null) builder.encoding(properties.getEncoding());
        if (properties.getConnectTimeout() != null) builder.connectTimeout(properties.getConnectTimeout());
        if (properties.getReadTimeout() != null) builder.readTimeout(properties.getReadTimeout());
        if (properties.getWriteTimeout() != null) builder.writeTimeout(properties.getWriteTimeout());
        if (properties.getMaxRequests() != null) builder.maxRequests(properties.getMaxRequests());
        if (properties.getCompressionEnabled() != null) builder.compressionEnabled(properties.getCompressionEnabled());
        if (properties.getMessageMaxBytes() != null) builder.messageMaxBytes(properties.getMessageMaxBytes());
        return builder.build();
    }

    @Bean
    public AsyncReporter asyncReporter(Sender sender,
                                       ZipkinProperties properties,
                                       InMemoryReporterMetrics reporterMetrics) {
        AsyncReporter.Builder builder = AsyncReporter.builder(sender);
        if (reporterMetrics != null) builder.metrics(reporterMetrics);
        if (properties.getCloseTimeoutMills() != null) builder.closeTimeout(properties.getCloseTimeoutMills(), TimeUnit.MILLISECONDS);
        return properties.getEncoder() != null ? builder.build((BytesEncoder) properties.getEncoder()) : builder.build();
    }

    @Bean("tracing")
    public TracingFactoryBean tracing(
            AsyncReporter asyncReporter,
            CurrentTraceContextFactoryBean currentTraceContextFactoryBean,
            ZipkinProperties properties) throws Exception {
        ///////////
        Assert.hasText(properties.getLocalServiceName(), "zipkin's localServiceName not assigned");
        TracingFactoryBean factoryBean = new TracingFactoryBean();
        factoryBean.setLocalServiceName(properties.getLocalServiceName());
        factoryBean.setSpanReporter(asyncReporter);
        factoryBean.setCurrentTraceContext(currentTraceContextFactoryBean.getObject());
        return factoryBean;
    }

    @Bean
    public CurrentTraceContextFactoryBean currentTraceContextFactoryBean() {
        final CurrentTraceContextFactoryBean traceContextFactoryBean = new CurrentTraceContextFactoryBean();
        traceContextFactoryBean.setScopeDecorators(Collections.singletonList(MDCScopeDecorator.create()));
        return traceContextFactoryBean;
    }
}
