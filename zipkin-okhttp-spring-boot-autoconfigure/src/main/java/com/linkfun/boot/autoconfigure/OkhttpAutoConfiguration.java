package com.linkfun.boot.autoconfigure;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import brave.Tracing;
import brave.http.HttpTracing;
import brave.okhttp3.TracingInterceptor;
import com.linkfun.boot.properties.ZipkinProperties;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;

/**
 * Description:
 * <p>
 * User: Mark.Yang
 * Email: ywengineer@gmail.com
 * Date: 2020-03-20
 * Time: 10:22
 */
@Configuration
@AutoConfigureAfter(ZipkinAutoConfiguration.class)
@AutoConfigureBefore(RestTemplateAutoConfiguration.class)
public class OkhttpAutoConfiguration {
    private final ObjectProvider<HttpMessageConverters> messageConverters;

    private final ObjectProvider<RestTemplateCustomizer> restTemplateCustomizers;

    private final ObjectProvider<ZipkinProperties> zipkinProperties;

    private final ObjectProvider<Tracing> tracing;

    public OkhttpAutoConfiguration(ObjectProvider<HttpMessageConverters> messageConverters,
                                   ObjectProvider<RestTemplateCustomizer> restTemplateCustomizers,
                                   ObjectProvider<ZipkinProperties> zipkinProperties,
                                   ObjectProvider<Tracing> tracing) {
        this.messageConverters = messageConverters;
        this.restTemplateCustomizers = restTemplateCustomizers;
        this.zipkinProperties = zipkinProperties;
        this.tracing = tracing;
    }

    @Bean
    public OkHttpClient.Builder okHttpClientBuilder(HttpTracing httpTracing) {
        final ZipkinProperties properties = zipkinProperties.getObject();
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .dispatcher(new Dispatcher(
                        httpTracing.tracing().currentTraceContext()
                                .executorService(newDispatcher(properties.getMaxRequests()).executorService())
                ))
                .addNetworkInterceptor(TracingInterceptor.create(httpTracing));
        if (properties.getConnectTimeout() != null) builder.connectTimeout(Duration.ofMillis(properties.getConnectTimeout()));
        if (properties.getReadTimeout() != null) builder.readTimeout(Duration.ofMillis(properties.getReadTimeout()));
        if (properties.getWriteTimeout() != null) builder.writeTimeout(Duration.ofMillis(properties.getWriteTimeout()));
        return builder;
    }

    @Bean
    public HttpTracing httpTracing() {
        return HttpTracing.create(tracing.getObject());
    }

    @Bean
    public RestTemplateBuilder restTemplateBuilder(OkHttpClient.Builder clientBuilder) {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        HttpMessageConverters converters = this.messageConverters.getIfUnique();
        if (converters != null) {
            builder = builder.messageConverters(converters.getConverters());
        }
        List<RestTemplateCustomizer> customizers = this.restTemplateCustomizers.orderedStream()
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(customizers)) {
            builder = builder.customizers(customizers);
        }
        //
        return builder.requestFactory(() -> new OkHttp3ClientHttpRequestFactory(clientBuilder.build()));
    }

    static Dispatcher newDispatcher(int maxRequests) {
        // bound the executor so that we get consistent performance
        ThreadPoolExecutor dispatchExecutor =
                new ThreadPoolExecutor(0, maxRequests, 60, TimeUnit.SECONDS,
                        // Using a synchronous queue means messages will send immediately until we hit max
                        // in-flight requests. Once max requests are hit, send will block the caller, which is
                        // the AsyncReporter flush thread. This is ok, as the AsyncReporter has a buffer of
                        // unsent spans for this purpose.
                        new SynchronousQueue<>(),
                        r -> new Thread(r, "Linkfun OkHttpSender Dispatcher"));

        Dispatcher dispatcher = new Dispatcher(dispatchExecutor);
        dispatcher.setMaxRequests(maxRequests);
        dispatcher.setMaxRequestsPerHost(maxRequests);
        return dispatcher;
    }
}
