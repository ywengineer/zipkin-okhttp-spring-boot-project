package com.linkfun.boot.endpoint;

import java.util.Objects;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import zipkin2.reporter.InMemoryReporterMetrics;

/**
 * Description:
 * <p>
 * User: Mark.Yang
 * Email: ywengineer@gmail.com
 * Date: 2020-03-20
 * Time: 10:49
 */
@Endpoint(id = "zipkin-metrics")
public class ZipkinMetricsEndpoint {

    private InMemoryReporterMetrics reporterMetrics;

    @ReadOperation
    public MetricsData invoke() {
        if (Objects.isNull(reporterMetrics)) {
            return null;
        }
        return MetricsData.builder()
                .messages(reporterMetrics.messages())
                .messageBytes(reporterMetrics.messageBytes())
                .spans(reporterMetrics.spans())
                .spanBytes(reporterMetrics.spanBytes())
                .spansDropped(reporterMetrics.spansDropped())
                .spansPending(reporterMetrics.queuedSpans())
                .spanBytesPending(reporterMetrics.queuedBytes())
                .build();
    }

    @Autowired(required = false)
    public void setReporterMetrics(InMemoryReporterMetrics reporterMetrics) {
        this.reporterMetrics = reporterMetrics;
    }

    @Getter
    @Setter
    @Builder
    public static class MetricsData {
        private long messages;
        private long messageBytes;
        private long spans;
        private long spanBytes;
        private long spansDropped;
        private long spansPending;
        private long spanBytesPending;
    }
}
