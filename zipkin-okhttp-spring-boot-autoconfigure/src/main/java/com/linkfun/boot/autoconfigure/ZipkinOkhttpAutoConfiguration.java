package com.linkfun.boot.autoconfigure;

import java.util.Collections;
import java.util.Objects;

import brave.context.slf4j.MDCScopeDecorator;
import brave.spring.beans.CurrentTraceContextFactoryBean;
import brave.spring.beans.TracingFactoryBean;
import com.linkfun.boot.properties.ZipkinProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import zipkin2.Span;
import zipkin2.reporter.InMemoryReporterMetrics;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.beans.AsyncReporterFactoryBean;
import zipkin2.reporter.beans.OkHttpSenderFactoryBean;

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
    public OkHttpSenderFactoryBean sender(ZipkinProperties properties) {
        if (Objects.isNull(properties.getSender()) || StringUtils.isEmpty(properties.getSender().getEncoding())) {
            throw new IllegalArgumentException("sender.endpoint not assigned");
        }
        OkHttpSenderFactoryBean senderFactoryBean = new OkHttpSenderFactoryBean();
        senderFactoryBean.setEndpoint(properties.getSender().getEndpoint());
        senderFactoryBean.setEncoding(properties.getSender().getEncoding());
        senderFactoryBean.setMaxRequests(properties.getSender().getMaxRequests());
        senderFactoryBean.setCompressionEnabled(properties.getSender().getCompressionEnabled());
        senderFactoryBean.setMessageMaxBytes(properties.getSender().getMessageMaxBytes());
        senderFactoryBean.setConnectTimeout(properties.getSender().getConnectTimeout());
        senderFactoryBean.setReadTimeout(properties.getSender().getReadTimeout());
        senderFactoryBean.setWriteTimeout(properties.getSender().getWriteTimeout());
        return senderFactoryBean;
    }

    @Bean
    public InMemoryReporterMetrics inMemoryReporterMetrics() {
        return new InMemoryReporterMetrics();
    }

    @Bean
    public AsyncReporterFactoryBean asyncReporterFactoryBean(OkHttpSenderFactoryBean senderFactoryBean, ZipkinProperties zipkinProperties) throws Exception {
        AsyncReporterFactoryBean factoryBean = new AsyncReporterFactoryBean();
        factoryBean.setMetrics(inMemoryReporterMetrics());
        factoryBean.setSender((Sender) senderFactoryBean.getObject());
        factoryBean.setCloseTimeout(Objects.isNull(zipkinProperties.getReporter()) ? null : zipkinProperties.getReporter().getCloseTimeoutMills());
        return factoryBean;
    }

    @Bean("tracing")
    public TracingFactoryBean tracing(AsyncReporterFactoryBean asyncReporterFactoryBean) throws Exception {
        TracingFactoryBean factoryBean = new TracingFactoryBean();
        factoryBean.setSpanReporter((Reporter<Span>) asyncReporterFactoryBean.getObject());
        factoryBean.setCurrentTraceContext(traceContextFactoryBean().getObject());
        return factoryBean;
    }

    @Bean
    public CurrentTraceContextFactoryBean traceContextFactoryBean() {
        final CurrentTraceContextFactoryBean traceContextFactoryBean = new CurrentTraceContextFactoryBean();
        traceContextFactoryBean.setScopeDecorators(Collections.singletonList(MDCScopeDecorator.create()));
        return traceContextFactoryBean;
    }
}
